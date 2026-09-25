package com.ffh.vpn.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ffh.vpn.core.VpnController
import com.ffh.vpn.core.VpnStateHolder
import com.ffh.vpn.core.VpnStatus
import com.ffh.vpn.core.xray.XrayConfigBuilder
import com.ffh.vpn.core.xray.XrayProbe
import com.ffh.vpn.data.AddResult
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.AppSettings
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.net.Pinger
import com.ffh.vpn.ui.screens.AboutScreen
import com.ffh.vpn.ui.screens.AddSubscriptionScreen
import com.ffh.vpn.ui.screens.AppearanceScreen
import com.ffh.vpn.ui.screens.AppsScreen
import com.ffh.vpn.ui.screens.BackupScreen
import com.ffh.vpn.ui.screens.ConfigPreview
import com.ffh.vpn.ui.screens.ConnectionScreen
import com.ffh.vpn.ui.screens.CoreScreen
import com.ffh.vpn.ui.screens.DnsScreen
import com.ffh.vpn.ui.screens.HomeScreen
import com.ffh.vpn.ui.screens.LogsScreen
import com.ffh.vpn.ui.screens.OptionsDialog
import com.ffh.vpn.ui.screens.RoutingScreen
import com.ffh.vpn.ui.screens.SettingsRootScreen
import com.ffh.vpn.ui.screens.SortDialog
import com.ffh.vpn.ui.screens.SubscriptionInfoScreen
import com.ffh.vpn.ui.screens.SubscriptionSettingsScreen
import com.ffh.vpn.ui.screens.SubscriptionsScreen
import com.ffh.vpn.ui.screens.ThemeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNav(
    onVpnPermission: (Intent) -> Unit,
    onCopyText: (String) -> Unit,
    onShareText: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onLanguageChanged: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    val state by AppRepository.state.collectAsState()
    val vpnState by VpnStateHolder.state.collectAsState()
    val updating by AppRepository.updating.collectAsState()
    val settings = state.settings

    val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }
    var optionsServer by remember { mutableStateOf<ServerProfile?>(null) }
    var sortDialog by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var configPreview by remember { mutableStateOf<String?>(null) }

    fun push(screen: Screen) {
        if (backStack.lastOrNull() != screen) backStack.add(screen)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun ping(servers: List<ServerProfile>) {
        if (servers.isEmpty()) return
        scope.launch {
            testing = true
            val results = if (settings.pingMode == "proxy") {
                // A real request through the server: slower, but it proves
                // that the server actually passes traffic.
                XrayProbe.measureAll(
                    context = context,
                    servers = servers.filter { it.isSupported },
                    url = settings.pingUrl,
                    timeoutMs = settings.pingTimeoutMs.coerceAtLeast(8_000),
                    parallelism = 2
                ) { partial -> AppRepository.setPing(partial) }
            } else {
                Pinger.measureAll(servers, settings.pingConcurrency, settings.pingTimeoutMs) { partial ->
                    AppRepository.setPing(partial)
                }
            }
            AppRepository.setPing(results)
            testing = false
        }
    }

    fun connect(serverId: String? = null) {
        scope.launch {
            when (val action = VpnController.connect(context, serverId)) {
                is VpnController.Action.NeedPermission -> onVpnPermission(action.intent)
                is VpnController.Action.Failed -> com.ffh.vpn.data.LogStore.append("vpn", action.message)
                VpnController.Action.Started -> Unit
            }
        }
    }

    fun togglePower() {
        scope.launch {
            when (val action = VpnController.toggle(context)) {
                is VpnController.Action.NeedPermission -> onVpnPermission(action.intent)
                is VpnController.Action.Failed -> com.ffh.vpn.data.LogStore.append("vpn", action.message)
                VpnController.Action.Started -> Unit
            }
        }
    }

    fun selectServer(id: String) {
        scope.launch {
            val server = AppRepository.server(id) ?: return@launch
            val wasConnected = VpnStateHolder.state.value.status == VpnStatus.CONNECTED
            if (wasConnected) VpnController.disconnect(context)
            AppRepository.setSelection(server.subscriptionId, id)
            if (wasConnected) {
                delay(700)
                connect(id)
            }
        }
    }

    fun updateSubscription(id: String) {
        scope.launch {
            if (AppRepository.refreshSubscription(id) && settings.pingOnUpdate) {
                AppRepository.subscription(id)?.let { ping(it.servers) }
            }
        }
    }

    fun applySettings(updated: AppSettings) {
        scope.launch {
            val previous = settings.language
            AppRepository.updateSettings { updated }
            if (AppRepository.settings().language != previous) {
                onLanguageChanged()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (settings.updateOnStart) {
            AppRepository.refreshAll()
        } else if (settings.autoUpdateHours > 0) {
            AppRepository.refreshStale()
        }
    }

    BackHandler(enabled = backStack.size > 1) { pop() }

    val subscriptionForInfo = (backStack.lastOrNull() as? Screen.Info)?.let { screen ->
        state.subscriptions.firstOrNull { it.id == screen.subscriptionId }
    }

    when (val screen = backStack.lastOrNull()) {
        Screen.Home -> HomeScreen(
            subscriptions = state.subscriptions,
            settings = settings,
            vpnState = vpnState,
            updating = updating,
            onOpenSubscriptions = { push(Screen.Subscriptions) },
            onOpenSubscriptionInfo = { push(Screen.Info(it)) },
            onOpenSettings = { push(Screen.SettingsRoot) },
            onAdd = { push(Screen.Add) },
            onUpdateSubscription = ::updateSubscription,
            onSelectServer = ::selectServer,
            onServerOptions = { optionsServer = it },
            onPowerClick = ::togglePower,
            onOpenHomepage = onOpenUrl,
            onOpenSupport = { push(Screen.About) },
            onSort = { sortDialog = true }
        )

        Screen.Subscriptions -> SubscriptionsScreen(
            subscriptions = state.subscriptions,
            updating = updating,
            onBack = ::pop,
            onAdd = { push(Screen.Add) },
            onOpen = { push(Screen.Info(it)) },
            onUpdate = ::updateSubscription,
            onUpdateAll = {
                scope.launch { AppRepository.refreshAll() }
            },
            onRename = { id, name ->
                scope.launch { AppRepository.updateSubscriptionMeta(id) { it.copy(name = name) } }
            },
            onDelete = { id -> scope.launch { AppRepository.removeSubscription(id) } }
        )

        Screen.Add -> AddSubscriptionScreen(
            onBack = ::pop,
            onDone = { id -> backStack.clear(); backStack.add(Screen.Home); backStack.add(Screen.Info(id)) },
            onSubmit = { text ->
                val result = AppRepository.addFromInput(text)
                if (result is AddResult.ThemeApplied) {
                    AppRepository.updateSettings { it.copy(customThemeJson = result.json) }
                }
                result
            }
        )

        is Screen.Info -> {
            val sub = subscriptionForInfo
            if (sub == null) {
                LaunchedEffect(Unit) { pop() }
            } else {
            SubscriptionInfoScreen(
                subscription = sub,
                settings = settings,
                updating = sub.id in updating,
                testing = testing,
                onBack = ::pop,
                onUpdate = { updateSubscription(sub.id) },
                onTestAll = { ping(sub.servers) },
                onSelectServer = ::selectServer,
                onServerOptions = { optionsServer = it },
                onRename = { name ->
                    scope.launch { AppRepository.updateSubscriptionMeta(sub.id) { it.copy(name = name) } }
                },
                onDelete = { scope.launch { AppRepository.removeSubscription(sub.id); pop() } },
                onExport = { onShareText(AppRepository.buildExportLinks(sub)) },
                onShare = { onShareText(sub.url ?: AppRepository.buildExportLinks(sub)) },
                onOpenHomepage = onOpenUrl,
                onSort = { sortDialog = true }
            )
            }
        }

        Screen.SettingsRoot -> SettingsRootScreen(
            onBack = ::pop,
            onOpen = { target ->
                push(
                    when (target) {
                        "appearance" -> Screen.Appearance
                        "connection" -> Screen.Connection
                        "routing" -> Screen.Routing
                        "dns" -> Screen.Dns
                        "subscriptions" -> Screen.SubscriptionsSettings
                        "core" -> Screen.Core
                        "logs" -> Screen.Logs
                        "backup" -> Screen.Backup
                        else -> Screen.About
                    }
                )
            }
        )

        Screen.Appearance -> AppearanceScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop,
            onOpenTheme = { push(Screen.Theme) }
        )

        Screen.Theme -> ThemeScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop,
            onCopyTheme = onCopyText
        )

        Screen.Connection -> ConnectionScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop
        )

        Screen.Routing -> RoutingScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop,
            onOpenApps = { push(Screen.Apps) }
        )

        Screen.Apps -> AppsScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop
        )

        Screen.Dns -> DnsScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop
        )

        Screen.SubscriptionsSettings -> SubscriptionSettingsScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop
        )

        Screen.Core -> CoreScreen(
            settings = settings,
            onChange = ::applySettings,
            onBack = ::pop,
            onOpenLogs = { push(Screen.Logs) },
            onExportConfig = {
                val server = AppRepository.selectedServer
                configPreview = if (server == null) {
                    "{}"
                } else {
                    runCatching { XrayConfigBuilder.build(server, settings) }.getOrDefault("{}")
                }
            }
        )

        Screen.Logs -> LogsScreen(onBack = ::pop, onCopy = onCopyText)

        Screen.Backup -> BackupScreen(
            onBack = ::pop,
            onCopy = onCopyText,
            onImport = { text ->
                var ok = false
                scope.launch { ok = AppRepository.importBackup(text) }
                ok
            }
        )

        Screen.About -> AboutScreen(onBack = ::pop)

        null -> Unit
    }

    if (sortDialog) {
        SortDialog(
            current = settings.serverSort,
            onPick = { mode -> applySettings(settings.copy(serverSort = mode)) },
            onDismiss = { sortDialog = false }
        )
    }

    optionsServer?.let { server ->
        OptionsDialog(
            title = server.displayName(),
            options = listOf(
                strings.selectServer to { selectServer(server.id) },
                strings.ping to { ping(listOf(server)) },
                strings.copyLink to { onCopyText(server.link) },
                strings.share to { onShareText(server.link) }
            ),
            onDismiss = { optionsServer = null }
        )
    }

    configPreview?.let { text ->
        ConfigPreview(
            text = text,
            onDismiss = { configPreview = null },
            onCopy = { onCopyText(text) }
        )
    }
}
