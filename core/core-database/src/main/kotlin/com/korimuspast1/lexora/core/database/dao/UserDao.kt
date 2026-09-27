package com.korimuspast1.lexora.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.korimuspast1.lexora.core.database.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}
