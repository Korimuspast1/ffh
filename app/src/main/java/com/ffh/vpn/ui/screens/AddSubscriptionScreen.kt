package com.ffh.vpn.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import com.ffh.vpn.data.AddResult
import com.ffh.vpn.i18n.LocalStrings
import com.ffh.vpn.ui.components.FfhButton
import com.ffh.vpn.ui.components.FfhTopBar
import com.ffh.vpn.ui.components.SecondaryButton
import com.ffh.vpn.ui.theme.FfhBackground
import com.ffh.vpn.ui.theme.LocalFfhPalette
import com.ffh.vpn.ui.theme.looksLikeThemeJson
import kotlinx.coroutines.launch

@Composable
fun AddSubscriptionScreen(
    initialText: String = "",
    onBack: () -> Unit,
    onDone: (String) -> Unit,
    onSubmit: suspend (String) -> AddResult
) {
    val strings = LocalStrings.current
    val palette = LocalFfhPalette.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(initialText) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var themeDetected by remember { mutableStateOf(looksLikeThemeJson(initialText)) }

    FfhBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            FfhTopBar(title = strings.addSubscriptionTitle, onBack = onBack)

            Text(
                text = strings.inputHint,
                style = MaterialTheme.typography.bodySmall,
                color = palette.serverRowSubTitleTextColor,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Spacer(Modifier.height(12.dp))

            SimpleTextField(
                value = text,
                onValueChange = {
                    text = it
                    themeDetected = looksLikeThemeJson(it)
                    message = null
                },
                hint = strings.inputHint,
                multiline = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            if (themeDetected) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = strings.themeDetected,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.subscriptionInfoTextColor
                )
            }

            message?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.serverRowSubTitleTextColor
                )
            }

            Spacer(Modifier.height(18.dp))

            SecondaryButton(
                text = strings.pasteFromClipboard,
                onClick = {
                    val clip = clipboard.getText()?.text ?: ""
                    if (clip.isNotBlank()) {
                        text = clip.trim()
                        themeDetected = looksLikeThemeJson(text)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            FfhButton(
                text = if (busy) strings.updating else strings.add,
                onClick = {
                    if (busy || text.isBlank()) return@FfhButton
                    scope.launch {
                        busy = true
                        val result = onSubmit(text.trim())
                        busy = false
                        when (result) {
                            is AddResult.Added -> onDone(result.subscription.id)
                            is AddResult.ThemeApplied -> {
                                message = strings.themeApplied
                                themeDetected = true
                            }
                            is AddResult.Failed -> message = result.message
                            AddResult.Invalid -> message = strings.invalidInput
                        }
                    }
                },
                enabled = text.isNotBlank() && !busy,
                icon = Icons.Default.Check,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
