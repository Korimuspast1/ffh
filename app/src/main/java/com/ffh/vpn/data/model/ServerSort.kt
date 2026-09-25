package com.ffh.vpn.data.model

/**
 * Ordering of the server lists.
 *
 * `ping` puts the fastest servers first; servers without a measurement and
 * servers the core cannot run are pushed to the bottom, so the top of the list
 * is always something that is actually usable.
 */
fun List<ServerProfile>.sortedWithMode(mode: String): List<ServerProfile> = when (mode) {
    "ping" -> sortedWith(
        compareBy<ServerProfile> { if (it.pingMillis > 0) 0 else 1 }
            .thenBy { if (it.pingMillis > 0) it.pingMillis else 0 }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName() }
    )

    "name" -> sortedWith(
        compareBy<ServerProfile>(String.CASE_INSENSITIVE_ORDER) { it.displayName() }
            .thenBy { it.address }
    )

    else -> this
}
