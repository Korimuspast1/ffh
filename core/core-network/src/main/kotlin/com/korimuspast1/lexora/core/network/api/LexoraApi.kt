package com.korimuspast1.lexora.core.network.api

import com.korimuspast1.lexora.core.network.dto.AchievementDto
import com.korimuspast1.lexora.core.network.dto.ApiEnvelope
import com.korimuspast1.lexora.core.network.dto.AuthResponseDto
import com.korimuspast1.lexora.core.network.dto.BuyItemResponseDto
import com.korimuspast1.lexora.core.network.dto.CompleteLessonRequestDto
import com.korimuspast1.lexora.core.network.dto.CompleteLessonResponseDto
import com.korimuspast1.lexora.core.network.dto.CourseDto
import com.korimuspast1.lexora.core.network.dto.DictionaryWordDto
import com.korimuspast1.lexora.core.network.dto.FriendDto
import com.korimuspast1.lexora.core.network.dto.LeaderboardEntryDto
import com.korimuspast1.lexora.core.network.dto.LessonDetailDto
import com.korimuspast1.lexora.core.network.dto.LoginRequestDto
import com.korimuspast1.lexora.core.network.dto.QuestDto
import com.korimuspast1.lexora.core.network.dto.RefreshRequestDto
import com.korimuspast1.lexora.core.network.dto.RegisterRequestDto
import com.korimuspast1.lexora.core.network.dto.ShopItemDto
import com.korimuspast1.lexora.core.network.dto.SrsReviewRequestDto
import com.korimuspast1.lexora.core.network.dto.SrsReviewResponseDto
import com.korimuspast1.lexora.core.network.dto.UnitDto
import com.korimuspast1.lexora.core.network.dto.UpdateProfileRequestDto
import com.korimuspast1.lexora.core.network.dto.UserProfileDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/register") suspend fun register(@Body request: RegisterRequestDto): ApiEnvelope<AuthResponseDto>
    @POST("auth/login") suspend fun login(@Body request: LoginRequestDto): ApiEnvelope<AuthResponseDto>
    @POST("auth/refresh") suspend fun refresh(@Body request: RefreshRequestDto): ApiEnvelope<AuthResponseDto>
    @POST("auth/logout") suspend fun logout(): ApiEnvelope<Unit>
}

interface UserApi {
    @GET("users/me") suspend fun me(): ApiEnvelope<UserProfileDto>
    @PATCH("users/me") suspend fun updateMe(@Body request: UpdateProfileRequestDto): ApiEnvelope<UserProfileDto>
}

interface CourseApi {
    @GET("courses") suspend fun courses(): ApiEnvelope<List<CourseDto>>
    @GET("courses/{id}/units") suspend fun units(@Path("id") courseId: String): ApiEnvelope<List<UnitDto>>
}

interface LessonApi {
    @GET("lessons/{id}") suspend fun lesson(@Path("id") lessonId: String): ApiEnvelope<LessonDetailDto>
    @POST("lessons/{id}/complete") suspend fun complete(@Path("id") lessonId: String, @Body request: CompleteLessonRequestDto): ApiEnvelope<CompleteLessonResponseDto>
}

interface LeaderboardApi {
    @GET("leaderboard/{leagueId}") suspend fun leaderboard(@Path("leagueId") leagueId: String, @Query("limit") limit: Int = 30): ApiEnvelope<List<LeaderboardEntryDto>>
}

interface QuestApi {
    @GET("quests/daily") suspend fun daily(): ApiEnvelope<List<QuestDto>>
    @POST("quests/{id}/claim") suspend fun claim(@Path("id") questId: String): ApiEnvelope<QuestDto>
}

interface ShopApi {
    @GET("shop/items") suspend fun items(): ApiEnvelope<List<ShopItemDto>>
    @POST("shop/buy/{itemId}") suspend fun buy(@Path("itemId") itemId: String): ApiEnvelope<BuyItemResponseDto>
}

interface DictionaryApi {
    @GET("dictionary") suspend fun dictionary(@Query("languageId") languageId: String, @Query("query") query: String? = null): ApiEnvelope<List<DictionaryWordDto>>
}

interface SrsApi {
    @POST("srs/review") suspend fun review(@Body request: SrsReviewRequestDto): ApiEnvelope<SrsReviewResponseDto>
}

interface AchievementApi {
    @GET("achievements") suspend fun achievements(): ApiEnvelope<List<AchievementDto>>
}

interface FriendsApi {
    @GET("friends") suspend fun friends(): ApiEnvelope<List<FriendDto>>
    @POST("friends/add/{userId}") suspend fun add(@Path("userId") userId: String): ApiEnvelope<FriendDto>
}
