package com.korimuspast1.lexora.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.korimuspast1.lexora.core.database.model.ProgressStatus
import com.korimuspast1.lexora.core.database.model.SrsCardEntity
import com.korimuspast1.lexora.core.database.model.StreakEntity
import com.korimuspast1.lexora.core.database.model.UserProgressEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Upsert suspend fun upsertProgress(progress: UserProgressEntity)
    @Upsert suspend fun upsertSrsCard(card: SrsCardEntity)
    @Upsert suspend fun upsertSrsCards(cards: List<SrsCardEntity>)
    @Upsert suspend fun upsertStreak(streak: StreakEntity)

    @Query("SELECT * FROM user_progress WHERE userId = :userId")
    fun observeUserProgress(userId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE userId = :userId AND lessonId = :lessonId LIMIT 1")
    fun observeLessonProgress(userId: String, lessonId: String): Flow<UserProgressEntity?>

    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND status IN ('Completed', 'Legendary')")
    fun observeCompletedLessonCount(userId: String): Flow<Int>

    @Query("SELECT * FROM srs_cards WHERE userId = :userId AND dueDate <= :now ORDER BY dueDate ASC LIMIT :limit")
    fun observeDueSrsCards(userId: String, now: Instant, limit: Int): Flow<List<SrsCardEntity>>

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    fun observeStreak(userId: String): Flow<StreakEntity?>

    @Query("DELETE FROM user_progress WHERE userId = :userId")
    suspend fun clearProgress(userId: String)
}
