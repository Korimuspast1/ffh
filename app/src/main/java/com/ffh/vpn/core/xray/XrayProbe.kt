package com.ffh.vpn.core.xray

import android.content.Context
import com.ffh.vpn.core.CoreHandle
import com.ffh.vpn.core.XrayProcess
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.data.model.ServerProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Measures latency the way it really feels: a temporary Xray instance is
 * started for a single server, an HTTP request is sent through it, and the
 * round trip is timed. The instance is killed immediately afterwards.
 *
 * This is the "ping through the proxy" mode — slower than a TCP connect but it
 * tells you whether the server actually passes traffic.
 */
object XrayProbe {

    private const val FIRST_PORT = 20800
    private const val LAST_PORT = 20900
    private val nextPort = AtomicInteger(FIRST_PORT)
    private val taken = ConcurrentHashMap.newKeySet<Int>()

    private fun takePort(): Int {
        repeat(LAST_PORT - FIRST_PORT) {
            val port = nextPort.getAndIncrement()
            if (port > LAST_PORT) nextPort.set(FIRST_PORT)
            if (taken.add(port)) return port
        }
        return 0
    }

    /** Config with a single local SOCKS inbound that leads to [server]. */
    fun buildConfig(server: ServerProfile, port: Int): String? {
        val outbound = server.outbound ?: return null
        val tagged = buildJsonObject {
            put("tag", XrayConfigBuilder.TAG_PROXY)
            for ((key, value) in outbound) {
                if (key == "tag") continue
                put(key, value)
            }
        }
        val root = buildJsonObject {
            putJsonObject("log") {
                put("loglevel", "error")
                put("access", "")
            }
            putJsonArray("inbounds") {
                addJsonObject {
                    put("tag", "probe-in")
                    put("listen", "127.0.0.1")
                    put("port", port)
                    put("protocol", "socks")
                    putJsonObject("settings") {
                        put("auth", "noauth")
                        put("udp", true)
                        put("ip", "127.0.0.1")
                    }
                }
            }
            putJsonArray("outbounds") {
                add(tagged)
            }
            putJsonObject("routing") {
                put("domainStrategy", "AsIs")
                putJsonArray("rules") {
                    addJsonObject {
                        put("type", "field")
                        putJsonArray("inboundTag") { add("probe-in") }
                        put("outboundTag", XrayConfigBuilder.TAG_PROXY)
                    }
                }
            }
        }
        return Json { prettyPrint = true }.encodeToString(root)
    }

    /** The core talks to stdout; it has to be drained or the pipe blocks. */
    private fun drain(handle: CoreHandle) {
        Thread {
            runCatching {
                handle.logs.bufferedReader().useLines { lines ->
                    for (line in lines) LogStore.append("probe", line)
                }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private suspend fun waitForPort(port: Int, timeoutMs: Int): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val open = runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port), 300)
                    true
                }
            }.getOrDefault(false)
            if (open) return true
            delay(120)
        }
        return false
    }

    /** Latency of one HTTP request through [server], or null when unreachable. */
    suspend fun measure(
        context: Context,
        server: ServerProfile,
        url: String,
        timeoutMs: Int
    ): Int? {
        if (!server.isSupported) return null
        if (!XrayProcess.isAvailable(context)) return null

        val port = takePort()
        if (port == 0) return null
        var process: CoreHandle? = null
        return try {
            val config = buildConfig(server, port) ?: return null
            val configFile = XrayProcess.probeConfigFile(context, port)
            configFile.writeText(config)
            process = XrayProcess.launch(context, configFile) ?: return null
            drain(process)
            if (!waitForPort(port, timeoutMs.coerceAtMost(4000))) {
                LogStore.append("ping", "${server.displayName()} did not open the local port")
                return null
            }
            val started = System.nanoTime()
            val ok = com.ffh.vpn.net.UrlTester.getThroughSocks(port, url, timeoutMs)
            if (!ok) {
                LogStore.append("ping", "${server.displayName()} failed the request")
                null
            } else {
                ((System.nanoTime() - started) / 1_000_000L).toInt().coerceAtLeast(1)
            }
        } catch (t: Throwable) {
            LogStore.append("ping", "${server.displayName()} error: ${t.message}")
            null
        } finally {
            runCatching {
                process?.destroyForcibly()
                process?.waitFor(400)
                process?.reap()
            }
            taken.remove(port)
        }
    }

    /** Runs [measure] over a list with a small amount of parallelism. */
    suspend fun measureAll(
        context: Context,
        servers: List<ServerProfile>,
        url: String,
        timeoutMs: Int,
        parallelism: Int = 3,
        onPartial: (suspend (Map<String, Int>) -> Unit)? = null
    ): Map<String, Int> {
        if (servers.isEmpty()) return emptyMap()
        val semaphore = Semaphore(parallelism.coerceAtLeast(1))
        val results = ConcurrentHashMap<String, Int>()
        coroutineScope {
            servers.map { server ->
                async {
                    semaphore.withPermit {
                        val value = measure(context, server, url, timeoutMs)
                        if (value != null) {
                            results[server.id] = value
                            onPartial?.invoke(mapOf(server.id to value))
                        }
                    }
                }
            }.forEach { it.await() }
        }
        return results.toMap()
    }
}
