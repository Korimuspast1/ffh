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
import java.net.InetSocketAddress
import java.net.Socket

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

/** Measures the real round trip through the running tunnel (URL test). */
object UrlTester {

    suspend fun testThroughProxy(proxyPort: Int, url: String, timeoutMs: Int = 8000): Long? =
        withContext(Dispatchers.IO) {
            val client = okhttp3.OkHttpClient.Builder()
                .proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", proxyPort)))
                .connectTimeout(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()
            val request = okhttp3.Request.Builder().url(url).get().build()
            try {
                val started = System.nanoTime()
                client.newCall(request).execute().use { response ->
                    response.body?.close()
                    if (!response.isSuccessful) return@withContext null
                    (System.nanoTime() - started) / 1_000_000L
                }
            } catch (t: Throwable) {
                null
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }
}
