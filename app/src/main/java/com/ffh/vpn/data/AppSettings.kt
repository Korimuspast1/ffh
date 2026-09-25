package com.ffh.vpn.data

import kotlinx.serialization.Serializable

/**
 * Every knob of the client lives here so that settings can be exported,
 * imported and unit tested as a single value.
 */
@Serializable
data class AppSettings(

    // ------------------------------------------------------------ appearance
    /** `system`, `ru` or `en`. */
    val language: String = "system",
    val themePresetId: String = "midnight",
    /** Raw JSON of a custom theme; wins over [themePresetId] when not blank. */
    val customThemeJson: String = "",
    val fontScale: Float = 1f,
    val showPing: Boolean = true,
    val showTrafficInRows: Boolean = true,
    val haptics: Boolean = true,

    // ------------------------------------------------------------ connection
    val tunMtu: Int = 1500,
    val enableIpv6: Boolean = false,
    val allowLan: Boolean = false,
    val excludeSelf: Boolean = true,
    val socksEnabled: Boolean = true,
    val socksPort: Int = 10808,
    val httpEnabled: Boolean = true,
    val httpPort: Int = 10809,
    val sniffing: Boolean = true,
    val blockQuic: Boolean = false,
    val autoConnect: Boolean = false,
    val connectOnBoot: Boolean = false,
    val reconnectOnFailure: Boolean = true,

    // ------------------------------------------------------------------- dns
    /** `remote` — resolve through the proxy, `local` — let the core resolve. */
    val dnsMode: String = "remote",
    val dnsServers: List<String> = DEFAULT_DNS,

    // --------------------------------------------------------------- routing
    /** `global`, `bypass_lan` or `manual`. */
    val routingMode: String = "bypass_lan",
    val bypassSubnets: List<String> = DEFAULT_BYPASS_V4,
    val perAppMode: String = "off",
    val perAppPackages: List<String> = emptyList(),

    // ------------------------------------------------------------------ core
    /** `debug`, `info`, `warning`, `error`, `none`. */
    val logLevel: String = "warning",
    val enableMux: Boolean = false,
    val muxConcurrency: Int = 8,

    // --------------------------------------------------------- subscriptions
    /** 0 disables automatic updates. */
    val autoUpdateHours: Int = 24,
    val updateOnStart: Boolean = true,
    val updateOnlyWifi: Boolean = false,
    val keepServersOnFailure: Boolean = true,
    val userAgent: String = DEFAULT_USER_AGENT,
    val pingOnUpdate: Boolean = true,
    /** `tcp` — a plain TCP connect, `proxy` — a real HTTP GET through the server. */
    val pingMode: String = "proxy",
    val pingUrl: String = "https://www.gstatic.com/generate_204",
    val pingTimeoutMs: Int = 4000,
    val pingConcurrency: Int = 24,

    // -------------------------------------------------------------- selection
    /** `default`, `ping` or `name` — how the server list is ordered. */
    val serverSort: String = "ping",

    val selectedSubscriptionId: String? = null,
    val selectedServerId: String? = null,

    /**
     * Bumped when a saved default has to be corrected once. Old files decode
     * this as 0 and are migrated on the next launch.
     */
    val settingsRevision: Int = 0
) {
    /** One-shot corrections for settings written by older builds. */
    fun migrated(): AppSettings {
        if (settingsRevision >= CURRENT_REVISION) return this
        return copy(
            settingsRevision = CURRENT_REVISION,
            // UDP 443 used to be dropped. Chrome and a lot of apps then sat
            // there until TCP fallback, which looks exactly like "nothing works".
            blockQuic = false
        )
    }
    companion object {
        /** Lazily built: the defaults below are members of this companion. */
        val DEFAULT: AppSettings by lazy { AppSettings() }

        val DEFAULT_DNS = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")

        /** Addresses that must never travel through the tunnel. */
        val DEFAULT_BYPASS_V4 = listOf(
            "0.0.0.0/8",
            "10.0.0.0/8",
            "100.64.0.0/10",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "172.16.0.0/12",
            "192.0.0.0/24",
            "192.0.2.0/24",
            "192.88.99.0/24",
            "192.168.0.0/16",
            "198.18.0.0/15",
            "198.51.100.0/24",
            "203.0.113.0/24",
            "224.0.0.0/4",
            "240.0.0.0/4"
        )

        val DEFAULT_BYPASS_V6 = listOf(
            "::/128",
            "::1/128",
            "fc00::/7",
            "fe80::/10",
            "ff00::/8"
        )

        const val DEFAULT_USER_AGENT = "FFH-VPN/1.0 (Xray-core)"

        val PING_MODES = listOf("proxy", "tcp")
        val SERVER_SORTS = listOf("default", "ping", "name")
        const val CURRENT_REVISION = 2

        val LANGUAGES = listOf("system", "ru", "en")
        val ROUTING_MODES = listOf("global", "bypass_lan", "manual")
        val PER_APP_MODES = listOf("off", "include", "exclude")
        val DNS_MODES = listOf("remote", "local")
        val LOG_LEVELS = listOf("debug", "info", "warning", "error", "none")
    }
}
