package com.ffh.vpn.net

import com.ffh.vpn.data.model.TrafficInfo
import com.ffh.vpn.data.parse.SubscriptionParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class FetchResult(
    val body: String? = null,
    val error: String? = null,
    val traffic: TrafficInfo? = null,
    val profileTitle: String? = null,
    val homepage: String? = null,
    val announcement: String? = null,
    val updateIntervalHours: Int? = null
)

object SubscriptionFetcher {

    private val client by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun fetch(url: String, userAgent: String): FetchResult {
        val request = runCatching {
            Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .get()
                .build()
        }.getOrElse { return FetchResult(error = "bad url: ${it.message}") }

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return FetchResult(error = "HTTP ${response.code}")
                }
                val body = response.body?.string()?.takeIf { it.isNotBlank() }
                    ?: return FetchResult(error = "empty response")
                val headers = response.headers
                FetchResult(
                    body = body,
                    traffic = SubscriptionParser.parseTrafficHeader(headers["subscription-userinfo"]),
                    profileTitle = SubscriptionParser.decodeTitle(headers["profile-title"]),
                    homepage = headers["profile-web-page-url"],
                    announcement = headers["announcement"] ?: headers["announce"],
                    updateIntervalHours = SubscriptionParser.parseUpdateInterval(headers["profile-update-interval"])
                )
            }
        } catch (t: Throwable) {
            FetchResult(error = t.message ?: t.javaClass.simpleName)
        }
    }
}
