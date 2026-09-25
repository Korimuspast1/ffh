package com.ffh.vpn.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Atomic JSON file storage used for state, settings and backups. */
object JsonStore {

    val pretty = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    val compact = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    inline fun <reified T> load(file: File, fallback: T): T {
        if (!file.exists()) return fallback
        return runCatching {
            pretty.decodeFromString<T>(file.readText())
        }.getOrElse {
            runCatching { pretty.decodeFromString<T>(file.readText()) }.getOrDefault(fallback)
        }
    }

    inline fun <reified T> save(file: File, value: T) {
        val text = pretty.encodeToString(value)
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text)
        if (file.exists()) file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    inline fun <reified T> encode(value: T): String = pretty.encodeToString(value)

    inline fun <reified T> decode(text: String): T? = runCatching {
        pretty.decodeFromString<T>(text)
    }.getOrNull()
}
