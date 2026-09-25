package com.ffh.vpn.core.xray

import com.ffh.vpn.data.AppSettings
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.data.parse.LinkParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks the document that is handed to Xray-core: inbound tags, the TUN
 * settings, the outbound built from a share link and the routing table.
 */
class XrayConfigBuilderTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val vlessLink = "vless://11111111-1111-1111-1111-111111111111@example.com:443" +
        "?encryption=none&security=tls&sni=example.com&type=ws&path=%2Fws&host=example.com#Home"

    private fun config(profile: ServerProfile, settings: AppSettings = AppSettings()): JsonObject {
        val text = XrayConfigBuilder.build(profile, settings)
        return json.parseToJsonElement(text).jsonObject
    }

    private fun JsonObject.array(key: String): JsonArray = getValue(key).jsonArray
    private fun JsonObject.obj(key: String): JsonObject = getValue(key).jsonObject

    private fun JsonArray.tag(tag: String): JsonObject? =
        firstOrNull { it.jsonObject["tag"]?.jsonPrimitive?.content == tag }?.jsonObject

    @Test
    fun `tun inbound is configured with the file descriptor name and mtu`() {
        val profile = LinkParser.parse(vlessLink)!!
        val inbounds = config(profile).array("inbounds")
        val tun = inbounds.tag("tun-in") ?: error("no tun-in inbound")

        assertEquals("tun", tun["protocol"]?.jsonPrimitive?.content)
        val settings = tun.obj("settings")
        assertEquals("tun0", settings["name"]?.jsonPrimitive?.content)
        assertEquals(
            AppSettings().tunMtu.toLong(),
            settings["mtu"]?.jsonPrimitive?.content?.toLong()
        )
        // sniffing keeps the original destination while still naming the domain
        val sniffing = tun.obj("sniffing")
        assertEquals(true, sniffing["enabled"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals(true, sniffing["routeOnly"]?.jsonPrimitive?.content?.toBoolean())
    }

    @Test
    fun `proxy outbound keeps the parsed link settings`() {
        val profile = LinkParser.parse(vlessLink)!!
        val outbounds = config(profile).array("outbounds")
        val proxy = outbounds.tag(XrayConfigBuilder.TAG_PROXY) ?: error("no proxy outbound")

        assertEquals("vless", proxy["protocol"]?.jsonPrimitive?.content)
        val vnext = proxy.obj("settings").array("vnext")
        assertEquals("example.com", vnext[0].jsonObject["address"]?.jsonPrimitive?.content)
        val stream = proxy.obj("streamSettings")
        assertEquals("ws", stream["network"]?.jsonPrimitive?.content)
        assertEquals("tls", stream["security"]?.jsonPrimitive?.content)
        assertEquals("/ws", stream.obj("wsSettings")["path"]?.jsonPrimitive?.content)
        assertEquals("example.com", stream.obj("wsSettings")["host"]?.jsonPrimitive?.content)
        assertEquals("none", config(profile).obj("log")["access"]?.jsonPrimitive?.content)
    }

    @Test
    fun `direct and blackhole outbounds exist`() {
        val profile = LinkParser.parse(vlessLink)!!
        val outbounds = config(profile).array("outbounds")
        assertEquals("freedom", outbounds.tag("direct")?.get("protocol")?.jsonPrimitive?.content)
        assertEquals("blackhole", outbounds.tag("block")?.get("protocol")?.jsonPrimitive?.content)
    }

    @Test
    fun `dns goes through the proxy by default`() {
        val profile = LinkParser.parse(vlessLink)!!
        val rules = config(profile).obj("routing").array("rules")
        val dnsRule = rules.first {
            it.jsonObject["port"]?.jsonPrimitive?.content == "53"
        }.jsonObject
        assertEquals("proxy", dnsRule["outboundTag"]?.jsonPrimitive?.content)
    }

    @Test
    fun `local dns mode never uses an outbound the core does not have`() {
        val profile = LinkParser.parse(vlessLink)!!
        val rules = config(profile, AppSettings(dnsMode = "local")).obj("routing").array("rules")
        val dnsRule = rules.first {
            it.jsonObject["port"]?.jsonPrimitive?.content == "53"
        }.jsonObject
        assertEquals("direct", dnsRule["outboundTag"]?.jsonPrimitive?.content)
        // every rule points at an outbound that really exists
        val tags = config(profile).array("outbounds").map { it.jsonObject["tag"]?.jsonPrimitive?.content }
        rules.forEach {
            val tag = it.jsonObject["outboundTag"]?.jsonPrimitive?.content
            assertNotNull(tag)
            assertTrue("unknown outbound: $tag", tag in tags)
        }
    }

    @Test
    fun `bypass mode routes local networks to direct`() {
        val profile = LinkParser.parse(vlessLink)!!
        val settings = AppSettings(routingMode = "bypass_lan")
        val rules = config(profile, settings).obj("routing").array("rules")
        val lanRule = rules.first { it.jsonObject.containsKey("ip") && it.jsonObject["outboundTag"]?.jsonPrimitive?.content == "direct" }
        val subnets = lanRule.jsonObject.array("ip").map { it.jsonPrimitive.content }
        assertTrue(subnets.contains("192.168.0.0/16"))
        assertTrue(subnets.contains("10.0.0.0/8"))
    }

    @Test
    fun `global mode has no lan bypass rule`() {
        val profile = LinkParser.parse(vlessLink)!!
        val rules = config(profile, AppSettings(routingMode = "global")).obj("routing").array("rules")
        val hasBypass = rules.any {
            it.jsonObject.containsKey("ip") && it.jsonObject["outboundTag"]?.jsonPrimitive?.content == "direct"
        }
        assertEquals(false, hasBypass)
    }

    @Test
    fun `local proxies are optional and bound to localhost`() {
        val profile = LinkParser.parse(vlessLink)!!
        val noProxy = config(profile, AppSettings(socksEnabled = false, httpEnabled = false)).array("inbounds")
        assertEquals(1, noProxy.size)

        val withProxy = config(profile, AppSettings(allowLan = true)).array("inbounds")
        val socks = withProxy.tag("socks-in") ?: error("no socks inbound")
        assertEquals("0.0.0.0", socks["listen"]?.jsonPrimitive?.content)
    }

    @Test
    fun `dns servers are configurable and quoted in plain form`() {
        val profile = LinkParser.parse(vlessLink)!!
        val dns = config(profile, AppSettings(dnsServers = listOf("1.1.1.1", "https://dns.google/dns-query")))
            .obj("dns").array("servers")
        val plain = dns.first { it is JsonPrimitive }.jsonPrimitive.content
        assertEquals("1.1.1.1", plain)
        val doh = dns.first { it is JsonObject && it.jsonObject["address"]?.jsonPrimitive?.content?.startsWith("https://") == true }
        assertEquals("https://dns.google/dns-query", doh.jsonObject["address"]?.jsonPrimitive?.content)
    }

    @Test
    fun `server domain is resolved by the system resolver so the tunnel can connect`() {
        val profile = LinkParser.parse(vlessLink)!!
        val servers = config(profile).obj("dns").array("servers")
        val bootstrap = servers.first {
            it is JsonObject && it.jsonObject["address"]?.jsonPrimitive?.content == "localhost"
        }.jsonObject
        val domains = bootstrap.array("domains").map { it.jsonPrimitive.content }
        assertTrue(domains.contains("full:example.com"))
    }

    @Test
    fun `unsupported protocols cannot produce a config`() {
        val profile = LinkParser.parse("tuic://11111111-1111-1111-1111-111111111111:pw@example.com:443#T")!!
        assertEquals(false, profile.isSupported)
        var thrown = false
        try {
            XrayConfigBuilder.build(profile, AppSettings())
        } catch (t: Throwable) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
