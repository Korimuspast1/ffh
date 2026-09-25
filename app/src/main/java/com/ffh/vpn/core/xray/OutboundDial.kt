package com.ffh.vpn.core.xray

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * The address Xray dials is not always the name shown in the list. Replacing a
 * domain with an IPv4 address before the tunnel starts means the core can open
 * the server socket without asking its own DNS — which would otherwise be
 * routed back into the tunnel that is not up yet.
 */
object OutboundDial {

    fun isIp(host: String): Boolean {
        val bare = host.trim().removePrefix("[").substringBefore("%").removeSuffix("]")
        if (bare.isEmpty()) return false
        if (bare.contains(':')) {
            return bare.all { it.isDigit() || it == ':' || it in 'a'..'f' || it in 'A'..'F' }
        }
        val parts = bare.split('.')
        return parts.size == 4 && parts.all { part -> part.toIntOrNull() in 0..255 }
    }

    /** Host the core will dial, without a port. */
    fun hostOf(outbound: JsonObject): String? {
        val settings = outbound["settings"] as? JsonObject ?: return null
        endpointHost(settings["peers"])?.let { return it }
        stringAt(settings, "vnext", "address")?.let { return it }
        stringAt(settings, "servers", "address")?.let { return it }
        val direct = settings["address"]
        if (direct is JsonPrimitive && direct.isString) return direct.content
        return null
    }

    /**
     * Xray 26.9.9 wants the websocket host as `wsSettings.host`. A `Host`
     * header is only a warning for ws and a hard error for httpupgrade, and
     * subscriptions already stored the old shape. Lift it before the core
     * reads the document.
     */
    fun normalize(outbound: JsonObject): JsonObject {
        val stream = outbound["streamSettings"] as? JsonObject ?: return outbound
        val copy = LinkedHashMap(stream)
        var changed = false
        for (key in listOf("wsSettings", "httpupgradeSettings", "xhttpSettings")) {
            if (liftHost(copy, key)) changed = true
        }
        if (!changed) return outbound
        return outbound.replacing("streamSettings", JsonObject(copy))
    }

    private fun liftHost(stream: MutableMap<String, JsonElement>, key: String): Boolean {
        val settings = stream[key] as? JsonObject ?: return false
        val headers = settings["headers"] as? JsonObject ?: return false
        val hostEntry = headers.entries.firstOrNull { it.key.equals("Host", ignoreCase = true) } ?: return false
        val updated = LinkedHashMap(settings)
        if (updated["host"] == null) updated["host"] = hostEntry.value
        val remaining = LinkedHashMap(headers)
        remaining.keys.filter { it.equals("Host", ignoreCase = true) }.forEach { remaining.remove(it) }
        if (remaining.isEmpty()) updated.remove("headers") else updated["headers"] = JsonObject(remaining)
        stream[key] = JsonObject(updated)
        return true
    }

    /**
     * Returns a copy whose dial address is [ip]. TLS server name, path and
     * every other field stay as they were parsed from the share link.
     */
    fun rewrite(outbound: JsonObject, ip: String): JsonObject {
        val settings = outbound["settings"] as? JsonObject ?: return outbound
        val updated = rewriteSettings(settings, ip) ?: return outbound
        return outbound.replacing("settings", updated)
    }

    private fun rewriteSettings(settings: JsonObject, ip: String): JsonObject? {
        val peers = settings["peers"] as? JsonArray
        if (peers != null && peers.isNotEmpty()) {
            val first = peers[0] as? JsonObject ?: return null
            val endpoint = first["endpoint"]?.jsonPrimitive?.contentOrNull ?: return null
            val rewritten = first.replacing("endpoint", JsonPrimitive(rewriteEndpoint(endpoint, ip)))
            return settings.replacing("peers", replaceFirst(peers, rewritten))
        }
        val vnext = settings["vnext"] as? JsonArray
        if (vnext != null && vnext.isNotEmpty()) {
            return settings.replacing("vnext", replaceAddress(vnext, ip))
        }
        val servers = settings["servers"] as? JsonArray
        if (servers != null && servers.isNotEmpty()) {
            return settings.replacing("servers", replaceAddress(servers, ip))
        }
        val address = settings["address"]
        if (address is JsonPrimitive && address.isString) {
            return settings.replacing("address", JsonPrimitive(ip))
        }
        return null
    }

    private fun endpointHost(peers: JsonElement?): String? {
        val first = (peers as? JsonArray)?.firstOrNull() as? JsonObject ?: return null
        val endpoint = first["endpoint"]?.jsonPrimitive?.contentOrNull ?: return null
        return endpointHost(endpoint)
    }

    fun endpointHost(endpoint: String): String {
        val trimmed = endpoint.trim()
        if (trimmed.startsWith("[")) return trimmed.substringAfter("[").substringBefore("]")
        val colon = trimmed.lastIndexOf(':')
        return if (colon > 0) trimmed.substring(0, colon) else trimmed
    }

    fun rewriteEndpoint(endpoint: String, ip: String): String {
        val trimmed = endpoint.trim()
        val port = when {
            trimmed.startsWith("[") -> trimmed.substringAfter("]:", "")
            trimmed.count { it == ':' } == 1 -> trimmed.substringAfter(':')
            else -> ""
        }
        if (port.isEmpty()) return if (ip.contains(':')) "[$ip]" else ip
        return if (ip.contains(':')) "[$ip]:$port" else "$ip:$port"
    }

    private fun stringAt(settings: JsonObject, arrayKey: String, field: String): String? {
        val array = settings[arrayKey] as? JsonArray ?: return null
        val first = array.firstOrNull() as? JsonObject ?: return null
        return first[field]?.jsonPrimitive?.contentOrNull
    }

    private fun replaceAddress(array: JsonArray, ip: String): JsonArray {
        val first = array[0] as? JsonObject ?: return array
        return replaceFirst(array, first.replacing("address", JsonPrimitive(ip)))
    }

    private fun replaceFirst(array: JsonArray, first: JsonObject): JsonArray = buildJsonArray {
        add(first)
        for (index in 1 until array.size) add(array[index])
    }

    private fun JsonObject.replacing(key: String, value: JsonElement): JsonObject {
        val copy = LinkedHashMap<String, JsonElement>(this)
        copy[key] = value
        return JsonObject(copy)
    }
}
