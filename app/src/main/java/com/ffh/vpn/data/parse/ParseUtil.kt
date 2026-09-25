package com.ffh.vpn.data.parse

import java.net.URLDecoder
import java.util.Base64

/**
 * Framework free URL / base64 helpers.
 *
 * `android.net.Uri` is deliberately not used: it throws in JVM unit tests, and
 * the parsers are covered by tests.
 */
data class UrlParts(
    val scheme: String,
    val userInfo: String?,
    val host: String,
    val port: Int?,
    val pathAndQuery: String?,
    val query: Map<String, String>,
    val fragment: String?
) {
    /** Query lookup that ignores the case of the key (clients are inconsistent). */
    fun q(key: String): String? {
        query[key]?.let { return it }
        val lower = key.lowercase()
        for ((k, v) in query) {
            if (k.lowercase() == lower) return v
        }
        return null
    }

    fun qInt(key: String): Int? = q(key)?.trim()?.toIntOrNull()

    fun qLong(key: String): Long? = q(key)?.trim()?.toLongOrNull()

    fun qBool(key: String, default: Boolean = false): Boolean {
        val raw = q(key)?.trim()?.lowercase() ?: return default
        return raw == "1" || raw == "true" || raw == "yes"
    }
}

fun parseUrl(input: String): UrlParts? {
    val raw = input.trim()
    val sep = raw.indexOf("://")
    if (sep <= 0) return null
    val scheme = raw.substring(0, sep).lowercase()
    var rest = raw.substring(sep + 3)

    var fragment: String? = null
    val hash = rest.indexOf('#')
    if (hash >= 0) {
        fragment = rest.substring(hash + 1)
        rest = rest.substring(0, hash)
    }

    var queryString: String? = null
    val qIndex = rest.indexOf('?')
    if (qIndex >= 0) {
        queryString = rest.substring(qIndex + 1)
        rest = rest.substring(0, qIndex)
    }

    val slash = rest.indexOf('/')
    val authority: String
    val path: String?
    if (slash >= 0) {
        authority = rest.substring(0, slash)
        path = rest.substring(slash)
    } else {
        authority = rest
        path = null
    }

    val at = authority.lastIndexOf('@')
    val userInfo = if (at >= 0) authority.substring(0, at) else null
    val hostPort = if (at >= 0) authority.substring(at + 1) else authority
    val (host, port) = splitHostPort(hostPort)

    return UrlParts(
        scheme = scheme,
        userInfo = userInfo,
        host = host,
        port = port,
        pathAndQuery = path,
        query = parseQuery(queryString),
        fragment = fragment
    )
}

fun splitHostPort(input: String): Pair<String, Int?> {
    var s = input.trim()
    if (s.startsWith("[")) {
        val end = s.indexOf(']')
        if (end > 0) {
            val host = s.substring(1, end)
            val rest = s.substring(end + 1)
            return host to rest.removePrefix(":").toIntOrNull()
        }
    }
    val idx = s.lastIndexOf(':')
    if (idx > 0 && !s.substring(idx + 1).contains(':')) {
        val port = s.substring(idx + 1).toIntOrNull()
        if (port != null) return s.substring(0, idx) to port
    }
    return s to null
}

fun parseQuery(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    val map = LinkedHashMap<String, String>()
    for (pair in raw.split('&')) {
        if (pair.isBlank()) continue
        val eq = pair.indexOf('=')
        if (eq <= 0) {
            map[urlDecode(pair)] = ""
        } else {
            map[urlDecode(pair.substring(0, eq))] = urlDecode(pair.substring(eq + 1))
        }
    }
    return map
}

fun urlDecode(value: String): String = runCatching {
    URLDecoder.decode(value, "UTF-8")
}.getOrDefault(value)

fun urlEncode(value: String): String = buildString {
    val allowed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"
    for (byte in value.toByteArray(Charsets.UTF_8)) {
        val c = byte.toInt().toChar()
        if (c in allowed) append(c) else append('%').append(byte.toInt().and(0xFF).toString(16).uppercase().padStart(2, '0'))
    }
}

fun encodeBase64(value: String): String =
    Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))

fun encodeBase64Url(value: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))

/**
 * Decodes the base64 flavours met in the wild: standard, URL safe, without
 * padding, with line breaks or spaces. Returns null when the result is not
 * usable text.
 */
fun decodeBase64(input: String): String? {
    var s = input.trim()
        .replace("\n", "")
        .replace("\r", "")
        .replace(" ", "")
        .replace("\t", "")
        .replace('-', '+')
        .replace('_', '/')
    if (s.isEmpty()) return null

    val decoders = listOf(
        runCatching { Base64.getDecoder() }.getOrNull(),
        runCatching { Base64.getMimeDecoder() }.getOrNull()
    )

    for (decoder in decoders) {
        if (decoder == null) continue
        var candidate = s
        while (candidate.length % 4 != 0) candidate += "="
        val bytes = runCatching { decoder.decode(candidate) }.getOrNull()
            ?: runCatching { decoder.decode(s) }.getOrNull()
            ?: continue
        val text = String(bytes, Charsets.UTF_8)
        if (text.contains('\u0000')) continue
        return text
    }
    return null
}

/** True when the text looks like base64 (used to decide how to read a body). */
fun looksLikeBase64(text: String): Boolean {
    val s = text.trim()
    if (s.length < 8) return false
    if (s.contains("://") || s.contains("\n")) return false
    return s.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' || it == '-' || it == '_' }
}
