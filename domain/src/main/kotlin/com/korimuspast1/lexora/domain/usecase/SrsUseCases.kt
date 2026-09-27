package com.korimuspast1.lexora.domain.usecase

import com.korimuspast1.lexora.domain.model.SrsCard
import com.korimuspast1.lexora.domain.model.WordStatus
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt

class CalculateSrsReviewUseCase(private val clock: Clock = Clock.systemUTC()) {
    operator fun invoke(card: SrsCard, quality0To5: Int): SrsCard {
        val quality = quality0To5.coerceIn(0, 5)
        val now = Instant.now(clock)
        val oldEase = max(1.3f, card.easeFactor)

        return if (quality < 3) {
            card.copy(
                status = WordStatus.Forgotten,
                easeFactor = max(1.3f, oldEase - 0.2f),
                intervalDays = 1,
                repetitions = 0,
                lapses = card.lapses + 1,
                dueDate = now.plus(1, ChronoUnit.DAYS),
            )
        } else {
            val newEase = max(
                1.3f,
                oldEase + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f)),
            )
            val nextRepetitions = card.repetitions + 1
            val interval = when (nextRepetitions) {
                1 -> 1
                2 -> 6
                else -> max(1, (card.intervalDays * newEase).roundToInt())
            }
            card.copy(
                status = if (quality >= 4) WordStatus.Known else WordStatus.Learning,
                easeFactor = newEase,
                intervalDays = interval,
                repetitions = nextRepetitions,
                dueDate = now.plus(interval.toLong(), ChronoUnit.DAYS),
            )
        }
    }
}
