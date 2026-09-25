package com.ffh.vpn.ui

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Applies the language chosen in the settings without appcompat. */
object AppLocale {

    fun localeFor(language: String): Locale = when (language.lowercase()) {
        "ru" -> Locale("ru")
        "en" -> Locale("en")
        else -> Locale.getDefault()
    }

    fun wrap(context: Context, language: String): Context {
        val locale = localeFor(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        return context.createConfigurationContext(config)
    }
}
