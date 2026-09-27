package com.korimuspast1.lexora.data.auth

import android.content.Context
import com.korimuspast1.lexora.core.network.AccessTokenProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPreferencesAccessTokenProvider @Inject constructor(
    @ApplicationContext context: Context,
) : AccessTokenProvider {
    private val preferences = context.getSharedPreferences("lexora_session", Context.MODE_PRIVATE)

    override fun accessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun save(accessToken: String, refreshToken: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun refreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
