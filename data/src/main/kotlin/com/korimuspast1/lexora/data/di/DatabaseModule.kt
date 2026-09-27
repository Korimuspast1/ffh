package com.korimuspast1.lexora.data.di

import android.content.Context
import androidx.room.Room
import com.korimuspast1.lexora.core.database.LexoraDatabase
import com.korimuspast1.lexora.core.database.dao.CourseDao
import com.korimuspast1.lexora.core.database.dao.GamificationDao
import com.korimuspast1.lexora.core.database.dao.ProgressDao
import com.korimuspast1.lexora.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LexoraDatabase =
        Room.databaseBuilder(context, LexoraDatabase::class.java, LexoraDatabase.DatabaseName)
            .addMigrations(*LexoraDatabase.Migrations)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideUserDao(database: LexoraDatabase): UserDao = database.userDao()
    @Provides fun provideCourseDao(database: LexoraDatabase): CourseDao = database.courseDao()
    @Provides fun provideProgressDao(database: LexoraDatabase): ProgressDao = database.progressDao()
    @Provides fun provideGamificationDao(database: LexoraDatabase): GamificationDao = database.gamificationDao()
}
