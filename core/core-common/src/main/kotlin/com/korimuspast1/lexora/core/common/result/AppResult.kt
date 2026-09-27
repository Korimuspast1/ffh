package com.korimuspast1.lexora.core.common.result

sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    val message: String

    data class Network(override val message: String, val code: Int? = null) : AppError
    data class Validation(override val message: String, val field: String? = null) : AppError
    data class Unauthorized(override val message: String = "Unauthorized") : AppError
    data class NotFound(override val message: String = "Not found") : AppError
    data class Storage(override val message: String) : AppError
    data class Unknown(override val message: String, val cause: Throwable? = null) : AppError
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(value))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(value)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
