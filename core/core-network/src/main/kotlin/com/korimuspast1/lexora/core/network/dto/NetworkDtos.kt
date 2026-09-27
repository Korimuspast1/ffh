package com.korimuspast1.lexora.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiEnvelope<T>(
    val data: T,
    val requestId: String? = null,
)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: JsonObject? = null,
)

@Serializable
data class RegisterRequestDto(val email: String, val password: String, val username: String, val displayName: String)
@Serializable
data class LoginRequestDto(val email: String, val password: String)
@Serializable
data class RefreshRequestDto(val refreshToken: String)
@Serializable
data class AuthTokensDto(val accessToken: String, val refreshToken: String, val expiresAtEpochMillis: Long)
@Serializable
data class AuthUserDto(val id: String, val email: String, val username: String, val displayName: String, val avatarUrl: String? = null)
@Serializable
data class AuthResponseDto(val user: AuthUserDto, val tokens: AuthTokensDto)

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val currentStreak: Int = 0,
    val totalXp: Int = 0,
    val gems: Int = 0,
    val hearts: Int = 5,
)

@Serializable
data class UpdateProfileRequestDto(val displayName: String? = null, val username: String? = null, val bio: String? = null, val avatarUrl: String? = null)

@Serializable
data class LanguageDto(val id: String, val code: String, val name: String, val nativeName: String, val isRtl: Boolean, val flagVectorKey: String)
@Serializable
data class CourseDto(val id: String, val language: LanguageDto, val code: String, val title: String, val description: String, val version: Int)
@Serializable
data class UnitDto(val id: String, val courseId: String, val orderIndex: Int, val title: String, val description: String, val colorToken: String)
@Serializable
data class LessonDto(val id: String, val unitId: String, val orderIndex: Int, val title: String, val type: String, val xpReward: Int, val estimatedMinutes: Int)
@Serializable
data class ExerciseDto(val id: String, val lessonId: String, val orderIndex: Int, val type: String, val prompt: String, val payload: JsonObject, val explanation: String, val timeLimitSeconds: Int? = null)
@Serializable
data class LessonDetailDto(val lesson: LessonDto, val exercises: List<ExerciseDto>)

@Serializable
data class CompleteLessonRequestDto(
    val score: Int,
    val accuracy: Float,
    val bestCombo: Int,
    val durationSeconds: Int,
    val mistakes: List<String>,
)

@Serializable
data class CompleteLessonResponseDto(
    val earnedXp: Int,
    val earnedGems: Int,
    val streakUpdated: Boolean,
    val unlockedAchievementIds: List<String>,
    val nextLessonId: String? = null,
)

@Serializable
data class LeaderboardEntryDto(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val leagueId: String,
    val weeklyXp: Int,
    val rank: Int,
)

@Serializable
data class QuestDto(val id: String, val kind: String, val title: String, val description: String, val target: Int, val progress: Int, val rewardXp: Int, val rewardGems: Int, val status: String)
@Serializable
data class ShopItemDto(val id: String, val type: String, val title: String, val description: String, val priceGems: Int, val payload: JsonObject)
@Serializable
data class BuyItemResponseDto(val itemId: String, val quantity: Int, val remainingGems: Int)
@Serializable
data class DictionaryWordDto(val id: String, val languageId: String, val word: String, val translation: String, val transcription: String, val status: String, val audioUrl: String? = null)
@Serializable
data class SrsReviewRequestDto(val wordId: String, @SerialName("quality") val quality0To5: Int, val reviewedAtEpochMillis: Long)
@Serializable
data class SrsReviewResponseDto(val wordId: String, val nextDueEpochMillis: Long, val easeFactor: Float, val intervalDays: Int)
@Serializable
data class AchievementDto(val id: String, val code: String, val title: String, val description: String, val iconVectorKey: String, val progress: Int, val target: Int, val unlockedAtEpochMillis: Long? = null)
@Serializable
data class FriendDto(val id: String, val username: String, val displayName: String, val avatarUrl: String? = null, val totalXp: Int, val currentStreak: Int)
