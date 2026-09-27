package com.korimuspast1.lexora.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true), Index(value = ["username"], unique = true)])
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val interfaceLanguage: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(tableName = "languages", indices = [Index(value = ["code"], unique = true)])
data class LanguageEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val nativeName: String,
    val isRtl: Boolean,
    val flagVectorKey: String,
    val sortOrder: Int,
)

@Entity(tableName = "courses", indices = [Index(value = ["languageId"]), Index(value = ["code"], unique = true)])
data class CourseEntity(
    @PrimaryKey val id: String,
    val languageId: String,
    val code: String,
    val title: String,
    val description: String,
    val cefrStart: String,
    val cefrEnd: String,
    val version: Int,
    val updatedAt: Instant,
)

@Entity(tableName = "units", indices = [Index(value = ["courseId", "orderIndex"], unique = true)])
data class UnitEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val orderIndex: Int,
    val title: String,
    val description: String,
    val colorToken: String,
    val requiredXp: Int,
)

@Entity(tableName = "lessons", indices = [Index(value = ["unitId", "orderIndex"], unique = true), Index(value = ["type"])] )
data class LessonEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val orderIndex: Int,
    val title: String,
    val type: LessonKind,
    val xpReward: Int,
    val heartsCost: Int,
    val estimatedMinutes: Int,
)

@Entity(tableName = "exercises", indices = [Index(value = ["lessonId", "orderIndex"], unique = true), Index(value = ["type"])] )
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val type: ExerciseKind,
    val prompt: String,
    val payloadJson: String,
    val explanation: String,
    val timeLimitSeconds: Int?,
)

@Entity(tableName = "words", indices = [Index(value = ["languageId", "word"], unique = true), Index(value = ["translation"])] )
data class WordEntity(
    @PrimaryKey val id: String,
    val languageId: String,
    val word: String,
    val translation: String,
    val transcription: String,
    val partOfSpeech: String,
    val audioKey: String,
    val difficulty: Int,
)

@Entity(tableName = "grammar_rules", indices = [Index(value = ["languageId", "unitId"])] )
data class GrammarRuleEntity(
    @PrimaryKey val id: String,
    val languageId: String,
    val unitId: String,
    val title: String,
    val body: String,
    val examplesJson: String,
    val orderIndex: Int,
)

@Entity(tableName = "stories", indices = [Index(value = ["languageId", "unitId", "orderIndex"], unique = true)] )
data class StoryEntity(
    @PrimaryKey val id: String,
    val languageId: String,
    val unitId: String,
    val title: String,
    val body: String,
    val glossaryJson: String,
    val orderIndex: Int,
)

@Entity(tableName = "user_progress", primaryKeys = ["userId", "lessonId"], indices = [Index(value = ["status"])] )
data class UserProgressEntity(
    val userId: String,
    val lessonId: String,
    val status: ProgressStatus,
    val score: Int,
    val accuracy: Float,
    val bestCombo: Int,
    val attempts: Int,
    val lastExerciseIndex: Int,
    val completedAt: Instant?,
    val updatedAt: Instant,
)

@Entity(tableName = "achievements", indices = [Index(value = ["code"], unique = true)] )
data class AchievementEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val description: String,
    val iconVectorKey: String,
    val tier: Int,
    val target: Int,
    val rewardGems: Int,
)

@Entity(tableName = "user_achievements", primaryKeys = ["userId", "achievementId"])
data class UserAchievementEntity(
    val userId: String,
    val achievementId: String,
    val progress: Int,
    val unlockedAt: Instant?,
)

@Entity(tableName = "quests", indices = [Index(value = ["kind"])] )
data class QuestEntity(
    @PrimaryKey val id: String,
    val kind: QuestKind,
    val title: String,
    val description: String,
    val target: Int,
    val rewardXp: Int,
    val rewardGems: Int,
    val activeFrom: Instant,
    val activeUntil: Instant,
)

@Entity(tableName = "user_quests", primaryKeys = ["userId", "questId"], indices = [Index(value = ["status"])] )
data class UserQuestEntity(
    val userId: String,
    val questId: String,
    val progress: Int,
    val status: QuestStatus,
    val claimedAt: Instant?,
    val updatedAt: Instant,
)

@Entity(tableName = "shop_items", indices = [Index(value = ["type"])] )
data class ShopItemEntity(
    @PrimaryKey val id: String,
    val type: ShopItemType,
    val title: String,
    val description: String,
    val priceGems: Int,
    val payloadJson: String,
    val isActive: Boolean,
)

@Entity(tableName = "user_inventory", primaryKeys = ["userId", "itemId"])
data class UserInventoryEntity(
    val userId: String,
    val itemId: String,
    val quantity: Int,
    val expiresAt: Instant?,
    val acquiredAt: Instant,
)

@Entity(tableName = "leaderboard", primaryKeys = ["userId", "leagueId", "weekStart"], indices = [Index(value = ["leagueId", "weekStart", "weeklyXp"])] )
data class LeaderboardEntity(
    val userId: String,
    val leagueId: LeagueId,
    val weeklyXp: Int,
    val rank: Int,
    val weekStart: Instant,
    val updatedAt: Instant,
)

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey val userId: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val freezes: Int,
    val lastActiveDateEpochDay: Long,
    val updatedAt: Instant,
)

@Entity(tableName = "srs_cards", primaryKeys = ["userId", "wordId"], indices = [Index(value = ["dueDate"])] )
data class SrsCardEntity(
    val userId: String,
    val wordId: String,
    val status: WordStatus,
    val easeFactor: Float,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueDate: Instant,
    val updatedAt: Instant,
)
