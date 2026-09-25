package com.ffh.vpn.data

import com.ffh.vpn.ui.theme.AppThemeSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against companion defaults that silently recurse into themselves —
 * which is exactly how a seemingly harmless `val DEFAULT = AppSettings()`
 * inside the class companion turns into a StackOverflowError at start-up.
 */
class DefaultsTest {

    @Test
    fun `app settings can be constructed`() {
        assertNotNull(AppSettings())
        assertNotNull(AppSettings.DEFAULT)
        assertEquals(AppSettings.DEFAULT.tunMtu, AppSettings().tunMtu)
        assertTrue(AppSettings().dnsServers.isNotEmpty())
        assertTrue(AppSettings().bypassSubnets.contains("192.168.0.0/16"))
    }

    @Test
    fun `all settings can be written and read back`() {
        val json = JsonStore.encode(AppSettings(socksPort = 9999, language = "ru"))
        val decoded = JsonStore.decode<AppSettings>(json)
        assertEquals(9999, decoded?.socksPort)
        assertEquals("ru", decoded?.language)
    }

    @Test
    fun `app state survives a json round trip`() {
        val state = AppState(settings = AppSettings(themePresetId = "graphite"))
        val text = JsonStore.encode(state)
        val decoded = JsonStore.decode<AppState>(text)
        assertEquals("graphite", decoded?.settings?.themePresetId)
        assertTrue(decoded?.subscriptions.isNullOrEmpty())
    }

    @Test
    fun `theme defaults can be constructed`() {
        assertNotNull(AppThemeSpec.DEFAULT)
        assertEquals("#000000FF", AppThemeSpec.DEFAULT.backgroundColors.first())
    }
}
