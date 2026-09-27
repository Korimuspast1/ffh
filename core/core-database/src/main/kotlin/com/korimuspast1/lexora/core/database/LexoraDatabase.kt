package com.korimuspast1.lexora.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.korimuspast1.lexora.core.database.dao.CourseDao
import com.korimuspast1.lexora.core.database.dao.GamificationDao
import com.korimuspast1.lexora.core.database.dao.ProgressDao
import com.korimuspast1.lexora.core.database.dao.UserDao
import com.korimuspast1.lexora.core.database.model.AchievementEntity
import com.korimuspast1.lexora.core.database.model.CourseEntity
import com.korimuspast1.lexora.core.database.model.ExerciseEntity
import com.korimuspast1.lexora.core.database.model.GrammarRuleEntity
import com.korimuspast1.lexora.core.database.model.LanguageEntity
import com.korimuspast1.lexora.core.database.model.LeaderboardEntity
import com.korimuspast1.lexora.core.database.model.LessonEntity
import com.korimuspast1.lexora.core.database.model.QuestEntity
import com.korimuspast1.lexora.core.database.model.ShopItemEntity
import com.korimuspast1.lexora.core.database.model.SrsCardEntity
import com.korimuspast1.lexora.core.database.model.StoryEntity
import com.korimuspast1.lexora.core.database.model.StreakEntity
import com.korimuspast1.lexora.core.database.model.UnitEntity
import com.korimuspast1.lexora.core.database.model.UserAchievementEntity
import com.korimuspast1.lexora.core.database.model.UserEntity
import com.korimuspast1.lexora.core.database.model.UserInventoryEntity
import com.korimuspast1.lexora.core.database.model.UserProgressEntity
import com.korimuspast1.lexora.core.database.model.UserQuestEntity
import com.korimuspast1.lexora.core.database.model.WordEntity

@Database(
    entities = [
        UserEntity::class,
        LanguageEntity::class,
        CourseEntity::class,
        UnitEntity::class,
        LessonEntity::class,
        ExerciseEntity::class,
        WordEntity::class,
        GrammarRuleEntity::class,
        StoryEntity::class,
        UserProgressEntity::class,
        AchievementEntity::class,
        UserAchievementEntity::class,
        QuestEntity::class,
        UserQuestEntity::class,
        ShopItemEntity::class,
        UserInventoryEntity::class,
        LeaderboardEntity::class,
        StreakEntity::class,
        SrsCardEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(LexoraTypeConverters::class)
abstract class LexoraDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun courseDao(): CourseDao
    abstract fun progressDao(): ProgressDao
    abstract fun gamificationDao(): GamificationDao

    companion object {
        const val DatabaseName = "lexora.db"

        val Migrations: Array<Migration> = emptyArray()

        val Migration1To2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN marketingOptIn INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
