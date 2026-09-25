package com.ffh.vpn.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ffh.vpn.core.XrayProcess
import com.ffh.vpn.core.xray.XrayConfigBuilder
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.AppSettings
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.components.CardBlock
import com.ffh.vpn.ui.components.FfhTopBar
import com.ffh.vpn.ui.components.Hairline
import com.ffh.vpn.ui.components.IconAction
import com.ffh.vpn.ui.components.SectionHeader
import com.ffh.vpn.ui.components.SettingRow
import com.ffh.vpn.ui.components.SettingSliderRow
import com.ffh.vpn.ui.components.SettingSwitchRow
import com.ffh.vpn.ui.components.SettingValueRow
import com.ffh.vpn.ui.theme.AppThemeSpec
import com.ffh.vpn.ui.theme.FfhBackground
import com.ffh.vpn.ui.theme.LocalFfhPalette
import com.ffh.vpn.ui.theme.ThemePresets
import com.ffh.vpn.ui.theme.normalized
import com.ffh.vpn.ui.theme.parseTheme
import com.ffh.vpn.ui.theme.toJson

typealias SettingsChange = (AppSettings) -> Unit

// --------------------------------------------------------------------- shell

@Composable
fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FfhTopBar(title = title, onBack = onBack, actions = actions)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth(), content = content)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- root

@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit
) {
    val strings = LocalStrings.current
    SettingsScaffold(title = strings.settings, onBack = onBack) {
        SectionHeader(strings.sectionAppearance)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(
                    title = strings.appearance,
                    value = "",
                    icon = Icons.Default.Palette,
                    onClick = { onOpen("appearance") }
                )
            }
        }

        SectionHeader(strings.sectionConnection)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.connection, "", icon = Icons.Default.Tune) { onOpen("connection") }
                Hairline()
                SettingValueRow(strings.routing, "", icon = Icons.Default.Route) { onOpen("routing") }
                Hairline()
                SettingValueRow(strings.dns, "", icon = Icons.Default.Dns) { onOpen("dns") }
            }
        }

        SectionHeader(strings.sectionSubscriptions)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.subscriptions, "", icon = Icons.Default.Storage) { onOpen("subscriptions") }
            }
        }

        SectionHeader(strings.sectionCore)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.coreTitle, "", icon = Icons.Default.VpnKey) { onOpen("core") }
                Hairline()
                SettingValueRow(strings.logs, "", icon = Icons.Default.Description) { onOpen("logs") }
            }
        }

        SectionHeader(strings.sectionData)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.backup, "", icon = Icons.Default.Storage) { onOpen("backup") }
            }
        }

        SectionHeader(strings.sectionAbout)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.about, "", icon = Icons.Default.Info) { onOpen("about") }
            }
        }
    }
}

// ---------------------------------------------------------------- appearance

@Composable
fun AppearanceScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit,
    onOpenTheme: () -> Unit
) {
    val strings = LocalStrings.current
    var languageDialog by remember { mutableStateOf(false) }
    val languageLabel = when (settings.language) {
        "ru" -> strings.languageRussian
        "en" -> strings.languageEnglish
        else -> strings.languageSystem
    }

    SettingsScaffold(title = strings.appearance, onBack = onBack) {
        SectionHeader(strings.appearance)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.language, languageLabel, icon = Icons.Default.Language) { languageDialog = true }
                Hairline()
                SettingValueRow(strings.theme, "", icon = Icons.Default.Palette, onClick = onOpenTheme)
            }
        }

        SectionHeader(strings.sectionAppearance)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingSliderRow(
                    title = strings.fontScale,
                    value = settings.fontScale,
                    range = 0.85f..1.35f,
                    steps = 9,
                    format = { "%.2f×".format(it) },
                    onValueChange = { onChange(settings.copy(fontScale = it)) }
                )
                Hairline()
                SettingSwitchRow(strings.showPing, null, null, settings.showPing) {
                    onChange(settings.copy(showPing = it))
                }
                Hairline()
                SettingSwitchRow(strings.showTrafficInRows, null, null, settings.showTrafficInRows) {
                    onChange(settings.copy(showTrafficInRows = it))
                }
                Hairline()
                SettingSwitchRow(strings.haptics, null, null, settings.haptics) {
                    onChange(settings.copy(haptics = it))
                }
            }
        }
    }

    if (languageDialog) {
        OptionsDialog(
            title = strings.language,
            options = listOf(
                strings.languageSystem to { onChange(settings.copy(language = "system")) },
                strings.languageRussian to { onChange(settings.copy(language = "ru")) },
                strings.languageEnglish to { onChange(settings.copy(language = "en")) }
            ),
            onDismiss = { languageDialog = false }
        )
    }
}

// --------------------------------------------------------------------- theme

@Composable
fun ThemeScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit,
    onCopyTheme: (String) -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    var json by remember { mutableStateOf(settings.customThemeJson) }
    var message by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(title = strings.themeTitle, onBack = onBack) {
        SectionHeader(strings.themePresets)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                ThemePresets.all.forEachIndexed { index, preset ->
                    val active = settings.customThemeJson.isBlank() && settings.themePresetId == preset.id
                    SettingRow(
                        title = preset.name,
                        onClick = { onChange(settings.copy(themePresetId = preset.id, customThemeJson = "")) }
                    ) {
                        if (active) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = palette.settingsControlsTintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index != ThemePresets.all.lastIndex) Hairline()
                }
            }
        }

        SectionHeader(strings.themeCustom, strings.themeHint)
        CardBlock(
            color = LocalFfhPalette.current.serverRowBackgroundColor,
            padding = PaddingValues(16.dp)
        ) {
            SimpleTextField(
                value = json,
                onValueChange = { json = it; message = null },
                hint = "{ \"backgroundColors\": [\"#000000FF\", …] }",
                multiline = true,
                textStyle = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message!!, style = MaterialTheme.typography.bodySmall, color = palette.serverRowSubTitleTextColor)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                com.ffh.vpn.ui.components.SecondaryButton(
                    text = strings.themeExport,
                    onClick = { onCopyTheme(json.ifBlank { AppThemeSpec.DEFAULT.toJson() }) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                com.ffh.vpn.ui.components.FfhButton(
                    text = strings.themeApply,
                    onClick = {
                        val spec = parseTheme(json)
                        if (spec == null) {
                            message = strings.themeInvalid
                        } else {
                            onChange(settings.copy(customThemeJson = spec.normalized().toJson()))
                            message = strings.themeApplied
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            com.ffh.vpn.ui.components.SecondaryButton(
                text = strings.themeReset,
                onClick = {
                    json = ""
                    onChange(settings.copy(customThemeJson = "", themePresetId = "midnight"))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------------------------------------------------------- connection

@Composable
fun ConnectionScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var mtuDialog by remember { mutableStateOf(false) }
    var socksPortDialog by remember { mutableStateOf(false) }
    var httpPortDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = strings.connection, onBack = onBack) {
        SectionHeader(strings.connection)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.mtu, "${settings.tunMtu}") { mtuDialog = true }
                Hairline()
                SettingSwitchRow(strings.ipv6, null, null, settings.enableIpv6) {
                    onChange(settings.copy(enableIpv6 = it))
                }
                Hairline()
                SettingSwitchRow(strings.allowLan, null, null, settings.allowLan) {
                    onChange(settings.copy(allowLan = it))
                }
                Hairline()
                SettingSwitchRow(strings.excludeSelf, null, null, settings.excludeSelf) {
                    onChange(settings.copy(excludeSelf = it))
                }
            }
        }

        SectionHeader(strings.sectionConnection)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingSwitchRow(strings.socksPort, null, null, settings.socksEnabled) {
                    onChange(settings.copy(socksEnabled = it))
                }
                if (settings.socksEnabled) {
                    Hairline()
                    SettingValueRow(strings.socksPort, "${settings.socksPort}") { socksPortDialog = true }
                }
                Hairline()
                SettingSwitchRow(strings.httpPort, null, null, settings.httpEnabled) {
                    onChange(settings.copy(httpEnabled = it))
                }
                if (settings.httpEnabled) {
                    Hairline()
                    SettingValueRow(strings.httpPort, "${settings.httpPort}") { httpPortDialog = true }
                }
            }
        }

        SectionHeader(strings.sectionRouting)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingSwitchRow(strings.sniffing, null, null, settings.sniffing) {
                    onChange(settings.copy(sniffing = it))
                }
                Hairline()
                SettingSwitchRow(strings.blockQuic, null, null, settings.blockQuic) {
                    onChange(settings.copy(blockQuic = it))
                }
            }
        }

        SectionHeader(strings.sectionCore)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingSwitchRow(strings.autoConnect, null, null, settings.autoConnect) {
                    onChange(settings.copy(autoConnect = it))
                }
                Hairline()
                SettingSwitchRow(strings.connectOnBoot, null, null, settings.connectOnBoot) {
                    onChange(settings.copy(connectOnBoot = it))
                }
                Hairline()
                SettingSwitchRow(strings.reconnect, null, null, settings.reconnectOnFailure) {
                    onChange(settings.copy(reconnectOnFailure = it))
                }
            }
        }
    }

    if (mtuDialog) {
        TextInputDialog(
            title = strings.mtu,
            initial = "${settings.tunMtu}",
            confirmText = strings.save,
            onConfirm = { value ->
                value.toIntOrNull()?.coerceIn(576, 9000)?.let { onChange(settings.copy(tunMtu = it)) }
            },
            onDismiss = { mtuDialog = false }
        )
    }
    if (socksPortDialog) {
        TextInputDialog(
            title = strings.socksPort,
            initial = "${settings.socksPort}",
            confirmText = strings.save,
            onConfirm = { value ->
                value.toIntOrNull()?.coerceIn(1025, 65535)?.let { onChange(settings.copy(socksPort = it)) }
            },
            onDismiss = { socksPortDialog = false }
        )
    }
    if (httpPortDialog) {
        TextInputDialog(
            title = strings.httpPort,
            initial = "${settings.httpPort}",
            confirmText = strings.save,
            onConfirm = { value ->
                value.toIntOrNull()?.coerceIn(1025, 65535)?.let { onChange(settings.copy(httpPort = it)) }
            },
            onDismiss = { httpPortDialog = false }
        )
    }
}

// ------------------------------------------------------------------- routing

@Composable
fun RoutingScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit,
    onOpenApps: () -> Unit
) {
    val strings = LocalStrings.current
    var modeDialog by remember { mutableStateOf(false) }
    var perAppDialog by remember { mutableStateOf(false) }
    var subnetsDialog by remember { mutableStateOf(false) }

    val modeLabel = when (settings.routingMode) {
        "global" -> strings.routingGlobal
        "manual" -> strings.routingManual
        else -> strings.routingBypassLan
    }
    val perAppLabel = when (settings.perAppMode) {
        "include" -> strings.perAppInclude
        "exclude" -> strings.perAppExclude
        else -> strings.perAppOff
    }

    SettingsScaffold(title = strings.routing, onBack = onBack) {
        SectionHeader(strings.routing)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.routingMode, modeLabel) { modeDialog = true }
                Hairline()
                SettingValueRow(strings.perApp, perAppLabel, icon = Icons.Default.Apps) { perAppDialog = true }
                if (settings.perAppMode != "off") {
                    Hairline()
                    SettingValueRow(
                        title = strings.selectApps,
                        value = "${settings.perAppPackages.size} ${strings.appsCount}",
                        icon = Icons.Default.Apps,
                        onClick = onOpenApps
                    )
                }
                if (settings.routingMode != "global") {
                    Hairline()
                    SettingValueRow(
                        title = strings.bypassSubnets,
                        value = "${settings.bypassSubnets.size}",
                        onClick = { subnetsDialog = true }
                    )
                }
            }
        }
    }

    if (modeDialog) {
        OptionsDialog(
            title = strings.routingMode,
            options = listOf(
                strings.routingBypassLan to { onChange(settings.copy(routingMode = "bypass_lan")) },
                strings.routingGlobal to { onChange(settings.copy(routingMode = "global")) },
                strings.routingManual to { onChange(settings.copy(routingMode = "manual")) }
            ),
            onDismiss = { modeDialog = false }
        )
    }
    if (perAppDialog) {
        OptionsDialog(
            title = strings.perApp,
            options = listOf(
                strings.perAppOff to { onChange(settings.copy(perAppMode = "off")) },
                strings.perAppInclude to { onChange(settings.copy(perAppMode = "include")) },
                strings.perAppExclude to { onChange(settings.copy(perAppMode = "exclude")) }
            ),
            onDismiss = { perAppDialog = false }
        )
    }
    if (subnetsDialog) {
        TextInputDialog(
            title = strings.bypassSubnets,
            initial = settings.bypassSubnets.joinToString("\n"),
            confirmText = strings.save,
            multiline = true,
            onConfirm = { value ->
                val list = value.split('\n', ',', ';', ' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onChange(settings.copy(bypassSubnets = list))
            },
            onDismiss = { subnetsDialog = false }
        )
    }
}

// ---------------------------------------------------------------------- apps

data class AppInfo(val packageName: String, val label: String)

fun installedApps(context: Context): List<AppInfo> {
    val manager = context.packageManager
    val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
    }
    val resolved = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        manager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
    } else {
        manager.queryIntentActivities(intent, 0)
    }
    return resolved
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(manager).toString()) }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun AppsScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val context = LocalContext.current
    val apps by produceState(initialValue = emptyList<AppInfo>()) {
        value = installedApps(context)
    }
    var query by remember { mutableStateOf("") }
    val selected = settings.perAppPackages.toSet()

    val filtered = apps.filter {
        query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)
    }

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FfhTopBar(
                title = strings.selectApps,
                subtitle = "${selected.size} ${strings.appsCount}",
                onBack = onBack
            )
            SimpleTextField(
                value = query,
                onValueChange = { query = it },
                hint = strings.searchApps,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(50.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = filtered, key = { it.packageName }) { app ->
                    val checked = app.packageName in selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = if (checked) {
                                    settings.perAppPackages - app.packageName
                                } else {
                                    settings.perAppPackages + app.packageName
                                }
                                onChange(settings.copy(perAppPackages = next))
                            }
                            .padding(horizontal = 20.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                app.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.serverRowTitleTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.serverRowSubTitleTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (checked) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = palette.settingsControlsTintColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Hairline()
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ----------------------------------------------------------------------- dns

@Composable
fun DnsScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var modeDialog by remember { mutableStateOf(false) }
    var serversDialog by remember { mutableStateOf(false) }
    val modeLabel = if (settings.dnsMode == "local") strings.dnsLocal else strings.dnsRemote

    SettingsScaffold(title = strings.dns, onBack = onBack) {
        SectionHeader(strings.dns)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.dnsMode, modeLabel) { modeDialog = true }
                Hairline()
                SettingValueRow(
                    title = strings.dnsServers,
                    value = "${settings.dnsServers.size}",
                    subtitle = settings.dnsServers.joinToString(", ")
                ) { serversDialog = true }
            }
        }
    }

    if (modeDialog) {
        OptionsDialog(
            title = strings.dnsMode,
            options = listOf(
                strings.dnsRemote to { onChange(settings.copy(dnsMode = "remote")) },
                strings.dnsLocal to { onChange(settings.copy(dnsMode = "local")) }
            ),
            onDismiss = { modeDialog = false }
        )
    }
    if (serversDialog) {
        TextInputDialog(
            title = strings.dnsServers,
            initial = settings.dnsServers.joinToString("\n"),
            confirmText = strings.save,
            multiline = true,
            onConfirm = { value ->
                val list = value.split('\n', ',', ';', ' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                onChange(settings.copy(dnsServers = list))
            },
            onDismiss = { serversDialog = false }
        )
    }
}

// -------------------------------------------------------------- subscription

@Composable
fun SubscriptionSettingsScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    var autoDialog by remember { mutableStateOf(false) }
    var agentDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = strings.subscriptions, onBack = onBack) {
        SectionHeader(strings.sectionSubscriptions)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(
                    title = strings.autoUpdate,
                    value = if (settings.autoUpdateHours == 0) strings.off else "${settings.autoUpdateHours} h"
                ) { autoDialog = true }
                Hairline()
                SettingSwitchRow(strings.updateOnStart, null, null, settings.updateOnStart) {
                    onChange(settings.copy(updateOnStart = it))
                }
                Hairline()
                SettingSwitchRow(strings.updateOnlyWifi, null, null, settings.updateOnlyWifi) {
                    onChange(settings.copy(updateOnlyWifi = it))
                }
                Hairline()
                SettingSwitchRow(strings.keepServersOnFailure, null, null, settings.keepServersOnFailure) {
                    onChange(settings.copy(keepServersOnFailure = it))
                }
                Hairline()
                SettingValueRow(
                    title = strings.userAgent,
                    value = settings.userAgent.take(18),
                    subtitle = settings.userAgent
                ) { agentDialog = true }
            }
        }

        SectionHeader(strings.ping)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingSwitchRow(strings.pingOnUpdate, null, null, settings.pingOnUpdate) {
                    onChange(settings.copy(pingOnUpdate = it))
                }
                Hairline()
                SettingSliderRow(
                    title = strings.pingTimeout,
                    value = settings.pingTimeoutMs.toFloat(),
                    range = 500f..6000f,
                    steps = 10,
                    format = { "${it.toInt()} ms" }
                ) { onChange(settings.copy(pingTimeoutMs = it.toInt())) }
                Hairline()
                SettingSliderRow(
                    title = strings.pingConcurrency,
                    value = settings.pingConcurrency.toFloat(),
                    range = 4f..64f,
                    steps = 14,
                    format = { "${it.toInt()}" }
                ) { onChange(settings.copy(pingConcurrency = it.toInt())) }
            }
        }
    }

    if (autoDialog) {
        OptionsDialog(
            title = strings.autoUpdate,
            options = listOf(
                strings.off to { onChange(settings.copy(autoUpdateHours = 0)) },
                "6 h" to { onChange(settings.copy(autoUpdateHours = 6)) },
                "12 h" to { onChange(settings.copy(autoUpdateHours = 12)) },
                "24 h" to { onChange(settings.copy(autoUpdateHours = 24)) },
                "48 h" to { onChange(settings.copy(autoUpdateHours = 48)) }
            ),
            onDismiss = { autoDialog = false }
        )
    }
    if (agentDialog) {
        TextInputDialog(
            title = strings.userAgent,
            initial = settings.userAgent,
            confirmText = strings.save,
            onConfirm = { onChange(settings.copy(userAgent = it.trim().ifBlank { AppSettings.DEFAULT_USER_AGENT })) },
            onDismiss = { agentDialog = false }
        )
    }
}

// ---------------------------------------------------------------------- core

@Composable
fun CoreScreen(
    settings: AppSettings,
    onChange: SettingsChange,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
    onExportConfig: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val version by produceState(initialValue = strings.unknown) {
        value = XrayProcess.version(context)
    }
    var levelDialog by remember { mutableStateOf(false) }

    SettingsScaffold(title = strings.coreTitle, onBack = onBack) {
        SectionHeader(strings.core)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingRow(
                    title = strings.coreVersion,
                    trailing = {
                        Text(version, style = MaterialTheme.typography.bodyMedium, color = LocalFfhPalette.current.serverRowSubTitleTextColor)
                    }
                )
                Hairline()
                SettingValueRow(strings.logLevel, settings.logLevel) { levelDialog = true }
                Hairline()
                SettingValueRow(strings.logs, "", icon = Icons.Default.Description, onClick = onOpenLogs)
                Hairline()
                SettingValueRow(strings.exportConfig, "", icon = Icons.Default.Link, onClick = onExportConfig)
            }
        }
    }

    if (levelDialog) {
        OptionsDialog(
            title = strings.logLevel,
            options = AppSettings.LOG_LEVELS.map { level -> level to { onChange(settings.copy(logLevel = level)) } },
            onDismiss = { levelDialog = false }
        )
    }
}

// ---------------------------------------------------------------------- logs

@Composable
fun LogsScreen(onBack: () -> Unit, onCopy: (String) -> Unit) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val lines by LogStore.flow.collectAsState()

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FfhTopBar(
                title = strings.logs,
                onBack = onBack,
                actions = {
                    IconAction(Icons.Default.Delete, strings.clearLogs, palette.subHeaderButtonColor, onClick = { LogStore.clear() })
                    IconAction(Icons.Default.Link, strings.copyLogs, palette.subHeaderButtonColor, onClick = { onCopy(LogStore.snapshot()) })
                }
            )
            if (lines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.emptyLogs, style = MaterialTheme.typography.bodyMedium, color = palette.serverRowSubTitleTextColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = lines) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.serverRowSubTitleTextColor
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// -------------------------------------------------------------------- backup

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onImport: (String) -> Boolean
) {
    val strings = LocalStrings.current
    var importDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    SettingsScaffold(title = strings.backup, onBack = onBack) {
        SectionHeader(strings.backup, strings.backupHint)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingValueRow(strings.exportBackup, "", icon = Icons.Default.Link) {
                    onCopy(AppRepository.exportBackup())
                }
                Hairline()
                SettingValueRow(strings.importBackup, "", icon = Icons.Default.Storage) { importDialog = true }
            }
        }
        message?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = LocalFfhPalette.current.serverRowSubTitleTextColor,
                modifier = Modifier.padding(20.dp)
            )
        }
    }

    var importText by remember { mutableStateOf("") }
    if (importDialog) {
        TextInputDialog(
            title = strings.importBackup,
            initial = importText,
            confirmText = strings.save,
            multiline = true,
            onConfirm = { text ->
                importText = text
                message = if (onImport(text)) strings.importSuccess else strings.importFailed
            },
            onDismiss = { importDialog = false }
        )
    }
}

// --------------------------------------------------------------------- about

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val version by produceState(initialValue = "") {
        value = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
    }
    val coreVersion by produceState(initialValue = strings.unknown) {
        value = XrayProcess.version(context)
    }

    SettingsScaffold(title = strings.about, onBack = onBack) {
        SectionHeader(strings.about, strings.aboutHint)
        CardBlock(color = LocalFfhPalette.current.serverRowBackgroundColor) {
            Column {
                SettingRow(
                    title = strings.version,
                    trailing = {
                        Text(version, style = MaterialTheme.typography.bodyMedium, color = LocalFfhPalette.current.serverRowSubTitleTextColor)
                    }
                )
                Hairline()
                SettingRow(
                    title = strings.coreTitle,
                    trailing = {
                        Text(coreVersion, style = MaterialTheme.typography.bodyMedium, color = LocalFfhPalette.current.serverRowSubTitleTextColor)
                    }
                )
                Hairline()
                SettingRow(title = strings.protocols, subtitle = strings.protocolsValue)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ThemeSwatch(color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) LocalFfhPalette.current.buttonColor else LocalFfhPalette.current.hairline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    )
}

@Composable
fun ConfigPreview(text: String, onDismiss: () -> Unit, onCopy: () -> Unit) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.subscriptionInfoBackgroundColor)
                .border(1.dp, palette.hairline, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(XrayConfigBuilder.TAG_PROXY, style = MaterialTheme.typography.titleMedium, color = palette.serverRowTitleTextColor)
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.serverRowBackgroundColor)
                    .padding(12.dp)
            ) {
                Text(
                    text,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.serverRowSubTitleTextColor
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                com.ffh.vpn.ui.components.SecondaryButton(text = strings.cancel, onClick = onDismiss)
                Spacer(Modifier.width(10.dp))
                com.ffh.vpn.ui.components.FfhButton(text = strings.copy, onClick = onCopy)
            }
        }
    }
}
