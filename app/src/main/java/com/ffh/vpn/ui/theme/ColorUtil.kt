package com.ffh.vpn.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure Kotlin colour helpers.
 *
 * Everything here is framework free on purpose: the same code is used by the
 * Compose UI and by the JVM unit tests, so no `android.*` stubs are involved.
 */

internal const val HEX_DIGITS = "0123456789ABCDEF"

/**
 * Parses colours in the shapes used by theme files:
 * `#RRGGBBAA` (the format used by FFH themes), `#RRGGBB`, `#ARGB`, `#RGB`,
 * `#RRGGBBAA` with or without the leading `#`.
 *
 * Returns [fallback] when the value cannot be understood, so a broken theme
 * can never crash the UI.
 */
fun parseHexColor(input: String?, fallback: Color = Color.Black): Color {
    if (input.isNullOrBlank()) return fallback
    var s = input.trim().removePrefix("#").removePrefix("0x").removePrefix("0X").uppercase()
    if (s.any { it !in HEX_DIGITS }) return fallback

    s = when (s.length) {
        3 -> buildString { s.forEach { append(it); append(it) } }.plus("FF")
        4 -> buildString { s.forEach { append(it); append(it) } }
        6 -> s + "FF"
        8 -> s
        else -> return fallback
    }

    return runCatching {
        val r = s.substring(0, 2).toInt(16)
        val g = s.substring(2, 4).toInt(16)
        val b = s.substring(4, 6).toInt(16)
        val a = s.substring(6, 8).toInt(16)
        Color(red = r, green = g, blue = b, alpha = a)
    }.getOrDefault(fallback)
}

/** Serialises back to `#RRGGBBAA`, the exact format used by theme files. */
fun Color.toHexString(): String {
    fun part(value: Float) = (value * 255f + 0.5f).toInt().coerceIn(0, 255)
    val r = part(red)
    val g = part(green)
    val b = part(blue)
    val a = part(alpha)
    return "#" + listOf(r, g, b, a).joinToString("") { it.toString(16).uppercase().padStart(2, '0') }
}

/** Perceived brightness, 0 (black) .. 1 (white). */
fun luminance(color: Color): Float =
    (0.2126f * channel(color.red) + 0.7152f * channel(color.green) + 0.0722f * channel(color.blue))

private fun channel(value: Float): Float {
    val c = value.coerceIn(0f, 1f)
    return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
}

/** True when the surface is bright enough to need dark content on top of it. */
fun Color.isBright(): Boolean = luminance(this) >= 0.5f

/** Black or white, whichever is readable on top of [this]. */
fun Color.readableContent(): Color = if (isBright()) Color.Black else Color.White

/** HSV representation, channels kept in the usual 0..360 / 0..1 / 0..1 ranges. */
data class Hsv(val hue: Float, val saturation: Float, val value: Float)

fun Color.toHsv(): Hsv {
    val r = red
    val g = green
    val b = blue
    val max = max(max(r, g), b)
    val min = min(min(r, g), b)
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    return Hsv(
        hue = if (hue < 0f) hue + 360f else hue,
        saturation = if (max == 0f) 0f else delta / max,
        value = max
    )
}

fun hsvToColor(hsv: Hsv, alpha: Float = 1f): Color {
    val c = hsv.value * hsv.saturation
    val x = c * (1f - abs(((hsv.hue / 60f) % 2f) - 1f))
    val m = hsv.value - c
    val sector = ((hsv.hue % 360f) / 60f).toInt() % 6
    val (r, g, b) = when (sector) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r + m).coerceIn(0f, 1f),
        green = (g + m).coerceIn(0f, 1f),
        blue = (b + m).coerceIn(0f, 1f),
        alpha = alpha.coerceIn(0f, 1f)
    )
}

/**
 * Applies the `backgroundGradientColorIntensity` knob: 0 keeps the colours as
 * they are, larger values push saturation and brightness of the background
 * gradient, which is exactly how the reference client behaves.
 */
fun Color.withIntensity(intensity: Float): Color {
    if (intensity <= 0f) return this
    val hsv = toHsv()
    val boost = 1f + intensity.coerceIn(0f, 1f)
    return hsvToColor(
        Hsv(
            hue = hsv.hue,
            saturation = (hsv.saturation * boost).coerceIn(0f, 1f),
            value = (hsv.value * (1f + intensity.coerceIn(0f, 1f) * 0.25f)).coerceIn(0f, 1f)
        ),
        alpha = alpha
    )
}

fun Color.withAlpha(alpha: Float): Color = copy(alpha = alpha.coerceIn(0f, 1f))

fun blend(from: Color, to: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
        red = from.red + (to.red - from.red) * t,
        green = from.green + (to.green - from.green) * t,
        blue = from.blue + (to.blue - from.blue) * t,
        alpha = from.alpha + (to.alpha - from.alpha) * t
    )
}

/** A slightly lighter / darker copy — handy for pressed states and dividers. */
fun Color.elevate(amount: Float): Color =
    blend(this, if (isBright()) Color.White else Color.Black, -amount.coerceIn(-1f, 1f)).let {
        if (amount >= 0f) it else it
    }
