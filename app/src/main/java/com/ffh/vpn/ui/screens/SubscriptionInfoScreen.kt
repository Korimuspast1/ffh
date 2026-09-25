package com.ffh.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.data.model.sortedWithMode
import com.ffh.vpn.data.model.Subscription
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.Format
import com.ffh.vpn.ui.components.FfhTopBar
import com.ffh.vpn.ui.components.Hairline
import com.ffh.vpn.ui.components.IconAction
import com.ffh.vpn.ui.components.SecondaryButton
import com.ffh.vpn.ui.components.ServerRow
import com.ffh.vpn.ui.components.SettingRow
import com.ffh.vpn.ui.theme.FfhBackground
import com.ffh.vpn.ui.theme.LocalFfhPalette

@Composable
fun SubscriptionInfoScreen(
    subscription: Subscription,
    settings: com.ffh.vpn.data.AppSettings,
    updating: Boolean,
    testing: Boolean,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    onTestAll: () -> Unit,
    onSelectServer: (String) -> Unit,
    onServerOptions: (ServerProfile) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onOpenHomepage: (String) -> Unit,
    onSort: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    var showDelete by remember { mutableStateOf(false) }

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FfhTopBar(
                title = subscription.displayName,
                subtitle = if (subscription.isRemote) subscription.url else strings.localSubscription,
                onBack = onBack,
                actions = {
                    IconAction(Icons.Default.Refresh, strings.updateNow, palette.subHeaderButtonColor, onUpdate)
                    if (!subscription.homepage.isNullOrBlank()) {
                        IconAction(
                            Icons.Default.Public,
                            strings.homepage,
                            palette.profileWebPageIconColor,
                            onClick = { onOpenHomepage(subscription.homepage!!) }
                        )
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                item {
                    if (!subscription.description.isBlank()) {
                        Text(
                            text = subscription.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.serverRowSubTitleTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                    }

                    subscription.traffic?.let { traffic ->
                        if (traffic.hasTraffic) {
                            TrafficCard(
                                used = traffic.used,
                                total = traffic.total,
                                left = traffic.left,
                                expire = traffic.expire
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        SecondaryButton(
                            text = if (testing) strings.testing else strings.testAll,
                            onClick = onTestAll,
                            icon = Icons.Default.NetworkPing,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        SecondaryButton(
                            text = strings.sortServers,
                            onClick = onSort,
                            icon = Icons.Default.Sort,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        SecondaryButton(
                            text = strings.exportLinks,
                            onClick = onExport,
                            icon = Icons.Default.Link,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                }

                item {
                    InfoRows(
                        subscription = subscription,
                        updating = updating,
                        onRename = onRename,
                        onDelete = { showDelete = true },
                        onShare = onShare
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (subscription.servers.isEmpty()) {
                    item {
                        Text(
                            text = strings.noServers,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.serverRowSubTitleTextColor,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    items(items = subscription.servers.sortedWithMode(settings.serverSort), key = { it.id }) { server ->
                        ServerRow(
                            profile = server,
                            selected = server.id == settings.selectedServerId,
                            showPing = settings.showPing,
                            onClick = { onSelectServer(server.id) },
                            onLongClick = { onServerOptions(server) }
                        )
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }

    if (showDelete) {
        OptionsDialog(
            title = strings.deleteSubscriptionTitle,
            message = strings.deleteSubscriptionMessage,
            options = listOf(strings.delete to onDelete),
            onDismiss = { showDelete = false }
        )
    }
}

@Composable
private fun TrafficCard(used: Long, total: Long, left: Long, expire: Long) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    com.ffh.vpn.ui.components.CardBlock(
        color = palette.subscriptionInfoBackgroundColor,
        padding = PaddingValues(18.dp)
    ) {
        Text(
            text = Format.bytes(left),
            style = MaterialTheme.typography.displayMedium,
            color = palette.subscriptionInfoTextColor
        )
        Text(
            text = strings.trafficLeft,
            style = MaterialTheme.typography.labelMedium,
            color = palette.disclosureSubHeaderTextColor
        )
        Spacer(Modifier.height(16.dp))
        TrafficBar(used = used, total = total)
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${strings.used}: ${Format.bytes(used)}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.disclosureSubHeaderTextColor,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${strings.total}: ${Format.bytes(total)}",
                style = MaterialTheme.typography.bodySmall,
                color = palette.disclosureSubHeaderTextColor
            )
        }
        if (expire > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${strings.expires}: ${Format.date(expire)} (${
                    Format.expire(expire, strings) ?: "—"
                })",
                style = MaterialTheme.typography.bodySmall,
                color = palette.disclosureSubHeaderTextColor
            )
        }
    }
}

@Composable
private fun InfoRows(
    subscription: Subscription,
    updating: Boolean,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    var renameDialog by remember { mutableStateOf(false) }

    com.ffh.vpn.ui.components.CardBlock(color = palette.serverRowBackgroundColor) {
        Column {
            SettingRow(
                title = strings.servers,
                subtitle = null,
                trailing = { Text("${subscription.servers.size}", style = MaterialTheme.typography.bodyMedium, color = palette.serverRowSubTitleTextColor) }
            )
            Hairline()
            SettingRow(
                title = strings.lastUpdate,
                trailing = {
                    Text(
                        if (updating) strings.updating else Format.relativeTime(subscription.lastUpdatedAt ?: 0L),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.serverRowSubTitleTextColor
                    )
                }
            )
            subscription.updateIntervalHours?.let {
                Hairline()
                SettingRow(
                    title = strings.updateInterval,
                    trailing = {
                        Text("$it h", style = MaterialTheme.typography.bodyMedium, color = palette.serverRowSubTitleTextColor)
                    }
                )
            }
            if (subscription.skipped > 0) {
                Hairline()
                SettingRow(
                    title = strings.skippedServers,
                    trailing = {
                        Text("${subscription.skipped}", style = MaterialTheme.typography.bodyMedium, color = palette.serverRowSubTitleTextColor)
                    }
                )
            }
            subscription.lastError?.let {
                Hairline()
                SettingRow(title = strings.error, subtitle = it)
            }
            Hairline()
            SettingRow(title = strings.rename, icon = Icons.Default.Edit, onClick = { renameDialog = true })
            Hairline()
            SettingRow(title = strings.share, icon = Icons.Default.Share, onClick = onShare)
            Hairline()
            SettingRow(
                title = strings.delete,
                icon = Icons.Default.Delete,
                onClick = onDelete,
                trailing = {
                    Icon(
                        Icons.Default.Delete,
                        null,
                        tint = palette.serverRowSubTitleTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }

    if (renameDialog) {
        TextInputDialog(
            title = strings.rename,
            initial = subscription.name,
            confirmText = strings.save,
            onConfirm = { onRename(it) },
            onDismiss = { renameDialog = false }
        )
    }
}

@Composable
fun CopyIcon(onClick: () -> Unit) {
    val palette = LocalFfhPalette.current
    IconAction(Icons.Default.ContentCopy, null, palette.additionalOptionsButtonColor, onClick)
}
