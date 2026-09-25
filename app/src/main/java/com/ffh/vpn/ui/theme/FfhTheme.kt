package com.ffh.vpn.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.ffh.vpn.R

/** Inter — a neutral grotesque that keeps the minimal look crisp at any size. */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val FfhShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

fun ffhTypography(fontScale: Float = 1f): Typography {
    fun style(size: Int, weight: FontWeight, line: Int, spacing: Double = 0.0) = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = weight,
        fontSize = (size * fontScale).sp,
        lineHeight = (line * fontScale).sp,
        letterSpacing = spacing.sp
    )
    return Typography(
        displayLarge = style(34, FontWeight.Bold, 42, -0.4),
        displayMedium = style(28, FontWeight.Bold, 36, -0.3),
        headlineLarge = style(24, FontWeight.SemiBold, 32, -0.2),
        headlineMedium = style(20, FontWeight.SemiBold, 28, -0.2),
        titleLarge = style(18, FontWeight.SemiBold, 24),
        titleMedium = style(16, FontWeight.Medium, 22),
        titleSmall = style(14, FontWeight.Medium, 20),
        bodyLarge = style(16, FontWeight.Normal, 24),
        bodyMedium = style(14, FontWeight.Normal, 20),
        bodySmall = style(12, FontWeight.Normal, 18),
        labelLarge = style(14, FontWeight.Medium, 20),
        labelMedium = style(12, FontWeight.Medium, 16),
        labelSmall = style(11, FontWeight.Medium, 14, 0.2)
    )
}

/** The whole visual identity of the app in one place. */
val LocalFfhPalette = staticCompositionLocalOf { FfhPalette.DEFAULT }

@Composable
fun FfhTheme(
    spec: AppThemeSpec = AppThemeSpec.DEFAULT,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val palette = remember(spec) { spec.normalized().toPalette() }
    val colorScheme = remember(palette) { palette.toColorScheme() }
    val typography = remember(fontScale) { ffhTypography(fontScale) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val useDarkIcons = palette.isLightBackground
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkIcons
        }
    }

    CompositionLocalProvider(LocalFfhPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = FfhShapes,
            content = content
        )
    }
}

/** Maps the custom theme onto Material 3 so stock components stay usable. */
fun FfhPalette.toColorScheme(
    followSystemDark: Boolean = false,
    systemDark: Boolean = true
): androidx.compose.material3.ColorScheme {
    val base = if (isLightBackground) lightColorScheme() else darkColorScheme()
    val outline = serverRowSubTitleTextColor.copy(alpha = 0.35f)
    return base.copy(
        primary = buttonColor,
        onPrimary = buttonTextColor,
        primaryContainer = selectedServerRowColor,
        onPrimaryContainer = serverRowTitleTextColor,
        secondary = serverRowTitleTextColor,
        onSecondary = if (serverRowBackgroundColor.isBright()) Color.Black else Color.White,
        secondaryContainer = subscriptionInfoBackgroundColor,
        onSecondaryContainer = serverRowTitleTextColor,
        tertiary = settingsControlsTintColor,
        onTertiary = if (settingsControlsTintColor.isBright()) Color.Black else Color.White,
        background = baseBackground,
        onBackground = serverRowTitleTextColor,
        surface = serverRowBackgroundColor,
        onSurface = serverRowTitleTextColor,
        surfaceVariant = subscriptionInfoBackgroundColor,
        onSurfaceVariant = serverRowSubTitleTextColor,
        surfaceTint = Color.Transparent,
        outline = outline,
        outlineVariant = serverRowSubTitleTextColor.copy(alpha = 0.18f),
        scrim = Color.Black.copy(alpha = 0.6f),
        error = Color(0xFFFF5A5F),
        onError = Color.White,
        errorContainer = Color(0xFF2A0A0C),
        onErrorContainer = Color(0xFFFFB4B4)
    )
}

@Composable
fun systemDarkThemeActive(): Boolean = isSystemInDarkTheme()
