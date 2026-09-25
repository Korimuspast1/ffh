package com.ffh.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ffh.vpn.core.QuickTile
import com.ffh.vpn.core.VpnState
import com.ffh.vpn.core.VpnStatus
import com.ffh.vpn.data.AppSettings
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.data.model.Subscription
import com.ffh.vpn.data.model.sortedWithMode
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.Format
import com.ffh.vpn.ui.components.IconAction
import com.ffh.vpn.ui.components.PowerButton
import com.ffh.vpn.ui.components.ServerRow
import com.ffh.vpn.ui.theme.FfhBackground
import com.ffh.vpn.ui.theme.LocalFfhPalette
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    subscriptions: List<Subscription>,
    settings: AppSettings,
    vpnState: VpnState,
    updating: Set<String>,
    onOpenSubscriptions: () -> Unit,
    onOpenSubscriptionInfo: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onAdd: () -> Unit,
    onUpdateSubscription: (String) -> Unit,
    onSelectServer: (String) -> Unit,
    onServerOptions: (ServerProfile) -> Unit,
    onPowerClick: () -> Unit,
    onOpenHomepage: (String) -> Unit,
    onOpenSupport: () -> Unit,
    onSort: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val context = LocalContext.current

    val selectedSub = subscriptions.firstOrNull { it.id == settings.selectedSubscriptionId }
        ?: subscriptions.firstOrNull()
    val servers = (selectedSub?.servers ?: emptyList()).sortedWithMode(settings.serverSort)

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // ------------------------------------------------------- top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconAction(Icons.Default.Help, strings.homepage, palette.supportIconColor, onOpenSupport)
                Spacer(Modifier.weight(1f))
                if (!selectedSub?.homepage.isNullOrBlank()) {
                    IconAction(
                        Icons.Default.Public,
                        strings.homepage,
                        palette.profileWebPageIconColor,
                        { onOpenHomepage(selectedSub.homepage!!) }
                    )
                }
                IconAction(
                    Icons.Default.PowerSettingsNew,
                    strings.addTile,
                    palette.topBarButtonsColor,
                    onClick = {
                        QuickTile.request(context, strings.addTileAdded, strings.addTileAlready, strings.addTileManual)
                    }
                )
                IconAction(Icons.Default.Sort, strings.sortServers, palette.topBarButtonsColor, onSort)
                IconAction(Icons.Default.Settings, strings.settings, palette.topBarButtonsColor, onOpenSettings)
            }

            // --------------------------------------------------- subs header
            SubsHeader(
                subscription = selectedSub,
                updating = selectedSub?.id in updating,
                strings = strings,
                onOpen = { selectedSub?.let { onOpenSubscriptionInfo(it.id) } ?: onOpenSubscriptions() },
                onAdd = onAdd,
                onUpdate = { selectedSub?.let { onUpdateSubscription(it.id) } }
            )

            // ---------------------------------------------- subscription info
            if (selectedSub != null) {
                SubscriptionBrief(
                    subscription = selectedSub,
                    showTraffic = settings.showTrafficInRows,
                    onOpen = { onOpenSubscriptionInfo(selectedSub.id) }
                )
            }

            // ---------------------------------------------------- server list
            if (servers.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (subscriptions.isEmpty()) strings.homeEmptyTitle else strings.noServers,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.serverRowSubTitleTextColor
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(settings.selectedServerId) {
                    val index = servers.indexOfFirst { it.id == settings.selectedServerId }
                    if (index >= 0) listState.animateScrollToItem(index)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
                ) {
                    items(items = servers, key = { it.id }) { server ->
                        ServerRow(
                            profile = server,
                            selected = server.id == settings.selectedServerId,
                            showPing = settings.showPing,
                            onClick = { onSelectServer(server.id) },
                            onLongClick = { onServerOptions(server) }
                        )
                    }
                }
            }

            // --------------------------------------------------------- bottom
            BottomPower(
                vpnState = vpnState,
                selectedName = servers.firstOrNull { it.id == settings.selectedServerId }?.displayName()
                    ?: (if (servers.isEmpty()) strings.noServerSelected else strings.selectServer),
                onPowerClick = onPowerClick
            )
        }
    }
}

@Composable
private fun SubsHeader(
    subscription: Subscription?,
    updating: Boolean,
    strings: com.ffh.vpn.i18n.Strings,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onUpdate: () -> Unit
) {
    val palette = LocalFfhPalette.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(shape)
            .background(palette.subsHeaderColor)
            .border(1.dp, palette.hairline, shape)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscription?.displayName ?: strings.addSubscription,
                style = MaterialTheme.typography.titleMedium,
                color = palette.serverRowTitleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (subscription == null) strings.homeEmptySubtitle
                else Format.subscriptionSummary(subscription, strings),
                style = MaterialTheme.typography.bodySmall,
                color = palette.serverRowSubTitleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (subscription != null) {
            IconAction(
                imageVector = Icons.Default.Refresh,
                description = strings.updateNow,
                tint = palette.subHeaderButtonColor,
                onClick = onUpdate
            )
        }
        IconAction(
            imageVector = Icons.Default.Add,
            description = strings.add,
            tint = palette.subHeaderButtonColor,
            onClick = onAdd
        )
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = palette.serverRowChevronColor.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SubscriptionBrief(
    subscription: Subscription,
    showTraffic: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val traffic = subscription.traffic
    if (!showTraffic || traffic == null || !traffic.hasTraffic) return

    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape)
            .background(palette.subscriptionInfoBackgroundColor)
            .border(1.dp, palette.hairline, shape)
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Storage,
                null,
                tint = palette.subscriptionInfoTextColor.copy(alpha = 0.75f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = Format.bytes(traffic.left),
                style = MaterialTheme.typography.titleSmall,
                color = palette.subscriptionInfoTextColor,
                modifier = Modifier.weight(1f)
            )
            Format.expire(traffic.expire, strings)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.disclosureSubHeaderTextColor
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TrafficBar(used = traffic.used, total = traffic.total)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${Format.bytes(traffic.used)} ${strings.of} ${Format.bytes(traffic.total)}",
            style = MaterialTheme.typography.bodySmall,
            color = palette.disclosureSubHeaderTextColor
        )
    }
}

@Composable
fun TrafficBar(used: Long, total: Long, modifier: Modifier = Modifier) {
    val palette = LocalFfhPalette.current
    val ratio = if (total > 0) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(palette.subscriptionTrafficBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(ratio)
                .height(8.dp)
                .clip(CircleShape)
                .background(palette.subscriptionInfoTextColor)
        )
    }
}

@Composable
private fun BottomPower(
    vpnState: VpnState,
    selectedName: String,
    onPowerClick: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(vpnState.status) {
        while (vpnState.status == VpnStatus.CONNECTED) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    val connected = vpnState.status == VpnStatus.CONNECTED
    val busy = vpnState.status == VpnStatus.CONNECTING || vpnState.status == VpnStatus.DISCONNECTING

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 26.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (connected && vpnState.startedAt != null) {
            Text(
                text = Format.duration(now - vpnState.startedAt),
                style = MaterialTheme.typography.titleMedium,
                color = palette.buttonTimerColor
            )
        } else {
            Text(
                text = when (vpnState.status) {
                    VpnStatus.CONNECTING -> strings.connecting
                    VpnStatus.DISCONNECTING -> strings.disconnecting
                    VpnStatus.ERROR -> vpnState.message ?: strings.error
                    else -> strings.notConnected
                },
                style = MaterialTheme.typography.titleMedium,
                color = palette.buttonTimerColor
            )
        }
        Spacer(Modifier.height(16.dp))
        PowerButton(connected = connected, busy = busy, onClick = onPowerClick)
        Spacer(Modifier.height(14.dp))
        Text(
            text = selectedName,
            style = MaterialTheme.typography.bodySmall,
            color = palette.serverRowSubTitleTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
    }
}

@Composable
fun RowDivider(color: Color = LocalFfhPalette.current.hairline) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}
