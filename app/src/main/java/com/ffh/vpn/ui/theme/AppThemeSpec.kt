package com.ffh.vpn.ui.theme

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The custom theme format of the app.
 *
 * A theme is a plain JSON object, exactly like this one:
 * ```json
 * {"backgroundGradientRotationAngle":0,"backgroundColors":["#000000FF", ...]}
 * ```
 * Any subset of fields may be present — everything missing falls back to the
 * default monochrome theme, and unknown fields are ignored, so themes coming
 * from other clients keep working.
 */
@Serializable
data class AppThemeSpec(
    @SerialName("backgroundGradientRotationAngle")
    val backgroundGradientRotationAngle: Float = 0f,

    @SerialName("backgroundGradientColorIntensity")
    val backgroundGradientColorIntensity: Float = 0f,

    @SerialName("backgroundColors")
    val backgroundColors: List<String> = DEFAULT_BACKGROUND_COLORS,

    @SerialName("serverRowBackgroundColor")
    val serverRowBackgroundColor: String = "#050505FF",

    @SerialName("selectedServerRowColor")
    val selectedServerRowColor: String = "#151515FF",

    @SerialName("subsHeaderColor")
    val subsHeaderColor: String = "#0A0A0AFF",

    @SerialName("buttonColor")
    val buttonColor: String = "#FFFFFFFF",

    @SerialName("buttonTextColor")
    val buttonTextColor: String = "#000000FF",

    @SerialName("powerIconColor")
    val powerIconColor: String = "#000000FF",

    @SerialName("serverRowTitleTextColor")
    val serverRowTitleTextColor: String = "#FFFFFFFF",

    @SerialName("serverRowSubTitleTextColor")
    val serverRowSubTitleTextColor: String = "#888888FF",

    @SerialName("topBarButtonsColor")
    val topBarButtonsColor: String = "#FFFFFFFF",

    @SerialName("supportIconColor")
    val supportIconColor: String = "#FFFFFFFF",

    @SerialName("profileWebPageIconColor")
    val profileWebPageIconColor: String = "#FFFFFFFF",

    @SerialName("subHeaderButtonColor")
    val subHeaderButtonColor: String = "#FFFFFFFF",

    @SerialName("settingsControlsTintColor")
    val settingsControlsTintColor: String = "#FFFFFFFF",

    @SerialName("subscriptionInfoBackgroundColor")
    val subscriptionInfoBackgroundColor: String = "#080808FF",

    @SerialName("subscriptionTrafficBackgroundColor")
    val subscriptionTrafficBackgroundColor: String = "#111111FF",

    @SerialName("subscriptionInfoTextColor")
    val subscriptionInfoTextColor: String = "#FFFFFFFF",

    @SerialName("disclosureHeaderTextColor")
    val disclosureHeaderTextColor: String = "#FFFFFFFF",

    @SerialName("disclosureSubHeaderTextColor")
    val disclosureSubHeaderTextColor: String = "#888888FF",

    @SerialName("serverRowChevronColor")
    val serverRowChevronColor: String = "#FFFFFFFF",

    @SerialName("additionalOptionsButtonColor")
    val additionalOptionsButtonColor: String = "#FFFFFFFF",

    @SerialName("buttonTimerColor")
    val buttonTimerColor: String = "#FFFFFFFF",

    @SerialName("elipseColors")
    val elipseColors: List<String> = DEFAULT_ELIPSE_COLORS,

    /** `system` — gradient + soft ellipses, `none` — flat colour, `custom` — user image. */
    @SerialName("backgroundImageType")
    val backgroundImageType: String = "system",

    /** `light` or `dark` — picks the glyph style of the connect button. */
    @SerialName("buttonImageType")
    val buttonImageType: String = "light"
) {
    companion object {
        val DEFAULT_BACKGROUND_COLORS = listOf("#000000FF", "#000000FF", "#000000FF")
        val DEFAULT_ELIPSE_COLORS = listOf("#161616FF", "#090909FF", "#000000FF")

        /** The theme the app starts with: pure black, white accents, no gradient. */
        val DEFAULT: AppThemeSpec by lazy { AppThemeSpec() }
    }
}

/** Lenient parser: tolerates unknown keys, wrong types and missing fields. */
private val themeJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}

private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/** Theme fields used to decide whether an arbitrary text is a theme at all. */
private val THEME_MARKERS = listOf(
    "backgroundColors",
    "serverRowBackgroundColor",
    "buttonColor",
    "elipseColors",
    "subsHeaderColor"
)

/**
 * True for texts that look like a theme definition.
 *
 * Used when the user pastes something into the "add subscription" field: if it
 * is a theme, the app applies it instead of trying to import servers from it.
 */
fun looksLikeThemeJson(text: String?): Boolean {
    val raw = text?.trim() ?: return false
    if (!raw.startsWith("{") || !raw.endsWith("}")) return false
    var score = 0
    for (marker in THEME_MARKERS) {
        if (raw.contains("\"$marker\"")) score++
        if (raw.contains(marker)) score += 0
    }
    return score >= 2
}

/**
 * Parses a theme, repairing anything that is off: short colour lists get
 * extended, unknown image types fall back to the defaults.
 */
fun parseTheme(input: String?): AppThemeSpec? {
    val raw = input?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val spec = runCatching { themeJson.decodeFromString<AppThemeSpec>(raw) }.getOrNull()
        ?: runCatching { themeJson.decodeFromString<AppThemeSpec>(sanitize(raw)) }.getOrNull()
        ?: return null
    return spec.normalized()
}

private fun sanitize(raw: String): String = raw
    .replace("\u201C", "\"")
    .replace("\u201D", "\"")
    .replace('\u00A0', ' ')

/** Makes sure the theme is always complete and safe to render. */
fun AppThemeSpec.normalized(): AppThemeSpec = copy(
    backgroundGradientRotationAngle =
        if (backgroundGradientRotationAngle.isFinite()) backgroundGradientRotationAngle % 360f else 0f,
    backgroundGradientColorIntensity =
        if (backgroundGradientColorIntensity.isFinite()) backgroundGradientColorIntensity.coerceIn(0f, 1f) else 0f,
    backgroundColors = backgroundColors.normalizedColors(AppThemeSpec.DEFAULT_BACKGROUND_COLORS),
    elipseColors = elipseColors.normalizedColors(AppThemeSpec.DEFAULT_ELIPSE_COLORS),
    backgroundImageType = backgroundImageType.lowercase().let {
        if (it in setOf("system", "none", "custom")) it else "system"
    },
    buttonImageType = buttonImageType.lowercase().let {
        if (it in setOf("light", "dark")) it else "light"
    }
)

private fun List<String>.normalizedColors(fallback: List<String>): List<String> {
    val cleaned = map { it.trim() }.filter { it.isNotEmpty() }.map {
        if (it.startsWith("#")) it else "#$it"
    }
    if (cleaned.isEmpty()) return fallback
    return when {
        cleaned.size >= 3 -> cleaned.take(3)
        cleaned.size == 2 -> cleaned + cleaned.last()
        else -> List(3) { cleaned.first() }
    }
}

fun AppThemeSpec.toJson(pretty: Boolean = true): String =
    (if (pretty) prettyJson else themeJson).encodeToString(this)

/** Resolved colours, ready to be used by composables. */
data class FfhPalette(
    val backgroundGradientRotationAngle: Float,
    val backgroundGradientColorIntensity: Float,
    val backgroundColors: List<Color>,
    val serverRowBackgroundColor: Color,
    val selectedServerRowColor: Color,
    val subsHeaderColor: Color,
    val buttonColor: Color,
    val buttonTextColor: Color,
    val powerIconColor: Color,
    val serverRowTitleTextColor: Color,
    val serverRowSubTitleTextColor: Color,
    val topBarButtonsColor: Color,
    val supportIconColor: Color,
    val profileWebPageIconColor: Color,
    val subHeaderButtonColor: Color,
    val settingsControlsTintColor: Color,
    val subscriptionInfoBackgroundColor: Color,
    val subscriptionTrafficBackgroundColor: Color,
    val subscriptionInfoTextColor: Color,
    val disclosureHeaderTextColor: Color,
    val disclosureSubHeaderTextColor: Color,
    val serverRowChevronColor: Color,
    val additionalOptionsButtonColor: Color,
    val buttonTimerColor: Color,
    val elipseColors: List<Color>,
    val backgroundImageType: String,
    val buttonImageType: String
) {
    /** Gradient colours with the intensity knob applied. */
    val gradientColors: List<Color>
        get() = backgroundColors.map { it.withIntensity(backgroundGradientColorIntensity) }

    val baseBackground: Color get() = gradientColors.first()

    val isLightBackground: Boolean get() = baseBackground.isBright()

    val hairline: Color get() = serverRowSubTitleTextColor.copy(alpha = 0.18f)

    companion object {
        val DEFAULT: FfhPalette by lazy { AppThemeSpec.DEFAULT.toPalette() }
    }
}

fun AppThemeSpec.toPalette(): FfhPalette = FfhPalette(
    backgroundGradientRotationAngle = backgroundGradientRotationAngle,
    backgroundGradientColorIntensity = backgroundGradientColorIntensity,
    backgroundColors = backgroundColors.map { parseHexColor(it, Color.Black) },
    serverRowBackgroundColor = parseHexColor(serverRowBackgroundColor, Color(0xFF050505)),
    selectedServerRowColor = parseHexColor(selectedServerRowColor, Color(0xFF151515)),
    subsHeaderColor = parseHexColor(subsHeaderColor, Color(0xFF0A0A0A)),
    buttonColor = parseHexColor(buttonColor, Color.White),
    buttonTextColor = parseHexColor(buttonTextColor, Color.Black),
    powerIconColor = parseHexColor(powerIconColor, Color.Black),
    serverRowTitleTextColor = parseHexColor(serverRowTitleTextColor, Color.White),
    serverRowSubTitleTextColor = parseHexColor(serverRowSubTitleTextColor, Color(0xFF888888)),
    topBarButtonsColor = parseHexColor(topBarButtonsColor, Color.White),
    supportIconColor = parseHexColor(supportIconColor, Color.White),
    profileWebPageIconColor = parseHexColor(profileWebPageIconColor, Color.White),
    subHeaderButtonColor = parseHexColor(subHeaderButtonColor, Color.White),
    settingsControlsTintColor = parseHexColor(settingsControlsTintColor, Color.White),
    subscriptionInfoBackgroundColor = parseHexColor(subscriptionInfoBackgroundColor, Color(0xFF080808)),
    subscriptionTrafficBackgroundColor = parseHexColor(subscriptionTrafficBackgroundColor, Color(0xFF111111)),
    subscriptionInfoTextColor = parseHexColor(subscriptionInfoTextColor, Color.White),
    disclosureHeaderTextColor = parseHexColor(disclosureHeaderTextColor, Color.White),
    disclosureSubHeaderTextColor = parseHexColor(disclosureSubHeaderTextColor, Color(0xFF888888)),
    serverRowChevronColor = parseHexColor(serverRowChevronColor, Color.White),
    additionalOptionsButtonColor = parseHexColor(additionalOptionsButtonColor, Color.White),
    buttonTimerColor = parseHexColor(buttonTimerColor, Color.White),
    elipseColors = elipseColors.map { parseHexColor(it, Color.Black) },
    backgroundImageType = backgroundImageType,
    buttonImageType = buttonImageType
)
