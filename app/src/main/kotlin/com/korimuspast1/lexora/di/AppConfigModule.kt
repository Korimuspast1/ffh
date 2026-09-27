package com.korimuspast1.lexora.di

import com.korimuspast1.lexora.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppConfigModule {
    @Provides
    @Named("apiBaseUrl")
    fun provideApiBaseUrl(): String = BuildConfig.API_BASE_URL

    @Provides
    @Named("wsBaseUrl")
    fun provideWsBaseUrl(): String = BuildConfig.WS_BASE_URL

    @Provides
    @Named("debugNetwork")
    fun provideDebugNetwork(): Boolean = BuildConfig.DEBUG
}
