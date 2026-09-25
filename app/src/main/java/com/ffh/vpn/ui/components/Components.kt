package com.ffh.vpn.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.Format
import com.ffh.vpn.ui.theme.LocalFfhPalette

// ------------------------------------------------------------------ top bar

@Composable
fun FfhTopBar(
    title: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val palette = LocalFfhPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconAction(Icons.Default.ArrowBack, "back", palette.topBarButtonsColor, onBack)
        } else {
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.serverRowTitleTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.serverRowSubTitleTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

@Composable
fun IconAction(
    imageVector: ImageVector,
    description: String?,
    tint: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 22.dp
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = tint),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = description,
            tint = tint.copy(alpha = if (enabled) 1f else 0.3f),
            modifier = Modifier.size(size)
        )
    }
}

// ------------------------------------------------------------------- blocks

@Composable
fun SectionHeader(title: String, subtitle: String? = null) {
    val palette = LocalFfhPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 18.dp, bottom = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = palette.disclosureHeaderTextColor.copy(alpha = 0.5f)
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.disclosureSubHeaderTextColor
            )
        }
    }
}

@Composable
fun CardBlock(
    modifier: Modifier = Modifier,
    color: Color = LocalFfhPalette.current.subscriptionInfoBackgroundColor,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalFfhPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color)
            .border(1.dp, palette.hairline, shape)
            .padding(padding),
        content = content
    )
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    val palette = LocalFfhPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.hairline)
    )
}

// ------------------------------------------------------------ settings rows

@Composable
fun SettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val palette = LocalFfhPalette.current
    val modifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(color = palette.settingsControlsTintColor),
            onClick = onClick
        )
    } else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = palette.settingsControlsTintColor.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.serverRowTitleTextColor)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.serverRowSubTitleTextColor
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

@Composable
fun SettingValueRow(
    title: String,
    value: String = "",
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val palette = LocalFfhPalette.current
    SettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        trailing = {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.serverRowSubTitleTextColor,
                maxLines = 1
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = palette.serverRowChevronColor.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    )
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalFfhPalette.current
    SettingRow(
        title = title,
        subtitle = subtitle,
        icon = icon
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.buttonColor,
                checkedTrackColor = palette.buttonColor.copy(alpha = 0.35f),
                uncheckedThumbColor = palette.serverRowSubTitleTextColor,
                uncheckedTrackColor = palette.serverRowSubTitleTextColor.copy(alpha = 0.2f),
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingSliderRow(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    format: (Float) -> String,
    onValueChange: (Float) -> Unit
) {
    val palette = LocalFfhPalette.current
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.serverRowTitleTextColor, modifier = Modifier.weight(1f))
            Text(format(value), style = MaterialTheme.typography.bodyMedium, color = palette.serverRowSubTitleTextColor)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = palette.buttonColor,
                activeTrackColor = palette.buttonColor,
                inactiveTrackColor = palette.serverRowSubTitleTextColor.copy(alpha = 0.2f)
            )
        )
    }
}

// -------------------------------------------------------------------- rows

@Composable
fun ServerRow(
    profile: ServerProfile,
    selected: Boolean,
    showPing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val palette = LocalFfhPalette.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(shape)
            .background(if (selected) palette.selectedServerRowColor else palette.serverRowBackgroundColor)
            .border(
                width = 1.dp,
                color = if (selected) palette.serverRowTitleTextColor.copy(alpha = 0.25f) else palette.hairline,
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.displayName(),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.serverRowTitleTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!profile.isSupported) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.serverRowBackgroundColor,
                        modifier = Modifier
                            .background(palette.serverRowSubTitleTextColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Text(
                text = profile.subtitle(),
                style = MaterialTheme.typography.bodySmall,
                color = palette.serverRowSubTitleTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showPing) {
            val strings = LocalStrings.current
            Text(
                text = when {
                    profile.pingMillis <= 0 -> "—"
                    else -> "${profile.pingMillis} ${strings.ms}"
                },
                style = MaterialTheme.typography.labelMedium,
                color = pingColor(profile.pingMillis),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (selected) Icons.Default.Check else Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (selected) palette.serverRowTitleTextColor else palette.serverRowChevronColor.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun pingColor(millis: Int): Color {
    val palette = LocalFfhPalette.current
    return when {
        millis <= 0 -> palette.serverRowSubTitleTextColor
        millis < 300 -> palette.serverRowTitleTextColor
        millis < 800 -> palette.serverRowTitleTextColor.copy(alpha = 0.8f)
        else -> palette.serverRowSubTitleTextColor
    }
}

// --------------------------------------------------------------- power button

@Composable
fun PowerButton(
    connected: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFfhPalette.current
    val infinite = rememberInfiniteTransition(label = "power")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (busy) 1.12f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .size(96.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .drawBehind {
                if (connected) {
                    drawCircle(
                        color = palette.buttonColor.copy(alpha = 0.16f),
                        radius = size.minDimension * 0.62f
                    )
                }
            }
            .clip(CircleShape)
            .background(if (connected) palette.buttonColor else palette.buttonColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = palette.powerIconColor),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PowerSettingsNew,
            contentDescription = "power",
            tint = palette.powerIconColor,
            modifier = Modifier
                .size(40.dp)
                .alpha(if (busy) 0.6f else 1f)
        )
    }
}

// ------------------------------------------------------------------- buttons

@Composable
fun FfhButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val palette = LocalFfhPalette.current
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.buttonColor.copy(alpha = if (enabled) 1f else 0.35f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = palette.buttonTextColor),
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, null, tint = palette.buttonTextColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = palette.buttonTextColor
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val palette = LocalFfhPalette.current
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.subscriptionInfoBackgroundColor)
            .border(1.dp, palette.hairline, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = palette.settingsControlsTintColor),
                onClick = onClick
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = palette.subHeaderButtonColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = palette.subHeaderButtonColor)
        }
    }
}

// --------------------------------------------------------------- empty state

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    val palette = LocalFfhPalette.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(icon, null, tint = palette.serverRowSubTitleTextColor, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.serverRowTitleTextColor,
            textAlign = TextAlign.Center
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.serverRowSubTitleTextColor,
                textAlign = TextAlign.Center
            )
        }
        if (action != null) {
            Spacer(Modifier.height(22.dp))
            action()
        }
    }
}

// -------------------------------------------------------------------- dialogs

@Composable
fun FfhDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String = "",
    content: @Composable () -> Unit
) {
    val palette = LocalFfhPalette.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.subscriptionInfoBackgroundColor)
                .border(1.dp, palette.hairline, RoundedCornerShape(24.dp))
                .padding(22.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.serverRowTitleTextColor)
            Spacer(Modifier.height(16.dp))
            content()
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                if (dismissText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            dismissText,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.serverRowSubTitleTextColor
                        )
                    }
                }
                if (confirmText != null && onConfirm != null) {
                    FfhButton(text = confirmText, onClick = onConfirm, modifier = Modifier.height(44.dp))
                }
            }
        }
    }
}

@Composable
fun ScrollUpHint(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    val palette = LocalFfhPalette.current
    Box(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(palette.subscriptionInfoBackgroundColor)
            .border(1.dp, palette.hairline, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.KeyboardArrowUp,
            null,
            tint = palette.additionalOptionsButtonColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
