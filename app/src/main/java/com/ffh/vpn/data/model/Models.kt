package com.ffh.vpn.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
enum class Protocol {
    @SerialName("vless") VLESS,
    @SerialName("vmess") VMESS,
    @SerialName("trojan") TROJAN,
    @SerialName("shadowsocks") SHADOWSOCKS,
    @SerialName("shadowsocksr") SHADOWSOCKSR,
    @SerialName("hysteria2") HYSTERIA2,
    @SerialName("tuic") TUIC,
    @SerialName("wireguard") WIREGUARD,
    @SerialName("http") HTTP,
    @SerialName("socks") SOCKS,
    @SerialName("unknown") UNKNOWN;

    /** Human readable protocol name used in the UI. */
    fun label(): String = when (this) {
        VLESS -> "VLESS"
        VMESS -> "VMess"
        TROJAN -> "Trojan"
        SHADOWSOCKS -> "Shadowsocks"
        SHADOWSOCKSR -> "ShadowsocksR"
        HYSTERIA2 -> "Hysteria 2"
        TUIC -> "TUIC"
        WIREGUARD -> "WireGuard"
        HTTP -> "HTTP"
        SOCKS -> "SOCKS"
        UNKNOWN -> "?"
    }
}

/**
 * A single server.
 *
 * [outbound] holds the ready-to-use Xray outbound object produced while parsing
 * the share link, so the config generator never has to re-interpret the link
 * and unknown parameters are never lost. When the protocol is not supported by
 * the core ([Protocol.TUIC], [Protocol.SHADOWSOCKSR]) the outbound stays null
 * and the UI marks the server accordingly.
 */
@Serializable
data class ServerProfile(
    val id: String = "",
    val subscriptionId: String? = null,
    val protocol: Protocol = Protocol.UNKNOWN,
    val remark: String = "",
    val address: String = "",
    val port: Int = 0,
    val outbound: JsonObject? = null,
    val link: String = "",
    val pingMillis: Int = 0,
    val addedAt: Long = 0L
) {
    val isSupported: Boolean get() = outbound != null

    fun displayName(): String = remark.ifBlank {
        if (address.isBlank()) "unknown" else "$address:${if (port == 0) "-" else port}"
    }

    fun subtitle(): String = buildString {
        append(protocol.label())
        if (address.isNotBlank()) {
            append(" · ")
            append(address)
        }
    }
}

@Serializable
data class TrafficInfo(
    val upload: Long = 0L,
    val download: Long = 0L,
    val total: Long = 0L,
    val expire: Long = 0L
) {
    val used: Long get() = upload + download
    val left: Long get() = if (total > 0) (total - used).coerceAtLeast(0L) else 0L
    val hasTraffic: Boolean get() = total > 0
    /** 0..1, or null when the plan is unlimited. */
    val ratio: Float? get() = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
}

@Serializable
data class Subscription(
    val id: String = "",
    val name: String = "",
    val url: String? = null,
    val description: String = "",
    val homepage: String? = null,
    val traffic: TrafficInfo? = null,
    val lastUpdatedAt: Long? = null,
    val updateIntervalHours: Int? = null,
    val lastError: String? = null,
    val servers: List<ServerProfile> = emptyList(),
    val sort: Int = 0,
    val createdAt: Long = 0L,
    /** Servers that could not be imported from the last update. */
    val skipped: Int = 0
) {
    val isRemote: Boolean get() = !url.isNullOrBlank()
    val displayName: String get() = name.ifBlank { url ?: "local" }
    val supportedCount: Int get() = servers.count { it.isSupported }
}

/** What came out of a subscription body, besides the servers themselves. */
@Serializable
data class SubscriptionMeta(
    val title: String? = null,
    val description: String? = null,
    val homepage: String? = null,
    val traffic: TrafficInfo? = null,
    val updateIntervalHours: Int? = null
)
