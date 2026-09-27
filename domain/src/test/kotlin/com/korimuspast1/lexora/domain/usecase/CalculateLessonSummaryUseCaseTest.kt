package com.korimuspast1.lexora.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateLessonSummaryUseCaseTest {
    @Test
    fun perfectLessonGetsBonus() {
        val summary = CalculateLessonSummaryUseCase()(correctAnswers = 10, totalAnswers = 10, baseXp = 15, bestCombo = 9, durationSeconds = 180)
        assertEquals(23, summary.earnedXp)
        assertTrue(summary.perfect)
        assertEquals(1f, summary.accuracy, 0.0001f)
    }

    @Test
    fun partialLessonStillReturnsAtLeastOneXp() {
        val summary = CalculateLessonSummaryUseCase()(correctAnswers = 0, totalAnswers = 8, baseXp = 15, bestCombo = 0, durationSeconds = 120)
        assertEquals(1, summary.earnedXp)
        assertFalse(summary.perfect)
    }
}
