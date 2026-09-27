package com.korimuspast1.lexora.domain.usecase

import com.korimuspast1.lexora.domain.model.SrsCard
import com.korimuspast1.lexora.domain.model.WordStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateSrsReviewUseCaseTest {
    private val now = Instant.parse("2026-09-27T00:00:00Z")
    private val useCase = CalculateSrsReviewUseCase(Clock.fixed(now, ZoneOffset.UTC))

    @Test
    fun successfulFirstReviewSchedulesOneDay() {
        val result = useCase(card(), quality0To5 = 5)
        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(now.plusSeconds(86_400), result.dueDate)
        assertEquals(WordStatus.Known, result.status)
    }

    @Test
    fun failedReviewResetsRepetitionsAndCountsLapse() {
        val result = useCase(card(repetitions = 4, intervalDays = 12), quality0To5 = 2)
        assertEquals(0, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(1, result.lapses)
        assertEquals(WordStatus.Forgotten, result.status)
        assertTrue(result.easeFactor >= 1.3f)
    }

    private fun card(repetitions: Int = 0, intervalDays: Int = 0): SrsCard = SrsCard(
        userId = "user",
        wordId = "word",
        status = WordStatus.Learning,
        easeFactor = 2.5f,
        intervalDays = intervalDays,
        repetitions = repetitions,
        lapses = 0,
        dueDate = now,
    )
}
