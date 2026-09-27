package com.korimuspast1.lexora.domain.usecase

import com.korimuspast1.lexora.domain.model.LessonSummary
import kotlin.math.roundToInt

class CalculateLessonSummaryUseCase {
    operator fun invoke(
        correctAnswers: Int,
        totalAnswers: Int,
        baseXp: Int,
        bestCombo: Int,
        durationSeconds: Int,
    ): LessonSummary {
        val safeTotal = totalAnswers.coerceAtLeast(1)
        val accuracy = correctAnswers.coerceIn(0, safeTotal).toFloat() / safeTotal
        val comboBonus = (bestCombo / 3).coerceAtMost(5)
        val perfectBonus = if (accuracy == 1f) 5 else 0
        val earnedXp = (baseXp * accuracy).roundToInt() + comboBonus + perfectBonus
        return LessonSummary(
            earnedXp = earnedXp.coerceAtLeast(1),
            accuracy = accuracy,
            durationSeconds = durationSeconds.coerceAtLeast(0),
            bestCombo = bestCombo.coerceAtLeast(0),
            perfect = accuracy == 1f,
        )
    }
}
