package com.korimuspast1.lexora.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

interface AccessTokenProvider {
    fun accessToken(): String?
}

class AuthHeaderInterceptor(private val tokenProvider: AccessTokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val original = chain.request()
        val token = tokenProvider.accessToken()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

object LexoraNetworkDefaults {
    val JsonFormat: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = false
    }

    fun okHttpClient(
        tokenProvider: AccessTokenProvider,
        debug: Boolean,
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (debug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthHeaderInterceptor(tokenProvider))
            .addInterceptor(logging)
            .build()
    }

    fun retrofit(
        baseUrl: String,
        client: OkHttpClient,
        json: Json = JsonFormat,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
