package com.korimuspast1.lexora.data.di

import com.korimuspast1.lexora.core.network.AccessTokenProvider
import com.korimuspast1.lexora.core.network.LexoraNetworkDefaults
import com.korimuspast1.lexora.core.network.api.AchievementApi
import com.korimuspast1.lexora.core.network.api.AuthApi
import com.korimuspast1.lexora.core.network.api.CourseApi
import com.korimuspast1.lexora.core.network.api.DictionaryApi
import com.korimuspast1.lexora.core.network.api.FriendsApi
import com.korimuspast1.lexora.core.network.api.LeaderboardApi
import com.korimuspast1.lexora.core.network.api.LessonApi
import com.korimuspast1.lexora.core.network.api.QuestApi
import com.korimuspast1.lexora.core.network.api.ShopApi
import com.korimuspast1.lexora.core.network.api.SrsApi
import com.korimuspast1.lexora.data.auth.SharedPreferencesAccessTokenProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenModule {
    @Binds
    @Singleton
    abstract fun bindAccessTokenProvider(provider: SharedPreferencesAccessTokenProvider): AccessTokenProvider
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenProvider: AccessTokenProvider,
        @Named("debugNetwork") debug: Boolean,
    ): OkHttpClient = LexoraNetworkDefaults.okHttpClient(tokenProvider, debug)

    @Provides
    @Singleton
    fun provideRetrofit(
        @Named("apiBaseUrl") baseUrl: String,
        client: OkHttpClient,
    ): Retrofit = LexoraNetworkDefaults.retrofit(baseUrl, client)

    @Provides fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    @Provides fun provideUserApi(retrofit: Retrofit): com.korimuspast1.lexora.core.network.api.UserApi = retrofit.create(com.korimuspast1.lexora.core.network.api.UserApi::class.java)
    @Provides fun provideCourseApi(retrofit: Retrofit): CourseApi = retrofit.create(CourseApi::class.java)
    @Provides fun provideLessonApi(retrofit: Retrofit): LessonApi = retrofit.create(LessonApi::class.java)
    @Provides fun provideLeaderboardApi(retrofit: Retrofit): LeaderboardApi = retrofit.create(LeaderboardApi::class.java)
    @Provides fun provideQuestApi(retrofit: Retrofit): QuestApi = retrofit.create(QuestApi::class.java)
    @Provides fun provideShopApi(retrofit: Retrofit): ShopApi = retrofit.create(ShopApi::class.java)
    @Provides fun provideDictionaryApi(retrofit: Retrofit): DictionaryApi = retrofit.create(DictionaryApi::class.java)
    @Provides fun provideSrsApi(retrofit: Retrofit): SrsApi = retrofit.create(SrsApi::class.java)
    @Provides fun provideAchievementApi(retrofit: Retrofit): AchievementApi = retrofit.create(AchievementApi::class.java)
    @Provides fun provideFriendsApi(retrofit: Retrofit): FriendsApi = retrofit.create(FriendsApi::class.java)
}
