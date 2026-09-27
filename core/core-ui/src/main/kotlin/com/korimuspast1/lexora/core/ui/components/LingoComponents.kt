package com.korimuspast1.lexora.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.korimuspast1.lexora.core.designsystem.icons.LexoraIcons
import com.korimuspast1.lexora.core.designsystem.theme.LexoraThemeValues

@Composable
fun LingoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prominent: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    val colors = if (prominent) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    } else {
        ButtonDefaults.outlinedButtonColors()
    }

    val content: @Composable () -> Unit = {
        if (leadingIcon != null) {
            Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text.uppercase(), fontWeight = FontWeight.Black)
    }

    if (prominent) {
        Button(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            colors = colors,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
            content = { Row(verticalAlignment = Alignment.CenterVertically, content = { content() }) },
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(56.dp),
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            content = { Row(verticalAlignment = Alignment.CenterVertically, content = { content() }) },
        )
    }
}

@Composable
fun LingoCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable Column.() -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "lingoCardBorder",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 8.dp else 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun LingoModal(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = "Cancel",
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = title, fontWeight = FontWeight.Bold) },
            text = { Text(text = message) },
            confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text(dismissText) } },
        )
    }
}

@Composable
fun LingoToast(
    message: String,
    modifier: Modifier = Modifier,
    success: Boolean = true,
) {
    val colors = LexoraThemeValues.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (success) colors.successContainer else MaterialTheme.colorScheme.errorContainer,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            text = message,
            color = if (success) colors.onSuccessContainer else MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun LingoProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp,
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
        color = LexoraThemeValues.colors.success,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        strokeCap = StrokeCap.Round,
    )
}

@Composable
fun LingoAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    level: Int? = null,
) {
    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials.take(2).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
            )
        }
        if (level != null) {
            LingoBadge(text = level.toString(), modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
fun LingoBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LexoraThemeValues.colors.gold,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color(0xFF2D2100),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LingoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    AssistChip(
        modifier = modifier,
        onClick = onClick,
        label = { Text(text = text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        leadingIcon = icon?.let { { Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(18.dp)) } },
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun <T> LingoDropdown(
    selected: T,
    items: List<T>,
    label: String,
    itemLabel: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }, shape = RoundedCornerShape(18.dp)) {
            Text(text = "$label: ${itemLabel(selected)}")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemLabel(item)) },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    },
                )
            }
        }
    }
}

@Composable
fun LingoTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    TabRow(selectedTabIndex = selectedIndex, modifier = modifier) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                text = { Text(title, fontWeight = FontWeight.Bold) },
            )
        }
    }
}

@Composable
fun LingoSkeleton(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(animation = tween(760), repeatMode = RepeatMode.Reverse),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)),
    )
}

@Composable
fun LingoSpinner(modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier.size(36.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

data class LingoNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun LingoBottomBar(
    items: List<LingoNavItem>,
    selectedRoute: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, tonalElevation = 10.dp) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.route == selectedRoute,
                onClick = { onSelected(item.route) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

@Composable
fun LingoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = LexoraIcons.Flag,
    trailing: @Composable Row.() -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
    }
}

enum class LingoNodeStatus { Completed, Current, Locked, Legendary }

@Composable
fun LingoPathNode(
    status: LingoNodeStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LexoraThemeValues.colors
    val containerColor = when (status) {
        LingoNodeStatus.Completed -> colors.gold
        LingoNodeStatus.Current -> MaterialTheme.colorScheme.primary
        LingoNodeStatus.Locked -> MaterialTheme.colorScheme.surfaceVariant
        LingoNodeStatus.Legendary -> colors.legendary
    }
    val icon = when (status) {
        LingoNodeStatus.Completed -> LexoraIcons.Check
        LingoNodeStatus.Current -> LexoraIcons.Star
        LingoNodeStatus.Locked -> LexoraIcons.Lock
        LingoNodeStatus.Legendary -> LexoraIcons.Crown
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (status == LingoNodeStatus.Locked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun LingoHeartRow(
    lives: Int,
    modifier: Modifier = Modifier,
    total: Int = 5,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { index ->
            Icon(
                imageVector = if (index < lives) LexoraIcons.Heart else LexoraIcons.BrokenHeart,
                contentDescription = null,
                tint = if (index < lives) LexoraThemeValues.colors.heart else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun LingoStreakFlame(
    days: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = LexoraThemeValues.colors.warningContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = LexoraIcons.Flame, contentDescription = null, tint = LexoraThemeValues.colors.streak, modifier = Modifier.size(20.dp))
            Text(text = days.toString(), color = LexoraThemeValues.colors.onWarningContainer, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun LingoConnector(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
) {
    Canvas(modifier = modifier.width(2.dp).height(40.dp)) {
        drawLine(
            color = color,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
    }
}
