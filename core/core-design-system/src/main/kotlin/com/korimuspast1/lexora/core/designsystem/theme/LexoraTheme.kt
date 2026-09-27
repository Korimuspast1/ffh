package com.korimuspast1.lexora.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = LexoraPalette.Primary50,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LexoraPalette.Primary90,
    onPrimaryContainer = LexoraPalette.Primary20,
    secondary = LexoraPalette.Secondary50,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = LexoraPalette.Secondary90,
    onSecondaryContainer = LexoraPalette.Secondary20,
    tertiary = LexoraPalette.Tertiary50,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    tertiaryContainer = LexoraPalette.Tertiary90,
    onTertiaryContainer = LexoraPalette.Tertiary20,
    error = LexoraPalette.Error50,
    onError = androidx.compose.ui.graphics.Color.White,
    errorContainer = LexoraPalette.Error90,
    onErrorContainer = LexoraPalette.Error20,
    background = LexoraPalette.Neutral95,
    onBackground = LexoraPalette.Neutral10,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = LexoraPalette.Neutral10,
    surfaceVariant = LexoraPalette.Neutral90,
    onSurfaceVariant = LexoraPalette.Neutral40,
    outline = LexoraPalette.Neutral70,
)

private val DarkColorScheme = darkColorScheme(
    primary = LexoraPalette.Primary70,
    onPrimary = LexoraPalette.Primary10,
    primaryContainer = LexoraPalette.Primary20,
    onPrimaryContainer = LexoraPalette.Primary90,
    secondary = LexoraPalette.Secondary70,
    onSecondary = LexoraPalette.Secondary10,
    secondaryContainer = LexoraPalette.Secondary20,
    onSecondaryContainer = LexoraPalette.Secondary90,
    tertiary = LexoraPalette.Tertiary70,
    onTertiary = LexoraPalette.Tertiary10,
    tertiaryContainer = LexoraPalette.Tertiary20,
    onTertiaryContainer = LexoraPalette.Tertiary90,
    error = LexoraPalette.Error70,
    onError = LexoraPalette.Error10,
    errorContainer = LexoraPalette.Error20,
    onErrorContainer = LexoraPalette.Error90,
    background = LexoraPalette.Neutral10,
    onBackground = LexoraPalette.Neutral95,
    surface = LexoraPalette.Neutral20,
    onSurface = LexoraPalette.Neutral95,
    surfaceVariant = LexoraPalette.Neutral30,
    onSurfaceVariant = LexoraPalette.Neutral80,
    outline = LexoraPalette.Neutral60,
)

val LexoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun LexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkLexoraExtendedColors else LightLexoraExtendedColors

    CompositionLocalProvider(
        LocalLexoraExtendedColors provides extendedColors,
        LocalLexoraSpacing provides LexoraSpacing(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LexoraTypography,
            shapes = LexoraShapes,
            content = content,
        )
    }
}

object LexoraThemeValues {
    val colors: LexoraExtendedColors
        @Composable get() = LocalLexoraExtendedColors.current

    val spacing: LexoraSpacing
        @Composable get() = LocalLexoraSpacing.current
}
