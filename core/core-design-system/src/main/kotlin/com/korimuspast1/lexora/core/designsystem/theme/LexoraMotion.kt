package com.korimuspast1.lexora.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object LexoraMotion {
    const val DurationShort = 140
    const val DurationMedium = 260
    const val DurationLong = 420
    const val DurationExtraLong = 700

    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1.18f)
    val ExitEasing: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}
