package com.ffh.vpn.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Paints the animated-free, static background of the app:
 * a linear gradient rotated by `backgroundGradientRotationAngle` plus three
 * soft "ellipses" drawn as radial gradients.
 */
fun Modifier.ffhBackground(palette: FfhPalette): Modifier = this.drawBehind {
    if (palette.backgroundImageType == "none") {
        drawRect(color = palette.gradientColors.firstOrNull() ?: Color.Black)
        return@drawBehind
    }

    val colors = palette.gradientColors
    if (colors.isEmpty()) return@drawBehind

    val angleRad = Math.toRadians(palette.backgroundGradientRotationAngle.toDouble())
    val dx = cos(angleRad).toFloat()
    val dy = sin(angleRad).toFloat()
    val length = abs(size.width * dx) + abs(size.height * dy)
    val cx = size.width / 2f
    val cy = size.height / 2f

    if (length < 1f || colors.size == 1) {
        drawRect(color = colors.first())
    } else {
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(cx - dx * length / 2f, cy - dy * length / 2f),
                end = Offset(cx + dx * length / 2f, cy + dy * length / 2f)
            )
        )
    }

    if (palette.backgroundImageType == "custom") {
        // A user supplied wallpaper is drawn by the caller; nothing to do here.
        return@drawBehind
    }

    drawEllipses(palette)
}

private fun DrawScope.drawEllipses(palette: FfhPalette) {
    if (palette.elipseColors.isEmpty()) return
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val spots = listOf(
        Triple(Offset(w * 0.12f, h * 0.10f), w * 0.85f, 0),
        Triple(Offset(w * 0.95f, h * 0.22f), w * 0.75f, 1),
        Triple(Offset(w * 0.45f, h * 1.02f), w * 1.05f, 2)
    )

    for ((center, radius, index) in spots) {
        val color = palette.elipseColors.getOrNull(index)
            ?: palette.elipseColors.last()
        if (color.alpha <= 0.002f) continue
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color,
                    color.copy(alpha = color.alpha * 0.55f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
        )
    }
}

@Composable
fun FfhBackground(
    modifier: Modifier = Modifier,
    palette: FfhPalette = LocalFfhPalette.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.ffhBackground(palette),
        contentAlignment = Alignment.TopStart,
        content = content
    )
}
