package com.korimuspast1.lexora.domain.usecase

import com.korimuspast1.lexora.core.common.result.AppResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUseCasesTest {
    @Test
    fun validEmailIsNormalized() {
        val result = ValidateEmailUseCase()("  USER@Example.COM ")
        assertTrue(result is AppResult.Success)
        assertEquals("user@example.com", (result as AppResult.Success).value)
    }

    @Test
    fun invalidEmailFails() {
        val result = ValidateEmailUseCase()("broken")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun strongPasswordPasses() {
        val result = ValidatePasswordUseCase()("Lexora2026")
        assertTrue(result is AppResult.Success)
    }
}
