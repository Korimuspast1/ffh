package com.korimuspast1.lexora.core.database.model

enum class LessonKind { Vocabulary, Grammar, Listening, Speaking, Story, Review, Legendary }
enum class ExerciseKind { WordChoice, SentenceBuilder, ListeningInput, GapChoice, MatchingPairs, Pronunciation, TextInsert, Dictation }
enum class ProgressStatus { Locked, Available, InProgress, Completed, Legendary }
enum class WordStatus { New, Learning, Known, Forgotten }
enum class QuestKind { EarnXp, CompleteLessons, Accuracy, PracticeWords, KeepStreak }
enum class QuestStatus { Active, Completed, Claimed, Expired }
enum class ShopItemType { Hearts, XpBoost, StreakFreeze, Theme, MascotCostume, Avatar }
enum class LeagueId { Bronze, Silver, Gold, Sapphire, Ruby, Emerald, Amethyst, Pearl, Obsidian, Diamond }
