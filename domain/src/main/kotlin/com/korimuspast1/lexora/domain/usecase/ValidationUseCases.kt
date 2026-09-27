package com.korimuspast1.lexora.domain.usecase

import com.korimuspast1.lexora.core.common.result.AppError
import com.korimuspast1.lexora.core.common.result.AppResult

class ValidateEmailUseCase {
    operator fun invoke(email: String): AppResult<String> {
        val normalized = email.trim().lowercase()
        val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        return when {
            normalized.isBlank() -> AppResult.Failure(AppError.Validation("Email is required", "email"))
            !emailRegex.matches(normalized) -> AppResult.Failure(AppError.Validation("Email format is invalid", "email"))
            else -> AppResult.Success(normalized)
        }
    }
}

class ValidatePasswordUseCase {
    operator fun invoke(password: String): AppResult<String> = when {
        password.length < 8 -> AppResult.Failure(AppError.Validation("Password must contain at least 8 characters", "password"))
        password.none(Char::isDigit) -> AppResult.Failure(AppError.Validation("Password must contain a digit", "password"))
        password.none(Char::isUpperCase) -> AppResult.Failure(AppError.Validation("Password must contain an uppercase letter", "password"))
        password.none(Char::isLowerCase) -> AppResult.Failure(AppError.Validation("Password must contain a lowercase letter", "password"))
        else -> AppResult.Success(password)
    }
}
