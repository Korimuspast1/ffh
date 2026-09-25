package com.ffh.vpn.data.parse

import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.data.model.SubscriptionMeta
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads whatever a subscription endpoint returns:
 * base64 blob, plain list of links, Clash YAML, Xray/sing-box JSON.
 */
object SubscriptionParser {

    enum class Kind { LINKS, BASE64, CLASH, JSON }

    data class Result(
        val servers: List<ServerProfile>,
        val meta: SubscriptionMeta? = null,
        val kind: Kind = Kind.LINKS,
        val skipped: Int = 0
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseBody(body: String, subscriptionId: String): Result {
        val text = body.trim()
        if (text.isEmpty()) return Result(emptyList())

        when {
            text.startsWith("{") -> return parseJson(text, subscriptionId)
            ClashParser.hasProxies(text) -> {
                val servers = ClashParser.parse(text, subscriptionId)
                return Result(servers, kind = Kind.CLASH, skipped = 0)
            }
            looksLikeBase64(text) -> {
                val decoded = decodeBase64(text)
                if (!decoded.isNullOrBlank() && (decoded.contains("://") || ClashParser.hasProxies(decoded) || decoded.trim().startsWith("{"))) {
                    return parseBody(decoded, subscriptionId).copy(kind = Kind.BASE64)
                }
            }
        }

        return parseLinks(text, subscriptionId)
    }

    fun parseLinks(text: String, subscriptionId: String): Result {
        val servers = mutableListOf<ServerProfile>()
        var skipped = 0
        val chunks = text.replace("\r", "\n").split('\n', ' ', ';', '|')
        for (chunk in chunks) {
            val line = chunk.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue
            when {
                line.startsWith("sub://", ignoreCase = true) -> {
                    val inner = line.removePrefix("sub://").removePrefix("sub://")
                    val decoded = decodeBase64(inner) ?: inner
                    val nested = parseBody(decoded, subscriptionId)
                    servers += nested.servers
                    skipped += nested.skipped
                }
                else -> {
                    val profile = LinkParser.parse(line, subscriptionId)
                    if (profile != null) servers += profile else skipped++
                }
            }
        }
        return Result(servers, kind = Kind.LINKS, skipped = skipped)
    }

    private fun parseJson(text: String, subscriptionId: String): Result {
        val root = runCatching { json.parseToJsonElement(text) }.getOrNull() ?: return Result(emptyList())
        val obj = root as? JsonObject ?: return parseLinks(text, subscriptionId)

        val outbounds = obj["outbounds"] as? JsonArray
        if (outbounds != null) {
            val servers = outbounds.mapNotNull { LinkParser.parseOutboundJson(it) }
                .filter { it.protocol.name != "UNKNOWN" }
                .map { withSubscription(it, subscriptionId) }
            return Result(servers, meta = metaFromJson(obj), kind = Kind.JSON)
        }

        val proxies = obj["proxies"] as? JsonArray
        if (proxies != null) {
            val servers = proxies.mapNotNull { element ->
                val map = (element as? JsonObject)?.entries?.associate { (k, v) -> k to primitive(v) }
                map?.let { ClashParser.toProfile(it, subscriptionId) }
            }
            return Result(servers, meta = metaFromJson(obj), kind = Kind.JSON)
        }

        if (obj.containsKey("protocol") || obj.containsKey("settings") || obj.containsKey("type")) {
            val single = LinkParser.parseOutboundJson(root)?.let { withSubscription(it, subscriptionId) }
            return Result(listOfNotNull(single), meta = metaFromJson(obj), kind = Kind.JSON)
        }

        return Result(emptyList(), kind = Kind.JSON)
    }

    private fun withSubscription(profile: ServerProfile, subscriptionId: String): ServerProfile = profile.copy(
        id = profile.id.ifBlank { java.util.UUID.randomUUID().toString() },
        subscriptionId = subscriptionId
    )

    private fun metaFromJson(obj: JsonObject): SubscriptionMeta? {
        val title = obj["remarks"]?.let { primitive(it) }
            ?: obj["title"]?.let { primitive(it) }
            ?: obj["name"]?.let { primitive(it) }
        val description = obj["description"]?.let { primitive(it) }
            ?: obj["announcement"]?.let { primitive(it) }
        if (title == null && description == null) return null
        return SubscriptionMeta(title = title, description = description)
    }

    private fun primitive(element: JsonElement): String =
        if (element is JsonPrimitive) element.content else element.toString()

    /** Parses the `subscription-userinfo` header of a subscription response. */
    fun parseTrafficHeader(value: String?): com.ffh.vpn.data.model.TrafficInfo? {
        if (value.isNullOrBlank()) return null
        var upload = 0L
        var download = 0L
        var total = 0L
        var expire = 0L
        var found = false
        for (part in value.split(';')) {
            val eq = part.indexOf('=')
            if (eq <= 0) continue
            val key = part.substring(0, eq).trim().lowercase()
            val raw = part.substring(eq + 1).trim()
            when (key) {
                "upload" -> { upload = raw.toLongOrNull() ?: 0L; found = true }
                "download" -> { download = raw.toLongOrNull() ?: 0L; found = true }
                "total" -> { total = raw.toLongOrNull() ?: 0L; found = true }
                "expire" -> { expire = raw.toLongOrNull() ?: 0L; found = true }
            }
        }
        return if (found) com.ffh.vpn.data.model.TrafficInfo(upload, download, total, expire) else null
    }

    /** `profile-title` may be plain text, URL encoded or base64. */
    fun decodeTitle(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val decoded = runCatching { decodeBase64(raw) }.getOrNull()
        val candidates = listOfNotNull(decoded, urlDecode(raw))
        val best = candidates.firstOrNull { it.isNotBlank() && it.all { ch -> ch.isLetterOrDigit() || ch.isWhitespace() || ch in "-_.[]()@:/ " } }
            ?: raw
        return best.trim().ifBlank { null }
    }

    fun parseUpdateInterval(value: String?): Int? {
        val raw = value?.trim()?.lowercase() ?: return null
        val number = raw.filter { it.isDigit() }.toIntOrNull() ?: return null
        return when {
            raw.contains("hour") -> number
            raw.contains("day") -> number * 24
            else -> number.coerceIn(1, 720)
        }
    }

    @Suppress("unused")
    private fun JsonElement.asObjectOrNull(): JsonObject? = this as? JsonObject

    @Suppress("unused")
    private fun JsonElement.asArrayOrNull(): JsonArray? = (this as? JsonArray)?.jsonArray
}
