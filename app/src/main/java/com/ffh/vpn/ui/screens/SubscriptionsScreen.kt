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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ffh.vpn.data.model.Subscription
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.Format
import com.ffh.vpn.ui.components.EmptyState
import com.ffh.vpn.ui.components.FfhTopBar
import com.ffh.vpn.ui.components.IconAction
import com.ffh.vpn.ui.theme.FfhBackground
import com.ffh.vpn.ui.theme.LocalFfhPalette

@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    updating: Set<String>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onUpdate: (String) -> Unit,
    onUpdateAll: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    var menuSubscription by remember { mutableStateOf<Subscription?>(null) }
    var renameSubscription by remember { mutableStateOf<Subscription?>(null) }
    var deleteSubscription by remember { mutableStateOf<Subscription?>(null) }

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            FfhTopBar(
                title = strings.subscriptions,
                subtitle = if (subscriptions.isEmpty()) null else "${subscriptions.size}",
                onBack = onBack,
                actions = {
                    if (subscriptions.isNotEmpty()) {
                        IconAction(Icons.Default.Refresh, strings.updateAll, palette.subHeaderButtonColor, onUpdateAll)
                    }
                    IconAction(Icons.Default.Add, strings.add, palette.subHeaderButtonColor, onAdd)
                }
            )

            if (subscriptions.isEmpty()) {
                EmptyState(
                    title = strings.homeEmptyTitle,
                    subtitle = strings.homeEmptySubtitle,
                    icon = Icons.Default.Add,
                    action = {
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.buttonColor)
                                .clickable(onClick = onAdd)
                                .padding(horizontal = 26.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.addSubscription, style = MaterialTheme.typography.titleSmall, color = palette.buttonTextColor)
                        }
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(items = subscriptions, key = { it.id }) { sub ->
                        SubscriptionListItem(
                            subscription = sub,
                            updating = sub.id in updating,
                            onClick = { onOpen(sub.id) },
                            onLongClick = { menuSubscription = sub }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    menuSubscription?.let { sub ->
        OptionsDialog(
            title = sub.displayName,
            options = listOf(
                strings.updateNow to { onUpdate(sub.id) },
                strings.rename to { renameSubscription = sub },
                strings.delete to { deleteSubscription = sub }
            ),
            onDismiss = { menuSubscription = null }
        )
    }

    renameSubscription?.let { sub ->
        TextInputDialog(
            title = strings.rename,
            initial = sub.name,
            confirmText = strings.save,
            onConfirm = { onRename(sub.id, it) },
            onDismiss = { renameSubscription = null }
        )
    }

    deleteSubscription?.let { sub ->
        OptionsDialog(
            title = strings.deleteSubscriptionTitle,
            message = strings.deleteSubscriptionMessage,
            options = listOf(strings.delete to { onDelete(sub.id) }),
            onDismiss = { deleteSubscription = null }
        )
    }
}

@Composable
fun SubscriptionListItem(
    subscription: Subscription,
    updating: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(shape)
            .background(palette.serverRowBackgroundColor)
            .border(1.dp, palette.hairline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = subscription.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = palette.serverRowTitleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (updating) strings.updating else Format.subscriptionSummary(subscription, strings),
                style = MaterialTheme.typography.bodySmall,
                color = palette.serverRowSubTitleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subscription.lastError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.serverRowSubTitleTextColor.copy(alpha = 0.8f)
                )
            }
        }
        if (updating) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.serverRowSubTitleTextColor.copy(alpha = 0.4f))
            )
        } else {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = palette.serverRowChevronColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Minimal option list dialog used everywhere instead of a bottom sheet. */
@Composable
fun OptionsDialog(
    title: String,
    message: String? = null,
    options: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.subscriptionInfoBackgroundColor)
                .border(1.dp, palette.hairline, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.serverRowTitleTextColor)
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = palette.serverRowSubTitleTextColor)
            }
            Spacer(Modifier.height(12.dp))
            options.forEach { (label, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            onDismiss()
                            action()
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when {
                        label == strings.delete -> Icons.Default.Delete
                        label == strings.rename -> Icons.Default.Edit
                        label == strings.updateNow -> Icons.Default.Refresh
                        else -> null
                    }
                    if (icon != null) {
                        Icon(icon, null, tint = palette.settingsControlsTintColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = palette.serverRowTitleTextColor)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.cancel, style = MaterialTheme.typography.labelLarge, color = palette.serverRowSubTitleTextColor)
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    multiline: Boolean = false
) {
    val strings = LocalStrings.current
    var text by remember { mutableStateOf(initial) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(com.ffh.vpn.ui.theme.LocalFfhPalette.current.subscriptionInfoBackgroundColor)
                .border(1.dp, com.ffh.vpn.ui.theme.LocalFfhPalette.current.hairline, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = com.ffh.vpn.ui.theme.LocalFfhPalette.current.serverRowTitleTextColor)
            Spacer(Modifier.height(14.dp))
            SimpleTextField(
                value = text,
                onValueChange = { text = it },
                hint = strings.value,
                multiline = multiline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (multiline) 160.dp else 52.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(strings.cancel, style = MaterialTheme.typography.labelLarge, color = com.ffh.vpn.ui.theme.LocalFfhPalette.current.serverRowSubTitleTextColor)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(com.ffh.vpn.ui.theme.LocalFfhPalette.current.buttonColor)
                        .clickable {
                            onConfirm(text)
                            onDismiss()
                        }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(confirmText, style = MaterialTheme.typography.labelLarge, color = com.ffh.vpn.ui.theme.LocalFfhPalette.current.buttonTextColor)
                }
            }
        }
    }
}

/** Borderless text field that follows the active theme. */
@Composable
fun SimpleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge
) {
    val palette = com.ffh.vpn.ui.theme.LocalFfhPalette.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(palette.serverRowBackgroundColor)
            .border(1.dp, palette.hairline, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(hint, style = textStyle, color = palette.serverRowSubTitleTextColor.copy(alpha = 0.7f))
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = textStyle.copy(color = palette.serverRowTitleTextColor),
            singleLine = !multiline,
            maxLines = if (multiline) 20 else 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
