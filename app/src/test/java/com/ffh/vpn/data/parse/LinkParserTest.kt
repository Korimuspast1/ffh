package com.ffh.vpn.data.parse

import com.ffh.vpn.data.model.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {

    private val uuid = "b831381d-6324-4d53-ad4f-8cda48b30811"

    private fun base64(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray())

    // ------------------------------------------------------------------ vless

    @Test
    fun `vless link with websocket transport`() {
        val link = "vless://$uuid@example.com:443" +
            "?encryption=none&security=tls&sni=example.com&type=ws&path=%2Fws&host=example.com" +
            "#My%20Server"
        val profile = LinkParser.parse(link, "sub-1")

        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.VLESS, profile.protocol)
        assertEquals("My Server", profile.remark)
        assertEquals("example.com", profile.address)
        assertEquals(443, profile.port)
        assertEquals("sub-1", profile.subscriptionId)
        assertNotNull(profile.outbound)
        assertTrue(profile.isSupported)

        val json = profile.outbound.toString()
        assertTrue(json.contains("\"protocol\":\"vless\""))
        assertTrue(json.contains(uuid))
        assertTrue(json.contains("\"network\":\"ws\""))
        assertTrue(json.contains("\"security\":\"tls\""))
        assertTrue(json.contains("\"path\":\"/ws\""))
    }

    @Test
    fun `vless link with reality`() {
        val link = "vless://$uuid@example.com:443?security=reality&pbk=public&sni=example.com&fp=chrome&sid=abcd&type=tcp&flow=xtls-rprx-vision#Reality"
        val profile = LinkParser.parse(link)
        assertNotNull(profile)
        checkNotNull(profile)
        val json = profile.outbound.toString()
        assertTrue(json.contains("\"security\":\"reality\""))
        assertTrue(json.contains("\"publicKey\":\"public\""))
        assertTrue(json.contains("\"shortId\":\"abcd\""))
        assertTrue(json.contains("\"fingerprint\":\"chrome\""))
        assertTrue(json.contains("\"flow\":\"xtls-rprx-vision\""))
    }

    // ------------------------------------------------------------------ vmess

    @Test
    fun `vmess link from base64 json`() {
        val payload = base64(
            """{"v":"2","ps":"VMess server","add":"example.com","port":"443","id":"$uuid","aid":"0",
               "scy":"auto","net":"ws","type":"none","host":"example.com","path":"/vmess","tls":"tls","sni":"example.com"}"""
                .replace("\n", "").replace("  ", "")
        )
        val profile = LinkParser.parse("vmess://$payload")
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.VMESS, profile.protocol)
        assertEquals("VMess server", profile.remark)
        assertEquals(443, profile.port)
        assertTrue(profile.outbound.toString().contains("\"alterId\":0"))
    }

    // ----------------------------------------------------------------- trojan

    @Test
    fun `trojan link`() {
        val profile = LinkParser.parse("trojan://secret@example.com:443?security=tls&sni=example.com#Trojan")
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.TROJAN, profile.protocol)
        assertEquals("Trojan", profile.remark)
        val json = profile.outbound.toString()
        assertTrue(json.contains("\"password\":\"secret\""))
        assertTrue(json.contains("\"security\":\"tls\""))
    }

    // ------------------------------------------------------------ shadowsocks

    @Test
    fun `shadowsocks sip002 link`() {
        val link = "ss://${base64("aes-256-gcm:password")}@example.com:8388#Shadowsocks"
        val profile = LinkParser.parse(link)
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.SHADOWSOCKS, profile.protocol)
        assertEquals("Shadowsocks", profile.remark)
        val json = profile.outbound.toString()
        assertTrue(json.contains("\"method\":\"aes-256-gcm\""))
        assertTrue(json.contains("\"password\":\"password\""))
    }

    @Test
    fun `shadowsocks legacy link`() {
        val link = "ss://${base64("chacha20-ietf-poly1305:pass@example.com:1234")}#Legacy"
        val profile = LinkParser.parse(link)
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals("example.com", profile.address)
        assertEquals(1234, profile.port)
        assertEquals("Legacy", profile.remark)
        assertTrue(profile.outbound.toString().contains("chacha20-ietf-poly1305"))
    }

    @Test
    fun `shadowsocks 2022 cipher keeps the key intact`() {
        val key = "8J58CT0d5KPt9xkQDdRrbgT9jZ8lWk8S3pM2nQ7vXaY="
        val profile = LinkParser.parse("ss://2022-blake3-aes-256-gcm:$key@example.com:8388#SS2022")
        assertNotNull(profile)
        checkNotNull(profile)
        assertTrue(profile.outbound.toString().contains("2022-blake3-aes-256-gcm"))
    }

    // -------------------------------------------------------------------- hy2

    @Test
    fun `hysteria2 link builds a version 2 outbound`() {
        val profile = LinkParser.parse("hysteria2://letmein@example.com:443?sni=example.com&insecure=1#HY2")
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.HYSTERIA2, profile.protocol)
        val json = profile.outbound.toString()
        assertTrue(json.contains("\"protocol\":\"hysteria\""))
        assertTrue(json.contains("\"version\":2"))
        assertTrue(json.contains("\"auth\":\"letmein\""))
        assertTrue(json.contains("\"allowInsecure\":true"))
    }

    // -------------------------------------------------------------- wireguard

    @Test
    fun `wireguard link`() {
        val profile = LinkParser.parse(
            "wireguard://privateKey@example.com:51820?publickey=publicKey&address=10.0.0.2/32&mtu=1420#WG"
        )
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.WIREGUARD, profile.protocol)
        val json = profile.outbound.toString()
        assertTrue(json.contains("\"secretKey\":\"privateKey\""))
        assertTrue(json.contains("\"endpoint\":\"example.com:51820\""))
    }

    // ------------------------------------------------ unsupported protocols

    @Test
    fun `tuic is parsed but has no xray outbound`() {
        val profile = LinkParser.parse("tuic://$uuid:pass@example.com:443?alpn=h3&sni=example.com#TUIC")
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.TUIC, profile.protocol)
        assertNull(profile.outbound)
        assertEquals(false, profile.isSupported)
    }

    @Test
    fun `ssr is parsed but has no xray outbound`() {
        val inner = base64("pass")
        val remarks = base64("My SSR")
        val body = base64(
            "example.com:1234:auth_aes128_md5:aes-256-cfb:plain:$inner/?obfsparam=&protoparam=&remarks=$remarks"
        )
        val profile = LinkParser.parse("ssr://$body")
        assertNotNull(profile)
        checkNotNull(profile)
        assertEquals(Protocol.SHADOWSOCKSR, profile.protocol)
        assertEquals("My SSR", profile.remark)
        assertNull(profile.outbound)
    }

    // ------------------------------------------------------------------ misc

    @Test
    fun `garbage is rejected`() {
        assertNull(LinkParser.parse(""))
        assertNull(LinkParser.parse("hello world"))
        assertNull(LinkParser.parse("unknown://something"))
    }

    @Test
    fun `urls are parsed correctly`() {
        val url = parseUrl("vless://uuid@[2001:db8::1]:443?type=ws#name")
        assertNotNull(url)
        checkNotNull(url)
        assertEquals("2001:db8::1", url.host)
        assertEquals(443, url.port)
        assertEquals("vless", url.scheme)
        assertEquals("name", url.fragment)
    }
}

class SubscriptionParserTest {

    private fun base64(value: String): String =
        java.util.Base64.getEncoder().encodeToString(value.toByteArray())

    @Test
    fun `plain list of links`() {
        val body = listOf(
            "vless://11111111-1111-1111-1111-111111111111@a.example.com:443#One",
            "trojan://pass@b.example.com:443#Two",
            "",
            "# comment",
            "trojan://broken"
        ).joinToString("\n")

        val result = SubscriptionParser.parseBody(body, "sub")
        assertEquals(2, result.servers.size)
        assertEquals(1, result.skipped)
        assertEquals("One", result.servers[0].remark)
        assertEquals("Two", result.servers[1].remark)
    }

    @Test
    fun `base64 wrapped list of links`() {
        val body = base64(
            "vless://11111111-1111-1111-1111-111111111111@a.example.com:443#One\n" +
                "trojan://pass@b.example.com:443#Two"
        )
        val result = SubscriptionParser.parseBody(body, "sub")
        assertEquals(2, result.servers.size)
    }

    @Test
    fun `clash yaml proxies`() {
        val yaml = """
            port: 7890
            proxies:
              - name: "clash ss"
                type: ss
                server: c.example.com
                port: 443
                cipher: aes-128-gcm
                password: secret
                udp: true
              - name: "clash vmess"
                type: vmess
                server: d.example.com
                port: 443
                uuid: 22222222-2222-2222-2222-222222222222
                alterId: 0
                cipher: auto
                tls: true
                servername: d.example.com
                network: ws
                ws-opts:
                  path: /clash
                  headers:
                    Host: d.example.com
        """.trimIndent()

        val result = SubscriptionParser.parseBody(yaml, "sub")
        assertEquals(2, result.servers.size)
        assertEquals("clash ss", result.servers[0].remark)
        assertEquals("clash vmess", result.servers[1].remark)
        assertTrue(result.servers[1].outbound.toString().contains("\"network\":\"ws\""))
    }

    @Test
    fun `xray json with outbounds`() {
        val json = """
            {"outbounds":[
              {"protocol":"shadowsocks","settings":{"servers":[{"address":"e.example.com","port":443,"method":"aes-256-gcm","password":"p"}]}},
              {"protocol":"trojan","settings":{"servers":[{"address":"f.example.com","port":443,"password":"p"}]}}
            ]}
        """.trimIndent()
        val result = SubscriptionParser.parseBody(json, "sub")
        assertEquals(2, result.servers.size)
        assertEquals("e.example.com", result.servers[0].address)
    }

    @Test
    fun `subscription userinfo header`() {
        val info = SubscriptionParser.parseTrafficHeader(
            "upload=1024; download=2048; total=1073741824; expire=4102444800"
        )
        assertNotNull(info)
        checkNotNull(info)
        assertEquals(1024L, info.upload)
        assertEquals(2048L, info.download)
        assertEquals(1073741824L, info.total)
        assertEquals(4102444800L, info.expire)
        assertEquals(1073741824L - 3072L, info.left)
    }

    @Test
    fun `profile title may be base64`() {
        assertEquals("My Proxy", SubscriptionParser.decodeTitle(base64("My Proxy")))
        assertEquals("Профиль", SubscriptionParser.decodeTitle(base64("Профиль")))
    }

    @Test
    fun `update interval parsing`() {
        assertEquals(24, SubscriptionParser.parseUpdateInterval("24"))
        assertEquals(24, SubscriptionParser.parseUpdateInterval("24 hours"))
        assertEquals(72, SubscriptionParser.parseUpdateInterval("3 day"))
        assertNull(SubscriptionParser.parseUpdateInterval(null))
    }
}
