package com.ffh.vpn.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSpecTest {

    private val referenceTheme = """
        {"backgroundGradientRotationAngle":0,"backgroundGradientColorIntensity":0,
         "backgroundColors":["#000000FF","#000000FF","#000000FF"],
         "serverRowBackgroundColor":"#050505FF","selectedServerRowColor":"#151515FF",
         "subsHeaderColor":"#0A0A0AFF","buttonColor":"#FFFFFFFF","buttonTextColor":"#000000FF",
         "powerIconColor":"#000000FF","serverRowTitleTextColor":"#FFFFFFFF",
         "serverRowSubTitleTextColor":"#888888FF","topBarButtonsColor":"#FFFFFFFF",
         "supportIconColor":"#FFFFFFFF","profileWebPageIconColor":"#FFFFFFFF",
         "subHeaderButtonColor":"#FFFFFFFF","settingsControlsTintColor":"#FFFFFFFF",
         "subscriptionInfoBackgroundColor":"#080808FF","subscriptionTrafficBackgroundColor":"#111111FF",
         "subscriptionInfoTextColor":"#FFFFFFFF","disclosureHeaderTextColor":"#FFFFFFFF",
         "disclosureSubHeaderTextColor":"#888888FF","serverRowChevronColor":"#FFFFFFFF",
         "additionalOptionsButtonColor":"#FFFFFFFF","buttonTimerColor":"#FFFFFFFF",
         "elipseColors":["#161616FF","#090909FF","#000000FF"],
         "backgroundImageType":"system","buttonImageType":"light"}
    """.trimIndent()

    @Test
    fun `reference theme is recognised as a theme`() {
        assertTrue(looksLikeThemeJson(referenceTheme))
    }

    @Test
    fun `subscription link is not a theme`() {
        assertFalse(looksLikeThemeJson("https://example.com/sub?token=abc"))
        assertFalse(looksLikeThemeJson("vless://uuid@host:443?type=ws#name"))
        assertFalse(looksLikeThemeJson(null))
    }

    @Test
    fun `reference theme round trips through json`() {
        val spec = parseTheme(referenceTheme)
        assertNotNull(spec)
        checkNotNull(spec)

        assertEquals(0f, spec.backgroundGradientRotationAngle)
        assertEquals("#000000FF", spec.backgroundColors.first())
        assertEquals("#FFFFFFFF", spec.buttonColor)
        assertEquals(3, spec.elipseColors.size)

        val palette = spec.toPalette()
        assertEquals(0xFF000000.toInt(), palette.baseBackground.toPackedInt())
        assertEquals(0xFF050505.toInt(), palette.serverRowBackgroundColor.toPackedInt())

        val exported = spec.toJson()
        val reparsed = parseTheme(exported)
        assertEquals(spec, reparsed)
    }

    @Test
    fun `partial themes fall back to defaults`() {
        val spec = parseTheme("""{"buttonColor":"#123456FF"}""")
        assertNotNull(spec)
        checkNotNull(spec)
        assertEquals("#123456FF", spec.buttonColor)
        assertEquals(AppThemeSpec.DEFAULT.serverRowBackgroundColor, spec.serverRowBackgroundColor)
        assertEquals(3, spec.backgroundColors.size)
        assertEquals(3, spec.elipseColors.size)
    }

    @Test
    fun `short colour lists are expanded`() {
        val spec = parseTheme("""{"backgroundColors":["#FF0000FF"],"elipseColors":[]}""")
        assertNotNull(spec)
        checkNotNull(spec)
        assertEquals(listOf("#FF0000FF", "#FF0000FF", "#FF0000FF"), spec.backgroundColors)
        assertEquals(AppThemeSpec.DEFAULT_ELIPSE_COLORS, spec.elipseColors)
    }

    @Test
    fun `garbage input is rejected without throwing`() {
        assertNull(parseTheme("not a theme"))
        assertNull(parseTheme(""))
        assertNull(parseTheme(null))
    }

    @Test
    fun `colours without hash are accepted`() {
        val spec = parseTheme("""{"buttonColor":"00FF0080"}""")
        assertNotNull(spec)
        // RRGGBBAA -> packed ARGB
        assertEquals(0x8000FF00.toInt(), parseHexColor(spec!!.buttonColor).toPackedInt())
    }

    @Test
    fun `hex parsing understands every supported shape`() {
        assertEquals(0xFFFFFFFF.toInt(), parseHexColor("#FFFFFFFF").toPackedInt())
        assertEquals(0xFF000000.toInt(), parseHexColor("#000").toPackedInt())
        assertEquals(0x00000000, parseHexColor("#0000").toPackedInt())
        assertEquals(0xFF112233.toInt(), parseHexColor("112233").toPackedInt())
        assertEquals(0x80112233.toInt(), parseHexColor("#11223380").toPackedInt())
        assertEquals(0xFF000000.toInt(), parseHexColor("nonsense").toPackedInt())
    }

    @Test
    fun `intensity keeps gray gray and only brightens it`() {
        val gray = parseHexColor("#808080FF")
        val boosted = gray.withIntensity(0.5f)
        assertEquals(boosted.red, boosted.green, 0.001f)
        assertEquals(boosted.green, boosted.blue, 0.001f)
        assertTrue(boosted.red >= gray.red)
        // zero intensity is a no-op
        assertEquals(gray.red, gray.withIntensity(0f).red, 0.001f)
        val red = parseHexColor("#FF0000FF")
        assertTrue(red.withIntensity(1f).red >= red.red)
    }

    @Test
    fun `presets are valid themes`() {
        ThemePresets.all.forEach { preset ->
            val json = preset.spec.toJson()
            val parsed = parseTheme(json)
            assertNotNull("${preset.id} must round trip", parsed)
            assertEquals(preset.spec.normalized(), parsed)
            assertTrue(preset.id.isNotBlank())
        }
    }
}

private fun androidx.compose.ui.graphics.Color.toPackedInt(): Int {
    fun part(value: Float) = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (part(alpha) shl 24) or (part(red) shl 16) or (part(green) shl 8) or part(blue)
}
