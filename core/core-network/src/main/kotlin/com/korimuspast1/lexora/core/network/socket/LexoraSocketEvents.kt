package com.korimuspast1.lexora.core.network.socket

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardSocketEvent(
    val leagueId: String,
    val userId: String,
    val weeklyXp: Int,
    val rank: Int,
    val emittedAtEpochMillis: Long,
)

@Serializable
data class DuelSocketEvent(
    val roomId: String,
    val event: String,
    val userId: String,
    val payloadJson: String,
    val emittedAtEpochMillis: Long,
)

interface LexoraSocketClient {
    fun leaderboard(leagueId: String): Flow<LeaderboardSocketEvent>
    fun duel(roomId: String): Flow<DuelSocketEvent>
    suspend fun disconnect()
}
