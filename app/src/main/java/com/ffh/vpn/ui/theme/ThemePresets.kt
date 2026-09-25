package com.ffh.vpn.ui.theme

/** Built-in monochrome themes; every one of them can be exported as JSON. */
data class ThemePreset(val id: String, val name: String, val spec: AppThemeSpec)

object ThemePresets {
    /** Pure black, white accents — the default look of the client. */
    val Midnight = ThemePreset(
        id = "midnight",
        name = "Midnight",
        spec = AppThemeSpec.DEFAULT
    )

    /** Softer dark grey rows, still monochrome. */
    val Graphite = ThemePreset(
        id = "graphite",
        name = "Graphite",
        spec = AppThemeSpec(
            backgroundGradientRotationAngle = 135f,
            backgroundGradientColorIntensity = 0.05f,
            backgroundColors = listOf("#0B0B0BFF", "#121212FF", "#050505FF"),
            serverRowBackgroundColor = "#151515FF",
            selectedServerRowColor = "#202020FF",
            subsHeaderColor = "#101010FF",
            buttonColor = "#FFFFFFFF",
            buttonTextColor = "#000000FF",
            powerIconColor = "#000000FF",
            serverRowTitleTextColor = "#FFFFFFFF",
            serverRowSubTitleTextColor = "#9A9A9AFF",
            topBarButtonsColor = "#FFFFFFFF",
            supportIconColor = "#FFFFFFFF",
            profileWebPageIconColor = "#FFFFFFFF",
            subHeaderButtonColor = "#FFFFFFFF",
            settingsControlsTintColor = "#FFFFFFFF",
            subscriptionInfoBackgroundColor = "#131313FF",
            subscriptionTrafficBackgroundColor = "#1C1C1CFF",
            subscriptionInfoTextColor = "#FFFFFFFF",
            disclosureHeaderTextColor = "#FFFFFFFF",
            disclosureSubHeaderTextColor = "#9A9A9AFF",
            serverRowChevronColor = "#FFFFFFFF",
            additionalOptionsButtonColor = "#FFFFFFFF",
            buttonTimerColor = "#FFFFFFFF",
            elipseColors = listOf("#1E1E1EFF", "#101010FF", "#000000FF"),
            backgroundImageType = "system",
            buttonImageType = "light"
        )
    )

    /** Light monochrome for daytime use. */
    val Paper = ThemePreset(
        id = "paper",
        name = "Paper",
        spec = AppThemeSpec(
            backgroundGradientRotationAngle = 90f,
            backgroundGradientColorIntensity = 0f,
            backgroundColors = listOf("#F5F5F5FF", "#EDEDEDFF", "#E4E4E4FF"),
            serverRowBackgroundColor = "#FFFFFFFF",
            selectedServerRowColor = "#EDEDEDFF",
            subsHeaderColor = "#F2F2F2FF",
            buttonColor = "#000000FF",
            buttonTextColor = "#FFFFFFFF",
            powerIconColor = "#FFFFFFFF",
            serverRowTitleTextColor = "#0A0A0AFF",
            serverRowSubTitleTextColor = "#6E6E6EFF",
            topBarButtonsColor = "#0A0A0AFF",
            supportIconColor = "#0A0A0AFF",
            profileWebPageIconColor = "#0A0A0AFF",
            subHeaderButtonColor = "#0A0A0AFF",
            settingsControlsTintColor = "#0A0A0AFF",
            subscriptionInfoBackgroundColor = "#FFFFFFFF",
            subscriptionTrafficBackgroundColor = "#EFEFEFFF",
            subscriptionInfoTextColor = "#0A0A0AFF",
            disclosureHeaderTextColor = "#0A0A0AFF",
            disclosureSubHeaderTextColor = "#6E6E6EFF",
            serverRowChevronColor = "#0A0A0AFF",
            additionalOptionsButtonColor = "#0A0A0AFF",
            buttonTimerColor = "#0A0A0AFF",
            elipseColors = listOf("#FFFFFFFF", "#F0F0F0FF", "#E0E0E0FF"),
            backgroundImageType = "system",
            buttonImageType = "dark"
        )
    )

    /** Maximum contrast, nothing but black and white. */
    val Monolith = ThemePreset(
        id = "monolith",
        name = "Monolith",
        spec = AppThemeSpec(
            backgroundGradientRotationAngle = 0f,
            backgroundGradientColorIntensity = 0f,
            backgroundColors = listOf("#000000FF", "#000000FF", "#000000FF"),
            serverRowBackgroundColor = "#000000FF",
            selectedServerRowColor = "#1F1F1FFF",
            subsHeaderColor = "#000000FF",
            serverRowTitleTextColor = "#FFFFFFFF",
            serverRowSubTitleTextColor = "#7A7A7AFF",
            subscriptionInfoBackgroundColor = "#0D0D0DFF",
            subscriptionTrafficBackgroundColor = "#191919FF",
            elipseColors = listOf("#000000FF", "#000000FF", "#000000FF"),
            backgroundImageType = "none"
        )
    )

    val all: List<ThemePreset> = listOf(Midnight, Graphite, Monolith, Paper)
}
