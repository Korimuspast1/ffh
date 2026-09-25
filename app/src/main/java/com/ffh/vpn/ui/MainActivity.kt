package com.ffh.vpn.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.ffh.vpn.core.VpnController
import com.ffh.vpn.core.XrayProcess
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.i18n.ProvideStrings
import com.ffh.vpn.ui.theme.AppThemeSpec
import com.ffh.vpn.ui.theme.FfhTheme
import com.ffh.vpn.ui.theme.ThemePresets
import com.ffh.vpn.ui.theme.parseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch { VpnController.connect(this@MainActivity) }
        } else {
            LogStore.append("vpn", "permission denied")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase, AppRepository.settings().language))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        XrayProcess.prepareAssets(this)
        requestNotificationPermission()

        setContent {
            val state by AppRepository.state.collectAsState()
            val spec = remember(state.settings.customThemeJson, state.settings.themePresetId) {
                resolveTheme(state.settings.customThemeJson, state.settings.themePresetId)
            }

            FfhTheme(spec = spec, fontScale = state.settings.fontScale) {
                ProvideStrings(state.settings.language) {
                    val context = LocalContext.current
                    AppNav(
                        onVpnPermission = { intent -> vpnPermissionLauncher.launch(intent) },
                        onCopyText = { copyText(context, it) },
                        onShareText = { shareText(context, it) },
                        onOpenUrl = { url -> openUrl(context, url) },
                        onLanguageChanged = { recreate() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun resolveTheme(customJson: String, presetId: String): AppThemeSpec {
        if (customJson.isNotBlank()) {
            parseTheme(customJson)?.let { return it.normalized() }
        }
        return ThemePresets.all.firstOrNull { it.id == presetId }?.spec ?: AppThemeSpec.DEFAULT
    }

    companion object {
        fun copyText(context: Context, text: String) {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            manager?.setPrimaryClip(ClipData.newPlainText("ffh", text))
        }

        fun shareText(context: Context, text: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { LogStore.append("ui", "share failed: ${it.message}") }
        }

        fun openUrl(context: Context, url: String) {
            val normalized = if (url.startsWith("http")) url else "https://$url"
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, normalized.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.onFailure { LogStore.append("ui", "browser failed: ${it.message}") }
        }
    }
}
