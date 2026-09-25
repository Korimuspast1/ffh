package com.ffh.vpn.net

import com.ffh.vpn.data.model.ServerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.util.concurrent.TimeUnit

/**
 * Real latency measurements: a TCP connect to the server endpoint, repeated a
 * couple of times so that a single unlucky packet does not ruin the result.
 */
object Pinger {

    data class Result(val id: String, val millis: Int, val reachable: Boolean)

    suspend fun measure(host: String, port: Int, timeoutMs: Int, attempts: Int = 2): Result? =
        withContext(Dispatchers.IO) {
            if (host.isBlank() || port <= 0) return@withContext null
            var best = Int.MAX_VALUE
            var reachable = false
            repeat(attempts) { index ->
                val started = System.nanoTime()
                try {
                    Socket().use { socket ->
                        socket.bind(null)
                        socket.connect(InetSocketAddress(host, port), timeoutMs)
                    }
                    val elapsed = ((System.nanoTime() - started) / 1_000_000L).toInt()
                    best = minOf(best, elapsed)
                    reachable = true
                } catch (t: Throwable) {
                    // unreachable / refused / timed out
                }
                if (index < attempts - 1 && reachable) delay(40)
            }
            if (!reachable) null else Result("", best.coerceAtLeast(0), true)
        }

    /**
     * Pings a whole list in parallel.
     * [onPartial] is called whenever a single result is ready so the UI can
     * update progressively.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun measureAll(
        servers: List<ServerProfile>,
        concurrency: Int,
        timeoutMs: Int,
        onPartial: (suspend (Map<String, Int>) -> Unit)? = null
    ): Map<String, Int> = coroutineScope {
        val semaphore = Semaphore(concurrency.coerceAtLeast(1))
        val results = java.util.concurrent.ConcurrentHashMap<String, Int>()

        val jobs = servers.map { server ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val result = measure(server.address, server.port, timeoutMs)
                    if (result != null) {
                        results[server.id] = result.millis
                        onPartial?.invoke(mapOf(server.id to result.millis))
                    }
                }
            }
        }
        jobs.awaitAll()
        results.toMap()
    }

    /** Fire and forget variant used by the UI. */
    fun launchPing(
        servers: List<ServerProfile>,
        concurrency: Int,
        timeoutMs: Int,
        onPartial: suspend (Map<String, Int>) -> Unit
    ) {
        kotlinx.coroutines.MainScope().launch {
            if (!isActive) return@launch
            measureAll(servers, concurrency, timeoutMs, onPartial)
        }
    }
}

/** Measures the real round trip of an HTTP request through a local proxy. */
object UrlTester {

    /** Sends one GET through a SOCKS5 proxy (the per-server probe). */
    suspend fun getThroughSocks(proxyPort: Int, url: String, timeoutMs: Int = 4000): Boolean =
        withContext(Dispatchers.IO) {
            val client = client(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", proxyPort)), timeoutMs)
            try {
                client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                    response.body?.close()
                    response.isSuccessful || response.code == 204 || response.code in 300..399
                }
            } catch (t: Throwable) {
                false
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }

    private fun client(proxy: Proxy, timeoutMs: Int): OkHttpClient = OkHttpClient.Builder()
        .proxy(proxy)
        .connectTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(timeoutMs.toLong(), TimeUnit.MILLISECONDS)
        .callTimeout((timeoutMs * 2).toLong(), TimeUnit.MILLISECONDS)
        .followRedirects(false)
        .build()

    suspend fun testThroughProxy(proxyPort: Int, url: String, timeoutMs: Int = 8000): Long? =
        withContext(Dispatchers.IO) {
            val client = client(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", proxyPort)), timeoutMs)
            try {
                val started = System.nanoTime()
                client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
                    response.body?.close()
                    if (!(response.isSuccessful || response.code == 204)) return@withContext null
                    (System.nanoTime() - started) / 1_000_000L
                }
            } catch (t: Throwable) {
                null
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }
}
