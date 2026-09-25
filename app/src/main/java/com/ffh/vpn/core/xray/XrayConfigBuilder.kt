package com.ffh.vpn.core.xray

import com.ffh.vpn.data.AppSettings
import com.ffh.vpn.data.model.ServerProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Builds the complete Xray configuration: TUN inbound, local SOCKS/HTTP
 * inbounds, the selected outbound, DNS and the routing table.
 */
object XrayConfigBuilder {

    const val TAG_PROXY = "proxy"
    const val TAG_DIRECT = "direct"
    const val TAG_BLOCK = "block"
    /** Xray has no `dns-out` handler, so "local" DNS is served by [TAG_DIRECT]. */
    const val TUN_NAME = "tun0"

    private val pretty = Json { prettyPrint = true }

    fun build(server: ServerProfile, settings: AppSettings): String {
        val outbound = server.outbound ?: error("server has no outbound")
        val dialHost = OutboundDial.hostOf(outbound) ?: server.address

        val root = buildJsonObject {
            putJsonObject("log") {
                put("loglevel", settings.logLevel)
                put("access", "")
            }
            put("dns", buildDns(settings, dialHost))
            putJsonArray("inbounds") {
                add(buildTunInbound(settings))
                if (settings.socksEnabled) add(buildSocksInbound(settings))
                if (settings.httpEnabled) add(buildHttpInbound(settings))
            }
            putJsonArray("outbounds") {
                add(withTag(outbound, TAG_PROXY))
                add(buildJsonObject {
                    put("tag", TAG_DIRECT)
                    put("protocol", "freedom")
                    putJsonObject("settings") { put("domainStrategy", "UseIP") }
                })
                add(buildJsonObject {
                    put("tag", TAG_BLOCK)
                    put("protocol", "blackhole")
                })
            }
            putJsonObject("routing") {
                put("domainStrategy", "AsIs")
                putJsonArray("rules") {
                    for (rule in buildRules(settings)) add(rule)
                }
            }
            putJsonObject("policy") {
                putJsonObject("levels") {
                    putJsonObject("0") {
                        put("handshake", 4)
                        put("connIdle", 300)
                        put("uplinkOnly", 2)
                        put("downlinkOnly", 5)
                        put("statsUserUplink", false)
                        put("statsUserDownlink", false)
                    }
                }
                putJsonObject("system") {
                    put("statsOutboundUplink", false)
                    put("statsOutboundDownlink", false)
                }
            }
        }

        return pretty.encodeToString(root)
    }

    private fun buildDns(settings: AppSettings, dialHost: String): JsonObject = buildJsonObject {
        putJsonArray("servers") {
            // The server name is resolved by the system resolver (the app is
            // excluded from the tunnel) so connecting cannot deadlock on a DNS
            // query that would have to travel through the tunnel itself.
            if (dialHost.isNotBlank() && !OutboundDial.isIp(dialHost)) {
                add(buildJsonObject {
                    put("address", "localhost")
                    putJsonArray("domains") { add("full:$dialHost") }
                    put("skipFallback", true)
                })
            }
            for (server in settings.dnsServers.map { it.trim() }.filter { it.isNotEmpty() }) {
                if (server.startsWith("https://") || server.startsWith("tcp://") || server.startsWith("quic://")) {
                    add(buildJsonObject {
                        put("address", server)
                        put("skipFallback", true)
                    })
                } else {
                    add(JsonPrimitive(server))
                }
            }
            if (settings.dnsServers.isEmpty()) add(JsonPrimitive("1.1.1.1"))
        }
        put("queryStrategy", if (settings.enableIpv6) "UseIP" else "UseIPv4")
        put("disableCache", false)
        put("disableFallback", true)
    }

    private fun buildTunInbound(settings: AppSettings): JsonObject = buildJsonObject {
        put("tag", "tun-in")
        put("port", 0)
        put("protocol", "tun")
        putJsonObject("settings") {
            put("name", TUN_NAME)
            put("mtu", settings.tunMtu)
            // Do not set autoOutboundsInterface: on Android that asks the TUN
            // fd for an interface index, which VpnService does not support, and
            // the inbound fails to start.
        }
        if (settings.sniffing) {
            putJsonObject("sniffing") {
                put("enabled", true)
                putJsonArray("destOverride") {
                    add("http")
                    add("tls")
                    add("quic")
                }
                put("routeOnly", true)
            }
        }
    }

    private fun sniffingObject(): JsonObject = buildJsonObject {
        put("enabled", true)
        putJsonArray("destOverride") {
            add("http")
            add("tls")
            add("quic")
        }
        put("routeOnly", false)
    }

    private fun buildSocksInbound(settings: AppSettings): JsonObject = buildJsonObject {
        put("tag", "socks-in")
        put("listen", if (settings.allowLan) "0.0.0.0" else "127.0.0.1")
        put("port", settings.socksPort)
        put("protocol", "socks")
        putJsonObject("settings") {
            put("auth", "noauth")
            put("udp", true)
            put("ip", if (settings.allowLan) "0.0.0.0" else "127.0.0.1")
        }
        if (settings.sniffing) put("sniffing", sniffingObject())
    }

    private fun buildHttpInbound(settings: AppSettings): JsonObject = buildJsonObject {
        put("tag", "http-in")
        put("listen", if (settings.allowLan) "0.0.0.0" else "127.0.0.1")
        put("port", settings.httpPort)
        put("protocol", "http")
        putJsonObject("settings") {
            put("allowTransparent", true)
        }
        if (settings.sniffing) put("sniffing", sniffingObject())
    }

    private fun buildRules(settings: AppSettings): List<JsonObject> {
        val rules = mutableListOf<JsonObject>()

        // Broadcast / multicast never leaves the device
        rules += buildJsonObject {
            put("type", "field")
            putJsonArray("ip") {
                add("224.0.0.0/4")
                add("255.255.255.255/32")
                add("ff00::/8")
            }
            put("outboundTag", TAG_BLOCK)
        }

        // DNS handling: "remote" resolves through the server, "local" sends the
        // query straight out so that it never travels through the tunnel.
        rules += buildJsonObject {
            put("type", "field")
            putJsonArray("inboundTag") { add("tun-in") }
            put("port", "53")
            put("network", "udp")
            put("outboundTag", if (settings.dnsMode == "local") TAG_DIRECT else TAG_PROXY)
        }

        if (settings.routingMode != "global") {
            val subnets = settings.bypassSubnets + (if (settings.enableIpv6) AppSettings.DEFAULT_BYPASS_V6 else emptyList())
            if (subnets.isNotEmpty()) {
                rules += buildJsonObject {
                    put("type", "field")
                    putJsonArray("ip") { subnets.distinct().forEach { add(it) } }
                    put("outboundTag", TAG_DIRECT)
                }
            }
        }

        if (settings.blockQuic) {
            rules += buildJsonObject {
                put("type", "field")
                put("network", "udp")
                put("port", "443")
                put("outboundTag", TAG_BLOCK)
            }
        }

        rules += buildJsonObject {
            put("type", "field")
            putJsonArray("inboundTag") {
                add("tun-in")
                if (settings.socksEnabled) add("socks-in")
                if (settings.httpEnabled) add("http-in")
            }
            put("outboundTag", TAG_PROXY)
        }

        return rules
    }

    /** Re-tags an outbound that came from a share link. */
    private fun withTag(outbound: JsonObject, tag: String): JsonObject {
        val entries = LinkedHashMap<String, JsonElement>()
        entries["tag"] = JsonPrimitive(tag)
        for ((key, value) in outbound) {
            if (key == "tag") continue
            entries[key] = value
        }
        return JsonObject(entries)
    }

    /** Small helper for the "share config" action in the UI. */
    fun buildOutboundOnly(server: ServerProfile): String {
        val outbound = server.outbound ?: return "{}"
        return pretty.encodeToString(withTag(outbound, TAG_PROXY))
    }
}
