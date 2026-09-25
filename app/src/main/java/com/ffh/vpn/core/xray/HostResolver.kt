package com.ffh.vpn.core.xray

import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

/** Resolves a dial host to IPv4 before the tunnel is up. Bounded so a stuck resolver cannot hang connect. */
object HostResolver {

    fun resolveV4(host: String, timeoutMs: Long = 4_000): String? {
        val bare = host.trim()
        if (bare.isEmpty()) return null
        if (OutboundDial.isIp(bare)) {
            return if (bare.contains(':')) null else bare.removePrefix("[").removeSuffix("]")
        }
        val found = AtomicReference<String?>(null)
        val worker = Thread {
            found.set(
                runCatching {
                    InetAddress.getAllByName(bare)
                        .firstOrNull { it is Inet4Address }
                        ?.hostAddress
                }.getOrNull()
            )
        }
        worker.isDaemon = true
        worker.name = "ffh-dns"
        worker.start()
        worker.join(timeoutMs)
        return found.get()
    }
}
