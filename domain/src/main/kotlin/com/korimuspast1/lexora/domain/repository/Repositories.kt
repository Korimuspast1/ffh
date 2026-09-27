package com.korimuspast1.lexora.domain.repository

import com.korimuspast1.lexora.core.common.result.AppResult
import com.korimuspast1.lexora.domain.model.Achievement
import com.korimuspast1.lexora.domain.model.Course
import com.korimuspast1.lexora.domain.model.Exercise
import com.korimuspast1.lexora.domain.model.Language
import com.korimuspast1.lexora.domain.model.LeaderboardEntry
import com.korimuspast1.lexora.domain.model.Lesson
import com.korimuspast1.lexora.domain.model.LessonProgress
import com.korimuspast1.lexora.domain.model.LessonSummary
import com.korimuspast1.lexora.domain.model.Quest
import com.korimuspast1.lexora.domain.model.SrsCard
import com.korimuspast1.lexora.domain.model.UnitNode
import com.korimuspast1.lexora.domain.model.User
import com.korimuspast1.lexora.domain.model.Word
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun register(email: String, password: String, username: String, displayName: String): AppResult<User>
    suspend fun login(email: String, password: String): AppResult<User>
    suspend fun logout(): AppResult<Unit>
    fun observeCurrentUser(): Flow<User?>
}

interface CourseRepository {
    fun observeLanguages(): Flow<List<Language>>
    fun observeCourses(): Flow<List<Course>>
    fun observeUnits(courseId: String): Flow<List<UnitNode>>
    fun observeLessons(unitId: String): Flow<List<Lesson>>
    fun observeExercises(lessonId: String): Flow<List<Exercise>>
    suspend fun syncCourses(): AppResult<Unit>
}

interface ProgressRepository {
    fun observeLessonProgress(userId: String, lessonId: String): Flow<LessonProgress?>
    fun observeAllProgress(userId: String): Flow<List<LessonProgress>>
    suspend fun saveProgress(progress: LessonProgress): AppResult<Unit>
    suspend fun completeLesson(userId: String, lessonId: String, summary: LessonSummary): AppResult<Unit>
}

interface PracticeRepository {
    fun observeDueCards(userId: String, limit: Int): Flow<List<SrsCard>>
    suspend fun saveReview(card: SrsCard, quality0To5: Int): AppResult<SrsCard>
}

interface DictionaryRepository {
    fun searchWords(languageId: String, query: String): Flow<List<Word>>
}

interface GamificationRepository {
    fun observeAchievements(userId: String): Flow<List<Achievement>>
    fun observeDailyQuests(userId: String): Flow<List<Quest>>
    fun observeLeaderboard(leagueId: String): Flow<List<LeaderboardEntry>>
    suspend fun claimQuest(userId: String, questId: String): AppResult<Quest>
}
