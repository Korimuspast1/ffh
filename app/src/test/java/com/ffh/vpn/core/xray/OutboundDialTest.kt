package com.ffh.vpn.core.xray

import com.ffh.vpn.data.parse.LinkParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutboundDialTest {

    @Test
    fun `ip detection accepts v4 and rejects domains`() {
        assertTrue(OutboundDial.isIp("1.1.1.1"))
        assertTrue(OutboundDial.isIp("2001:db8::1"))
        assertFalse(OutboundDial.isIp("example.com"))
        assertFalse(OutboundDial.isIp("1.2.3"))
    }

    @Test
    fun `rewriting the dial address keeps tls server name`() {
        val link = "vless://11111111-1111-1111-1111-111111111111@example.com:443" +
            "?encryption=none&security=tls&sni=www.example.com&type=tcp#Home"
        val profile = LinkParser.parse(link)!!
        val rewritten = OutboundDial.rewrite(profile.outbound!!, "203.0.113.10")

        val vnext = rewritten["settings"]!!.jsonObject["vnext"]!!.jsonArray[0].jsonObject
        assertEquals("203.0.113.10", vnext["address"]!!.jsonPrimitive.content)
        val sni = rewritten["streamSettings"]!!.jsonObject["tlsSettings"]!!
            .jsonObject["serverName"]!!.jsonPrimitive.content
        assertEquals("www.example.com", sni)
    }

    @Test
    fun `websocket host moves out of headers`() {
        val stored = Json.parseToJsonElement(
            """{"protocol":"vless","streamSettings":{"network":"ws","wsSettings":{"path":"/ws","headers":{"Host":"cdn.example"}}}}"""
        ).jsonObject
        val fixed = OutboundDial.normalize(stored)
        val ws = fixed["streamSettings"]!!.jsonObject["wsSettings"]!!.jsonObject
        assertEquals("cdn.example", ws["host"]!!.jsonPrimitive.content)
        assertFalse(ws.containsKey("headers"))
    }

    @Test
    fun `wireguard endpoint keeps its port`() {
        assertEquals("203.0.113.10:2408", OutboundDial.rewriteEndpoint("vpn.example:2408", "203.0.113.10"))
        assertEquals("[2001:db8::1]:2408", OutboundDial.rewriteEndpoint("[2001:db8::2]:2408", "2001:db8::1"))
        assertEquals("vpn.example", OutboundDial.endpointHost("vpn.example:2408"))
    }
}
