package com.ffh.vpn.data.parse

import com.ffh.vpn.data.model.Protocol
import com.ffh.vpn.data.model.ServerProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

/**
 * Share link -> server profile.
 *
 * Supported: vless, vmess, trojan, shadowsocks (SIP002, legacy and 2022 ciphers),
 * shadowsocksr, hysteria2, tuic, wireguard, http, socks.
 * Protocols that Xray cannot run are still parsed (they stay visible in the UI)
 * but get no outbound, so the UI can flag them.
 */
object LinkParser {

    fun parse(line: String, subscriptionId: String? = null): ServerProfile? {
        val raw = line.trim().trimEnd('/', '\n', '\r')
        if (raw.isBlank()) return null
        val scheme = raw.substringBefore("://", "").lowercase()

        val parsed = runCatching {
            when (scheme) {
                "vless" -> parseVless(raw)
                "vmess" -> parseVmess(raw)
                "trojan" -> parseTrojan(raw)
                "ss" -> parseShadowsocks(raw)
                "ssr" -> parseShadowsocksR(raw)
                "hysteria2", "hy2", "hysteria" -> parseHysteria2(raw)
                "tuic" -> parseTuic(raw)
                "wireguard", "wg" -> parseWireguard(raw)
                "http", "https" -> parseHttp(raw)
                "socks", "socks5", "socks4" -> parseSocks(raw)
                else -> null
            }
        }.getOrNull() ?: return null

        if (parsed.address.isBlank() && parsed.port == 0) return null

        return parsed.copy(
            id = UUID.randomUUID().toString(),
            subscriptionId = subscriptionId,
            addedAt = System.currentTimeMillis(),
            link = raw
        )
    }

    // ------------------------------------------------------------------ vless

    private fun parseVless(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val uuid = url.userInfo?.substringBefore(':')?.takeIf { it.isNotBlank() } ?: return null
        val host = url.host
        val port = url.port ?: 443
        val p = Params(url.query)

        return ServerProfile(
            protocol = Protocol.VLESS,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "vless")
                putJsonObject("settings") {
                    putJsonArray("vnext") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            putJsonArray("users") {
                                addJsonObject {
                                    put("id", uuid)
                                    put("encryption", p.s("encryption") ?: "none")
                                    p.s("flow")?.let { put("flow", it) }
                                    put("level", 0)
                                }
                            }
                        }
                    }
                }
                put("streamSettings", buildStreamSettings(p))
            }
        )
    }

    // ------------------------------------------------------------------ vmess

    private fun parseVmess(raw: String): ServerProfile? {
        val payload = raw.removePrefix("vmess://").trim()
        val text = decodeBase64(payload) ?: return null
        val obj = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null

        val host = obj.str("add") ?: return null
        val port = obj.int("port") ?: return null
        val id = obj.str("id") ?: return null
        val network = normalizeNetwork(obj.str("net"))
        val tls = if (obj.str("tls").equals("tls", true)) "tls" else "none"

        val map = mutableMapOf<String, String>()
        obj.str("sni")?.let { map["sni"] = it }
        obj.str("host")?.let { map["host"] = it }
        obj.str("path")?.let { map["path"] = it }
        obj.str("fp")?.let { map["fp"] = it }
        obj.str("alpn")?.let { map["alpn"] = it }
        obj.str("type")?.let { map["headerType"] = it }
        obj.str("seed")?.let { map["seed"] = it }
        map["type"] = network ?: "tcp"
        map["security"] = tls

        return ServerProfile(
            protocol = Protocol.VMESS,
            remark = obj.str("ps")?.ifBlank { null } ?: "$host:$port",
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "vmess")
                putJsonObject("settings") {
                    putJsonArray("vnext") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            putJsonArray("users") {
                                addJsonObject {
                                    put("id", id)
                                    put("alterId", obj.int("aid") ?: 0)
                                    put("security", obj.str("scy") ?: "auto")
                                    put("level", 0)
                                }
                            }
                        }
                    }
                }
                put("streamSettings", buildStreamSettings(Params(map)))
            }
        )
    }

    // ----------------------------------------------------------------- trojan

    private fun parseTrojan(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val password = url.userInfo?.takeIf { it.isNotBlank() } ?: return null
        val host = url.host
        val port = url.port ?: 443
        val p = Params(url.query)

        return ServerProfile(
            protocol = Protocol.TROJAN,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "trojan")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            put("password", urlDecode(password))
                            put("level", 0)
                        }
                    }
                }
                put("streamSettings", buildStreamSettings(p))
            }
        )
    }

    // ----------------------------------------------------------- shadowsocks

    private fun parseShadowsocks(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val fragment = url.fragment?.let { urlDecode(it) }

        val method: String
        val password: String
        val host: String
        val port: Int

        if (!url.userInfo.isNullOrBlank()) {
            val rawUserInfo = url.userInfo
            val decoded = if (rawUserInfo.contains(':')) rawUserInfo else decodeBase64(rawUserInfo) ?: rawUserInfo
            val colon = decoded.indexOf(':')
            if (colon <= 0) return null
            method = decoded.substring(0, colon)
            password = decoded.substring(colon + 1)
            host = url.host
            port = url.port ?: 8388
        } else {
            val body = raw.removePrefix("ss://").substringBefore('#')
            val decoded = decodeBase64(body) ?: return null
            val at = decoded.lastIndexOf('@')
            if (at <= 0) return null
            val left = decoded.substring(0, at)
            val right = decoded.substring(at + 1)
            val colon = left.indexOf(':')
            if (colon <= 0) return null
            method = left.substring(0, colon)
            password = left.substring(colon + 1)
            val (h, p) = splitHostPort(right)
            host = h
            port = p ?: 8388
        }

        if (method.isBlank() || host.isBlank()) return null

        return ServerProfile(
            protocol = Protocol.SHADOWSOCKS,
            remark = fragment?.ifBlank { null } ?: "$host:$port",
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "shadowsocks")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            put("method", method)
                            put("password", password)
                            put("level", 0)
                        }
                    }
                }
                put("streamSettings", buildJsonObject { put("network", "tcp") })
            }
        )
    }

    // ---------------------------------------------------------- shadowsocksr

    private fun parseShadowsocksR(raw: String): ServerProfile? {
        val body = raw.removePrefix("ssr://").trim()
        val decoded = decodeBase64(body) ?: return null
        val main = decoded.substringBefore("/?")
        val query = parseQuery(decoded.substringAfter("/?", ""))
        val parts = main.split(':')
        if (parts.size < 6) return null

        val host = parts[0]
        val port = parts[1].toIntOrNull() ?: return null
        val remark = query["remarks"]?.let { decodeBase64(it) }?.ifBlank { null }
            ?: query["remarks"]?.let { urlDecode(it) }
            ?: "$host:$port"

        // Xray has no ShadowsocksR implementation, so the profile is kept for
        // display / export only.
        return ServerProfile(
            protocol = Protocol.SHADOWSOCKSR,
            remark = remark,
            address = host,
            port = port,
            outbound = null
        )
    }

    // ------------------------------------------------------------ hysteria 2

    private fun parseHysteria2(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val auth = url.userInfo?.let { urlDecode(it) }?.takeIf { it.isNotBlank() } ?: return null
        val host = url.host
        val port = url.port ?: 443
        val p = Params(url.query)
        val sni = p.s("sni") ?: p.s("peer") ?: host
        val insecure = p.b("insecure")

        return ServerProfile(
            protocol = Protocol.HYSTERIA2,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "hysteria")
                putJsonObject("settings") {
                    put("version", 2)
                    put("address", host)
                    put("port", port)
                }
                putJsonObject("streamSettings") {
                    put("security", "tls")
                    putJsonObject("tlsSettings") {
                        put("serverName", sni)
                        put("allowInsecure", insecure)
                        putJsonArray("alpn") { add("h3") }
                    }
                    putJsonObject("hysteriaSettings") {
                        put("version", 2)
                        put("auth", auth)
                    }
                }
            }
        )
    }

    // ------------------------------------------------------------------ tuic

    private fun parseTuic(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val userInfo = url.userInfo?.let { urlDecode(it) } ?: return null
        if (userInfo.isBlank()) return null
        val host = url.host
        val port = url.port ?: 443
        // Xray does not ship a TUIC client, the profile is display only.
        return ServerProfile(
            protocol = Protocol.TUIC,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = null
        )
    }

    // ------------------------------------------------------------- wireguard

    private fun parseWireguard(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val p = Params(url.query)
        val privateKey = url.userInfo?.let { urlDecode(it) }?.takeIf { it.isNotBlank() }
            ?: p.s("privatekey") ?: p.s("private_key") ?: return null
        val publicKey = p.s("publickey") ?: p.s("public_key") ?: p.s("peer") ?: return null
        val host = url.host
        val port = url.port ?: 51820
        val addresses = (p.s("address") ?: p.s("addresses") ?: "10.0.0.2/32")
            .split(',').map { it.trim() }.filter { it.isNotEmpty() }
        val reserved = (p.s("reserved") ?: "")
            .split(',').mapNotNull { it.trim().toIntOrNull() }
        val mtu = p.i("mtu") ?: 1420
        val keepAlive = p.i("keepalive") ?: p.i("keep_alive") ?: 25
        val endpoint = if (host.contains(':')) "[$host]:$port" else "$host:$port"

        return ServerProfile(
            protocol = Protocol.WIREGUARD,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "wireguard")
                putJsonObject("settings") {
                    put("secretKey", privateKey)
                    putJsonArray("address") { addresses.forEach { add(it) } }
                    putJsonArray("peers") {
                        addJsonObject {
                            put("publicKey", publicKey)
                            put("endpoint", endpoint)
                            put("keepAlive", keepAlive)
                        }
                    }
                    put("mtu", mtu)
                    put("noKernelTun", true)
                    if (reserved.isNotEmpty()) {
                        putJsonArray("reserved") { reserved.forEach { add(it) } }
                    }
                }
            }
        )
    }

    // ------------------------------------------------------------------ http

    private fun parseHttp(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val host = url.host
        if (host.isBlank()) return null
        val port = url.port ?: if (url.scheme == "https") 443 else 80
        val userInfo = url.userInfo?.let { urlDecode(it) }
        val user = userInfo?.substringBefore(':')
        val pass = userInfo?.substringAfter(':', "")

        return ServerProfile(
            protocol = Protocol.HTTP,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "http")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            if (!user.isNullOrBlank()) {
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("user", user)
                                        put("pass", pass ?: "")
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                }
                if (url.scheme == "https") {
                    putJsonObject("streamSettings") {
                        put("security", "tls")
                        putJsonObject("tlsSettings") {
                            put("serverName", host)
                        }
                    }
                }
            }
        )
    }

    private fun parseSocks(raw: String): ServerProfile? {
        val url = parseUrl(raw) ?: return null
        val host = url.host
        if (host.isBlank()) return null
        val port = url.port ?: 1080
        val userInfo = url.userInfo?.let { urlDecode(it) }
        val user = userInfo?.substringBefore(':')
        val pass = userInfo?.substringAfter(':', "")

        return ServerProfile(
            protocol = Protocol.SOCKS,
            remark = remark(url, "$host:$port"),
            address = host,
            port = port,
            outbound = buildJsonObject {
                put("protocol", "socks")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", host)
                            put("port", port)
                            if (!user.isNullOrBlank()) {
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("user", user)
                                        put("pass", pass ?: "")
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    // -------------------------------------------------------------- helpers

    private fun remark(url: UrlParts, fallback: String): String =
        url.fragment?.let { urlDecode(it) }?.trim()?.ifBlank { null } ?: fallback

    private fun normalizeNetwork(value: String?): String = when (value?.lowercase()) {
        "h2", "http" -> "http"
        "xhttp", "splithttp" -> "xhttp"
        "httpupgrade" -> "httpupgrade"
        "grpc", "gun" -> "grpc"
        "ws", "websocket" -> "ws"
        "quic" -> "quic"
        "kcp", "mkcp" -> "kcp"
        "raw", "tcp" -> "raw"
        else -> "raw"
    }

    /** Thin accessor over the different parameter sources (query, json, clash). */
    class Params(private val map: Map<String, String>) {
        operator fun get(key: String): String? = map[key] ?: map[key.lowercase()] ?: map[key.uppercase()]

        fun s(key: String): String? = this[key]?.takeIf { it.isNotBlank() }
        fun i(key: String): Int? = this[key]?.trim()?.toIntOrNull()
        fun b(key: String, default: Boolean = false): Boolean {
            val raw = this[key]?.trim()?.lowercase() ?: return default
            return raw == "1" || raw == "true" || raw == "yes" || raw == "on"
        }
        fun list(key: String): List<String>? =
            this[key]?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
    }

    /** Builds the Xray `streamSettings` object out of the link parameters. */
    fun buildStreamSettings(p: Params): JsonObject = buildJsonObject {
        val network = normalizeNetwork(p.s("type") ?: p.s("network"))
        val security = p.s("security")?.lowercase() ?: "none"
        val host = p.s("host")
        val path = p.s("path")
        val sni = p.s("sni") ?: p.s("peer") ?: host
        val fingerprint = p.s("fp") ?: p.s("fingerprint")
        val alpn = p.list("alpn")

        put("network", network)

        when (network) {
            "ws" -> putJsonObject("wsSettings") {
                put("path", path ?: "/")
                if (!host.isNullOrBlank()) putJsonObject("headers") { put("Host", host) }
                p.i("maxed")?.let { put("maxEarlyData", it) }
                p.i("maxearlydata")?.let { put("maxEarlyData", it) }
                p.s("edn")?.let { put("earlyDataHeaderName", it) }
            }
            "grpc" -> putJsonObject("grpcSettings") {
                put("serviceName", p.s("serviceName") ?: p.s("servicename") ?: path ?: "")
                p.b("multiMode").let { if (it) put("multiMode", true) }
            }
            "httpupgrade" -> putJsonObject("httpupgradeSettings") {
                put("path", path ?: "/")
                if (!host.isNullOrBlank()) put("host", host)
            }
            "xhttp" -> putJsonObject("xhttpSettings") {
                put("path", path ?: "/")
                if (!host.isNullOrBlank()) put("host", host)
                p.s("mode")?.let { put("mode", it) }
            }
            "http" -> putJsonObject("httpSettings") {
                put("path", path ?: "/")
                val hosts = host?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                if (hosts.isNotEmpty()) putJsonArray("host") { hosts.forEach { add(it) } }
            }
            "kcp" -> putJsonObject("kcpSettings") {
                put("mtu", p.i("mtu") ?: 1350)
                put("tti", 20)
                put("uplinkCapacity", 12)
                put("downlinkCapacity", 50)
                put("congestion", false)
                put("readBufferSize", 1)
                put("writeBufferSize", 1)
                putJsonObject("header") { put("type", p.s("headerType") ?: "none") }
                p.s("seed")?.let { put("seed", it) }
            }
            "quic" -> putJsonObject("quicSettings") {
                put("security", "none")
                put("key", "")
                putJsonObject("header") { put("type", p.s("headerType") ?: "none") }
            }
        }

        when (security) {
            "tls" -> {
                put("security", "tls")
                putJsonObject("tlsSettings") {
                    sni?.let { put("serverName", it) }
                    put("allowInsecure", p.b("allowInsecure") || p.b("allowinsecure") || p.b("insecure"))
                    fingerprint?.let { put("fingerprint", it) }
                    alpn?.let { putJsonArray("alpn") { it.forEach { add(it) } } }
                }
            }
            "reality" -> {
                put("security", "reality")
                putJsonObject("realitySettings") {
                    sni?.let { put("serverName", it) }
                    p.s("pbk")?.let { put("publicKey", it) }
                    p.s("sid")?.let { put("shortId", it) }
                    p.s("spx")?.let { put("spiderX", it) }
                    fingerprint?.let { put("fingerprint", it) }
                }
            }
        }

        val sockopt = buildJsonObject {
            if (p.b("tfo")) put("tcpFastOpen", true)
            if (p.b("mptcp")) put("tcpMptcp", true)
        }
        if (sockopt.isNotEmpty()) put("sockopt", sockopt)
    }

    private fun JsonObject?.str(key: String): String? =
        this?.get(key)?.let { if (it is JsonPrimitive) it.content else null }

    private fun JsonObject?.int(key: String): Int? = str(key)?.trim()?.toIntOrNull()

    /** Parses a single Xray / sing-box style outbound object. */
    fun parseOutboundJson(element: JsonElement): ServerProfile? {
        val obj = element as? JsonObject ?: return null
        val protocolName = obj["protocol"]?.let { if (it is JsonPrimitive) it.content else null }
            ?: obj["type"]?.let { if (it is JsonPrimitive) it.content else null }
            ?: return null
        val settings = obj["settings"] as? JsonObject
        val address = firstString(settings, "address")
            ?: firstString(settings, "vnext", "address")
            ?: firstString(settings, "servers", "address")
        val port = firstInt(settings, "port")
            ?: firstInt(settings, "vnext", "port")
            ?: firstInt(settings, "servers", "port")
        val remark = obj["tag"]?.let { if (it is JsonPrimitive) it.content else null } ?: address ?: protocolName

        val protocol = when (protocolName.lowercase()) {
            "vless" -> Protocol.VLESS
            "vmess" -> Protocol.VMESS
            "trojan" -> Protocol.TROJAN
            "shadowsocks" -> Protocol.SHADOWSOCKS
            "hysteria", "hysteria2" -> Protocol.HYSTERIA2
            "tuic" -> Protocol.TUIC
            "wireguard" -> Protocol.WIREGUARD
            "http" -> Protocol.HTTP
            "socks" -> Protocol.SOCKS
            else -> Protocol.UNKNOWN
        }

        val outbound = buildJsonObject {
            put("protocol", protocolName)
            settings?.let { put("settings", it) }
            (obj["streamSettings"] as? JsonObject)?.let { put("streamSettings", it) }
        }

        return ServerProfile(
            protocol = protocol,
            remark = remark,
            address = address ?: "",
            port = port ?: 0,
            outbound = if (protocol == Protocol.TUIC) null else outbound
        )
    }

    private fun firstString(settings: JsonObject?, vararg path: String): String? {
        if (settings == null) return null
        if (path.size == 1) return settings.str(path[0])
        val array = settings[path[0]] as? kotlinx.serialization.json.JsonArray ?: return null
        val first = array.firstOrNull() as? JsonObject ?: return null
        return first.str(path[1])
    }

    private fun firstInt(settings: JsonObject?, vararg path: String): Int? {
        if (settings == null) return null
        if (path.size == 1) return settings.int(path[0])
        val array = settings[path[0]] as? kotlinx.serialization.json.JsonArray ?: return null
        val first = array.firstOrNull() as? JsonObject ?: return null
        return first.int(path[1])
    }
}
