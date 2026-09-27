package com.korimuspast1.lexora.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val bio: String?,
    val interfaceLanguage: String,
)

data class Language(
    val id: String,
    val code: String,
    val name: String,
    val nativeName: String,
    val isRtl: Boolean,
    val flagVectorKey: String,
)

data class Course(val id: String, val language: Language, val code: String, val title: String, val description: String, val version: Int)
data class UnitNode(val id: String, val courseId: String, val orderIndex: Int, val title: String, val description: String)
data class Lesson(val id: String, val unitId: String, val orderIndex: Int, val title: String, val kind: LessonKind, val xpReward: Int, val estimatedMinutes: Int)

enum class LessonKind { Vocabulary, Grammar, Listening, Speaking, Story, Review, Legendary }
enum class ExerciseKind { WordChoice, SentenceBuilder, ListeningInput, GapChoice, MatchingPairs, Pronunciation, TextInsert, Dictation }
enum class ProgressStatus { Locked, Available, InProgress, Completed, Legendary }
enum class WordStatus { New, Learning, Known, Forgotten }
enum class QuestStatus { Active, Completed, Claimed, Expired }
enum class LeagueId { Bronze, Silver, Gold, Sapphire, Ruby, Emerald, Amethyst, Pearl, Obsidian, Diamond }

data class Exercise(
    val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val kind: ExerciseKind,
    val prompt: String,
    val payloadJson: String,
    val explanation: String,
    val timeLimitSeconds: Int?,
)

data class LessonProgress(
    val userId: String,
    val lessonId: String,
    val status: ProgressStatus,
    val score: Int,
    val accuracy: Float,
    val bestCombo: Int,
    val attempts: Int,
    val lastExerciseIndex: Int,
    val completedAt: Instant?,
)

data class Word(
    val id: String,
    val languageId: String,
    val word: String,
    val translation: String,
    val transcription: String,
    val status: WordStatus,
)

data class Achievement(
    val id: String,
    val code: String,
    val title: String,
    val description: String,
    val iconVectorKey: String,
    val progress: Int,
    val target: Int,
    val unlockedAt: Instant?,
)

data class Quest(
    val id: String,
    val title: String,
    val description: String,
    val target: Int,
    val progress: Int,
    val rewardXp: Int,
    val rewardGems: Int,
    val status: QuestStatus,
)

data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val leagueId: LeagueId,
    val weeklyXp: Int,
    val rank: Int,
)

data class SrsCard(
    val userId: String,
    val wordId: String,
    val status: WordStatus,
    val easeFactor: Float,
    val intervalDays: Int,
    val repetitions: Int,
    val lapses: Int,
    val dueDate: Instant,
)

data class LessonSummary(
    val earnedXp: Int,
    val accuracy: Float,
    val durationSeconds: Int,
    val bestCombo: Int,
    val perfect: Boolean,
)
