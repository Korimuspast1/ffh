package com.ffh.vpn.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerSortTest {

    private fun server(name: String, ping: Int) = ServerProfile(
        id = name,
        remark = name,
        address = "10.0.0.1",
        port = 443,
        pingMillis = ping
    )

    @Test
    fun `ping sort puts the lowest measured ping first`() {
        val sorted = listOf(
            server("slow", 400),
            server("unknown", 0),
            server("fast", 40),
            server("mid", 120)
        ).sortedWithMode("ping")

        assertEquals(listOf("fast", "mid", "slow", "unknown"), sorted.map { it.remark })
    }

    @Test
    fun `name sort ignores ping`() {
        val sorted = listOf(
            server("zeta", 10),
            server("alpha", 900)
        ).sortedWithMode("name")
        assertEquals(listOf("alpha", "zeta"), sorted.map { it.remark })
    }

    @Test
    fun `default sort keeps the subscription order`() {
        val original = listOf(server("b", 10), server("a", 1))
        assertEquals(original, original.sortedWithMode("default"))
    }
}
