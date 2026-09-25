package com.ffh.vpn.data.parse

import com.ffh.vpn.data.model.Protocol
import com.ffh.vpn.data.model.ServerProfile
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.util.UUID

/**
 * A tiny YAML reader that understands just enough of the Clash config format to
 * import `proxies:` — no external YAML dependency, no reflection.
 */
object ClashParser {

    fun hasProxies(text: String): Boolean = text.contains("proxies:", ignoreCase = true)

    fun extractProxies(text: String): List<Map<String, String>> {
        val result = mutableListOf<MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        var inProxies = false

        for (rawLine in text.lineSequence()) {
            val line = rawLine.replace("\t", "  ")
            if (line.isBlank()) continue
            val trimmed = line.trimStart()
            if (!line.startsWith(" ")) {
                inProxies = trimmed.equals("proxies:", ignoreCase = true)
                if (inProxies && current != null) {
                    result.add(current)
                    current = null
                }
                continue
            }
            if (!inProxies) continue
            val content = line.trim()
            if (content.startsWith("- ")) {
                current?.let { result.add(it) }
                current = LinkedHashMap()
                val item = content.removePrefix("- ").trim()
                val colon = item.indexOf(':')
                if (colon > 0) {
                    current[item.substring(0, colon).trim()] = item.substring(colon + 1).trim().trimQuotes()
                }
            } else {
                val map = current ?: continue
                val colon = content.indexOf(':')
                if (colon > 0) {
                    val key = content.substring(0, colon).trim()
                    val value = content.substring(colon + 1).trim().trimQuotes()
                    if (value.isNotEmpty() && !map.containsKey(key)) map[key] = value
                }
            }
        }
        current?.let { result.add(it) }
        return result
    }

    fun parse(text: String, subscriptionId: String? = null): List<ServerProfile> =
        extractProxies(text).mapNotNull { toProfile(it, subscriptionId) }

    fun toProfile(map: Map<String, String>, subscriptionId: String? = null): ServerProfile? {
        val type = map["type"]?.lowercase() ?: return null
        val name = map["name"]?.ifBlank { null }
        val server = map["server"] ?: map["hostname"] ?: return null
        val port = map["port"]?.toIntOrNull() ?: return null
        val params = clashParams(map)

        val (protocol, outbound) = when (type) {
            "ss" -> {
                val method = map["cipher"] ?: return null
                val password = map["password"] ?: return null
                Protocol.SHADOWSOCKS to buildJsonObject {
                    put("protocol", "shadowsocks")
                    putJsonObject("settings") {
                        putJsonArray("servers") {
                            addJsonObject {
                                put("address", server)
                                put("port", port)
                                put("method", method)
                                put("password", password)
                                put("level", 0)
                            }
                        }
                    }
                    putJsonObject("streamSettings") { put("network", "raw") }
                }
            }

            "vmess" -> {
                val uuid = map["uuid"] ?: return null
                Protocol.VMESS to buildJsonObject {
                    put("protocol", "vmess")
                    putJsonObject("settings") {
                        putJsonArray("vnext") {
                            addJsonObject {
                                put("address", server)
                                put("port", port)
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("id", uuid)
                                        put("alterId", map["alterId"]?.toIntOrNull() ?: 0)
                                        put("security", map["cipher"] ?: "auto")
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                    put("streamSettings", LinkParser.buildStreamSettings(LinkParser.Params(params)))
                }
            }

            "vless" -> {
                val uuid = map["uuid"] ?: return null
                Protocol.VLESS to buildJsonObject {
                    put("protocol", "vless")
                    putJsonObject("settings") {
                        putJsonArray("vnext") {
                            addJsonObject {
                                put("address", server)
                                put("port", port)
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("id", uuid)
                                        put("encryption", "none")
                                        map["flow"]?.let { put("flow", it) }
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                    put("streamSettings", LinkParser.buildStreamSettings(LinkParser.Params(params)))
                }
            }

            "trojan" -> {
                val password = map["password"] ?: return null
                Protocol.TROJAN to buildJsonObject {
                    put("protocol", "trojan")
                    putJsonObject("settings") {
                        putJsonArray("servers") {
                            addJsonObject {
                                put("address", server)
                                put("port", port)
                                put("password", password)
                                put("level", 0)
                            }
                        }
                    }
                    put("streamSettings", LinkParser.buildStreamSettings(LinkParser.Params(params)))
                }
            }

            "hysteria2", "hysteria" -> {
                val password = map["password"] ?: map["auth"] ?: map["auth-str"] ?: return null
                Protocol.HYSTERIA2 to buildJsonObject {
                    put("protocol", "hysteria")
                    putJsonObject("settings") {
                        put("version", 2)
                        put("address", server)
                        put("port", port)
                    }
                    putJsonObject("streamSettings") {
                        put("security", "tls")
                        putJsonObject("tlsSettings") {
                            put("serverName", map["sni"] ?: map["servername"] ?: server)
                            put("allowInsecure", map["skip-cert-verify"] == "true")
                            putJsonArray("alpn") { add("h3") }
                        }
                        putJsonObject("hysteriaSettings") {
                            put("version", 2)
                            put("auth", password)
                        }
                    }
                }
            }

            "wireguard" -> {
                val privateKey = map["private-key"] ?: map["privateKey"] ?: return null
                val publicKey = map["public-key"] ?: map["publicKey"] ?: return null
                Protocol.WIREGUARD to buildJsonObject {
                    put("protocol", "wireguard")
                    putJsonObject("settings") {
                        put("secretKey", privateKey)
                        putJsonArray("address") {
                            add(map["ip"] ?: "10.0.0.2/32")
                            map["ipv6"]?.let { add(it) }
                        }
                        putJsonArray("peers") {
                            addJsonObject {
                                put("publicKey", publicKey)
                                put("endpoint", if (server.contains(':')) "[$server]:$port" else "$server:$port")
                                put("keepAlive", map["keepalive"]?.toIntOrNull() ?: 25)
                            }
                        }
                        put("mtu", map["mtu"]?.toIntOrNull() ?: 1420)
                        put("noKernelTun", true)
                    }
                }
            }

            "tuic" -> Protocol.TUIC to null
            "ssr" -> Protocol.SHADOWSOCKSR to null
            "http" -> Protocol.HTTP to buildJsonObject {
                put("protocol", "http")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", server)
                            put("port", port)
                            val user = map["username"]
                            if (!user.isNullOrBlank()) {
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("user", user)
                                        put("pass", map["password"] ?: "")
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "socks", "socks5" -> Protocol.SOCKS to buildJsonObject {
                put("protocol", "socks")
                putJsonObject("settings") {
                    putJsonArray("servers") {
                        addJsonObject {
                            put("address", server)
                            put("port", port)
                            val user = map["username"]
                            if (!user.isNullOrBlank()) {
                                putJsonArray("users") {
                                    addJsonObject {
                                        put("user", user)
                                        put("pass", map["password"] ?: "")
                                        put("level", 0)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            else -> return null
        }

        return ServerProfile(
            id = UUID.randomUUID().toString(),
            subscriptionId = subscriptionId,
            protocol = protocol,
            remark = name ?: "$server:$port",
            address = server,
            port = port,
            outbound = outbound,
            link = "",
            addedAt = System.currentTimeMillis()
        )
    }

    /** Maps Clash keys onto the parameter names used by share links. */
    private fun clashParams(map: Map<String, String>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        map["network"]?.let { out["type"] = it }
        if (map["tls"] == "true") out["security"] = "tls"
        map["servername"]?.let { out["sni"] = it }
        map["sni"]?.let { out["sni"] = it }
        map["skip-cert-verify"]?.let { out["allowInsecure"] = it }
        map["client-fingerprint"]?.let { out["fp"] = it }
        map["fingerprint"]?.let { out["fp"] = it }
        map["path"]?.let { out["path"] = it }
        map["Host"]?.let { out["host"] = it }
        map["host"]?.let { out["host"] = it }
        map["public-key"]?.let { out["pbk"] = it }
        map["short-id"]?.let { out["sid"] = it }
        map["serviceName"]?.let { out["serviceName"] = it }
        map["grpc-service-name"]?.let { out["serviceName"] = it }
        (map["alpn"] ?: map["alpn-list"])?.let { out["alpn"] = it.trim('[', ']', '"') }
        return out
    }

    private fun String.trimQuotes(): String = trim().trim('"').trim('\'')
}
