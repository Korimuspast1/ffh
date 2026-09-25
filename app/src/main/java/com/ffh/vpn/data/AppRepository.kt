package com.ffh.vpn.data

import android.content.Context
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.data.model.Subscription
import com.ffh.vpn.data.model.TrafficInfo
import com.ffh.vpn.data.parse.SubscriptionParser
import com.ffh.vpn.data.parse.parseUrl
import com.ffh.vpn.net.SubscriptionFetcher
import com.ffh.vpn.ui.theme.looksLikeThemeJson
import com.ffh.vpn.ui.theme.normalized
import com.ffh.vpn.ui.theme.parseTheme
import com.ffh.vpn.ui.theme.toJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class AppState(
    val subscriptions: List<Subscription> = emptyList(),
    val settings: AppSettings = AppSettings()
)

sealed interface AddResult {
    data class Added(val subscription: Subscription) : AddResult
    data class ThemeApplied(val json: String) : AddResult
    data object Invalid : AddResult
    data class Failed(val message: String) : AddResult
}

object AppRepository {

    private lateinit var stateFile: File
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppState())

    val state: StateFlow<AppState> = _state.asStateFlow()

    /** Ids of the subscriptions that are being refreshed right now. */
    private val _updating = MutableStateFlow<Set<String>>(emptySet())
    val updating: StateFlow<Set<String>> = _updating.asStateFlow()

    fun init(context: Context) {
        if (::stateFile.isInitialized) return
        stateFile = File(context.filesDir, "ffh/state.json")
        _state.value = JsonStore.load(stateFile, AppState())
    }

    val isReady: Boolean get() = ::stateFile.isInitialized

    private suspend fun persist() {
        val snapshot = _state.value
        withContext(Dispatchers.IO) { JsonStore.save(stateFile, snapshot) }
    }

    fun settings(): AppSettings = _state.value.settings

    fun subscription(id: String?): Subscription? =
        _state.value.subscriptions.firstOrNull { it.id == id }

    fun server(id: String?): ServerProfile? {
        if (id == null) return null
        for (sub in _state.value.subscriptions) {
            val found = sub.servers.firstOrNull { it.id == id }
            if (found != null) return found
        }
        return null
    }

    val selectedServer: ServerProfile?
        get() = server(_state.value.settings.selectedServerId)

    // ------------------------------------------------------------- settings

    suspend fun updateSettings(block: (AppSettings) -> AppSettings) = mutex.withLock {
        _state.value = _state.value.copy(settings = block(_state.value.settings))
        persist()
    }

    // -------------------------------------------------------- subscriptions

    suspend fun addSubscription(subscription: Subscription): Subscription = mutex.withLock {
        val list = _state.value.subscriptions.toMutableList()
        list.add(subscription)
        _state.value = _state.value.copy(subscriptions = list)
        persist()
        subscription
    }

    suspend fun removeSubscription(id: String) = mutex.withLock {
        val settings = _state.value.settings
        _state.value = _state.value.copy(
            subscriptions = _state.value.subscriptions.filter { it.id != id },
            settings = settings.copy(
                selectedServerId = settings.selectedServerId.takeIf { server(it)?.subscriptionId != id },
                selectedSubscriptionId = settings.selectedSubscriptionId.takeIf { it != id }
            )
        )
        persist()
    }

    suspend fun updateSubscriptionMeta(id: String, block: (Subscription) -> Subscription) = mutex.withLock {
        _state.value = _state.value.copy(
            subscriptions = _state.value.subscriptions.map { if (it.id == id) block(it) else it }
        )
        persist()
    }

    suspend fun moveSubscription(id: String, delta: Int) = mutex.withLock {
        val list = _state.value.subscriptions.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = (index + delta).coerceIn(0, list.lastIndex)
        if (index >= 0 && index != target) {
            val item = list.removeAt(index)
            list.add(target, item)
            _state.value = _state.value.copy(subscriptions = list)
            persist()
        }
    }

    suspend fun setSelection(subscriptionId: String?, serverId: String?) = mutex.withLock {
        _state.value = _state.value.copy(
            settings = _state.value.settings.copy(
                selectedSubscriptionId = subscriptionId,
                selectedServerId = serverId
            )
        )
        persist()
    }

    suspend fun setPing(results: Map<String, Int>) = mutex.withLock {
        if (results.isEmpty()) return
        _state.value = _state.value.copy(
            subscriptions = _state.value.subscriptions.map { sub ->
                sub.copy(servers = sub.servers.map { s ->
                    val value = results[s.id]
                    if (value != null) s.copy(pingMillis = value) else s
                })
            }
        )
        persist()
    }

    // ---------------------------------------------------------------- input

    /**
     * Handles anything the user pastes: an http(s) subscription URL, a blob of
     * links, base64, Clash YAML, Xray JSON — or a theme definition.
     */
    suspend fun addFromInput(input: String): AddResult {
        val text = input.trim()
        if (text.isBlank()) return AddResult.Invalid

        if (looksLikeThemeJson(text)) {
            val spec = parseTheme(text) ?: return AddResult.Invalid
            return AddResult.ThemeApplied(spec.normalized().toJson())
        }

        return withContext(Dispatchers.IO) {
            when {
                text.startsWith("http://", true) || text.startsWith("https://", true) -> {
                    val id = UUID.randomUUID().toString()
                    val sub = Subscription(
                        id = id,
                        name = hostOf(text),
                        url = text,
                        createdAt = System.currentTimeMillis()
                    )
                    addSubscription(sub)
                    val ok = refreshSubscription(id)
                    val stored = subscription(id) ?: sub
                    if (ok || stored.servers.isNotEmpty()) {
                        AddResult.Added(stored)
                    } else {
                        AddResult.Failed(stored.lastError ?: "empty subscription")
                    }
                }

                text.startsWith("sub://", true) -> {
                    val id = UUID.randomUUID().toString()
                    val parsed = SubscriptionParser.parseBody(text, id)
                    if (parsed.servers.isEmpty()) return@withContext AddResult.Invalid
                    val sub = Subscription(
                        id = id,
                        name = parsed.meta?.title ?: parsed.servers.first().displayName(),
                        servers = parsed.servers,
                        skipped = parsed.skipped,
                        createdAt = System.currentTimeMillis(),
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                    addSubscription(sub)
                    AddResult.Added(sub)
                }

                else -> {
                    val id = UUID.randomUUID().toString()
                    val parsed = SubscriptionParser.parseBody(text, id)
                    if (parsed.servers.isEmpty()) return@withContext AddResult.Invalid
                    val sub = Subscription(
                        id = id,
                        name = parsed.meta?.title ?: parsed.servers.first().displayName(),
                        description = parsed.meta?.description ?: "",
                        servers = parsed.servers,
                        skipped = parsed.skipped,
                        createdAt = System.currentTimeMillis(),
                        lastUpdatedAt = System.currentTimeMillis()
                    )
                    addSubscription(sub)
                    AddResult.Added(sub)
                }
            }
        }
    }

    /** Re-downloads a remote subscription and merges the result. */
    suspend fun refreshSubscription(id: String): Boolean = withContext(Dispatchers.IO) {
        val sub = subscription(id) ?: return@withContext false
        val url = sub.url
        if (url.isNullOrBlank()) return@withContext false
        val settings = settings()

        _updating.value = _updating.value + id
        try {
            val result = SubscriptionFetcher.fetch(url, settings.userAgent)
            if (result.body == null) {
                val message = result.error ?: "network error"
                LogStore.append("sub", "$url -> $message")
                if (!settings.keepServersOnFailure) {
                    updateSubscriptionMeta(id) { it.copy(lastError = message, servers = emptyList()) }
                } else {
                    updateSubscriptionMeta(id) { it.copy(lastError = message) }
                }
                return@withContext false
            }

            val parsed = SubscriptionParser.parseBody(result.body, id)
            if (parsed.servers.isEmpty() && !settings.keepServersOnFailure) {
                updateSubscriptionMeta(id) { it.copy(lastError = "no servers found") }
                return@withContext false
            }

            val traffic = result.traffic ?: sub.traffic
            val title = result.profileTitle?.takeIf { it.isNotBlank() }
                ?: parsed.meta?.title?.takeIf { it.isNotBlank() }
                ?: sub.name.takeIf { it.isNotBlank() && it != hostOf(url) }
                ?: hostOf(url)

            updateSubscriptionMeta(id) { current ->
                current.copy(
                    name = title,
                    description = result.announcement ?: parsed.meta?.description ?: current.description,
                    homepage = result.homepage ?: current.homepage,
                    traffic = traffic ?: current.traffic,
                    updateIntervalHours = result.updateIntervalHours
                        ?: parsed.meta?.updateIntervalHours
                        ?: current.updateIntervalHours,
                    lastUpdatedAt = System.currentTimeMillis(),
                    lastError = null,
                    skipped = parsed.skipped,
                    servers = if (parsed.servers.isEmpty()) current.servers else replaceServers(current, parsed.servers)
                )
            }
            true
        } catch (t: Throwable) {
            LogStore.append("sub", "update failed: ${t.message}")
            updateSubscriptionMeta(id) { it.copy(lastError = t.message ?: "error") }
            false
        } finally {
            _updating.value = _updating.value - id
        }
    }

    /**
     * Refreshes remote subscriptions whose data is older than their update
     * interval (`profile-update-interval` or the global setting).
     */
    suspend fun refreshStale(): Int {
        val settings = settings()
        if (settings.autoUpdateHours <= 0) return 0
        val now = System.currentTimeMillis()
        var count = 0
        for (sub in _state.value.subscriptions.filter { it.isRemote }) {
            val hours = (sub.updateIntervalHours?.takeIf { it > 0 } ?: settings.autoUpdateHours).toLong()
            val last = sub.lastUpdatedAt ?: 0L
            if (last <= 0L || now - last >= hours * 60 * 60 * 1000L) {
                if (refreshSubscription(sub.id)) count++
            }
        }
        return count
    }

    suspend fun refreshAll(): Int {
        val subs = _state.value.subscriptions.filter { it.isRemote }
        var ok = 0
        for (sub in subs) {
            if (refreshSubscription(sub.id)) ok++
        }
        return ok
    }

    /** Keeps existing ping results and ids when the same server comes back. */
    private fun replaceServers(current: Subscription, fresh: List<ServerProfile>): List<ServerProfile> {
        val previous = current.servers.associateBy { it.link.ifBlank { "${it.address}:${it.port}:${it.remark}" } }
        return fresh.map { server ->
            val key = server.link.ifBlank { "${server.address}:${server.port}:${server.remark}" }
            val old = previous[key]
            if (old != null) server.copy(id = old.id, pingMillis = old.pingMillis) else server
        }
    }

    fun buildExportLinks(sub: Subscription): String = sub.servers.joinToString("\n") {
        it.link.ifBlank { "" }
    }.trim()

    // --------------------------------------------------------------- backup

    fun exportBackup(): String = JsonStore.encode(_state.value)

    suspend fun importBackup(text: String): Boolean {
        val parsed = JsonStore.decode<AppState>(text) ?: return false
        mutex.withLock {
            _state.value = parsed
            persist()
        }
        return true
    }

    fun hostOf(url: String): String {
        val parts = parseUrl(url)
        val host = parts?.host?.takeIf { it.isNotBlank() }
        return host ?: url.take(48)
    }

    fun trafficOf(sub: Subscription): TrafficInfo? = sub.traffic
}
