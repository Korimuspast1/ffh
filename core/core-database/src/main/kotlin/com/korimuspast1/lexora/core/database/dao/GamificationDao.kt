package com.korimuspast1.lexora.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.korimuspast1.lexora.core.database.model.AchievementEntity
import com.korimuspast1.lexora.core.database.model.LeaderboardEntity
import com.korimuspast1.lexora.core.database.model.LeagueId
import com.korimuspast1.lexora.core.database.model.QuestEntity
import com.korimuspast1.lexora.core.database.model.ShopItemEntity
import com.korimuspast1.lexora.core.database.model.UserAchievementEntity
import com.korimuspast1.lexora.core.database.model.UserInventoryEntity
import com.korimuspast1.lexora.core.database.model.UserQuestEntity
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface GamificationDao {
    @Upsert suspend fun upsertAchievements(items: List<AchievementEntity>)
    @Upsert suspend fun upsertUserAchievement(item: UserAchievementEntity)
    @Upsert suspend fun upsertQuests(items: List<QuestEntity>)
    @Upsert suspend fun upsertUserQuest(item: UserQuestEntity)
    @Upsert suspend fun upsertShopItems(items: List<ShopItemEntity>)
    @Upsert suspend fun upsertInventoryItem(item: UserInventoryEntity)
    @Upsert suspend fun upsertLeaderboard(items: List<LeaderboardEntity>)

    @Query("SELECT * FROM achievements ORDER BY tier ASC, title ASC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM user_achievements WHERE userId = :userId")
    fun observeUserAchievements(userId: String): Flow<List<UserAchievementEntity>>

    @Query("SELECT * FROM quests WHERE activeFrom <= :now AND activeUntil > :now ORDER BY activeUntil ASC")
    fun observeActiveQuests(now: Instant): Flow<List<QuestEntity>>

    @Query("SELECT * FROM user_quests WHERE userId = :userId")
    fun observeUserQuests(userId: String): Flow<List<UserQuestEntity>>

    @Query("SELECT * FROM shop_items WHERE isActive = 1 ORDER BY priceGems ASC")
    fun observeShopItems(): Flow<List<ShopItemEntity>>

    @Query("SELECT * FROM user_inventory WHERE userId = :userId")
    fun observeInventory(userId: String): Flow<List<UserInventoryEntity>>

    @Query("SELECT * FROM leaderboard WHERE leagueId = :leagueId AND weekStart = :weekStart ORDER BY weeklyXp DESC LIMIT :limit")
    fun observeLeaderboard(leagueId: LeagueId, weekStart: Instant, limit: Int): Flow<List<LeaderboardEntity>>
}
