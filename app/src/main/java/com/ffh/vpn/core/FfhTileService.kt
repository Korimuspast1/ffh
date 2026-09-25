package com.ffh.vpn.core

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.ffh.vpn.R
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.ui.MainActivity
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Asks the system to put the connect tile into the quick settings shade. */
object QuickTile {

    fun request(context: Context, added: String, already: String, manual: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            toast(context, manual)
            return
        }
        val manager = context.getSystemService(StatusBarManager::class.java)
        if (manager == null) {
            toast(context, manual)
            return
        }
        val executor = Executor { command -> Handler(Looper.getMainLooper()).post(command) }
        runCatching {
            manager.requestAddTileService(
                ComponentName(context, FfhTileService::class.java),
                context.getString(R.string.tile_label),
                Icon.createWithResource(context, R.drawable.ic_tile_power),
                executor,
                Consumer { result ->
                    val text = when (result) {
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> added
                        StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> already
                        else -> manual
                    }
                    toast(context, text)
                }
            )
        }.onFailure {
            LogStore.append("ui", "tile request failed: ${it.message}")
            toast(context, manual)
        }
    }

    private fun toast(context: Context, text: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show()
        }
    }
}

/** Connect / disconnect straight from the quick settings shade. */
class FfhTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var watcher: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        watcher?.cancel()
        watcher = scope.launch {
            VpnStateHolder.state.collect { updateTile(it.status) }
        }
    }

    override fun onStopListening() {
        watcher?.cancel()
        watcher = null
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        val state = VpnStateHolder.state.value
        when (state.status) {
            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> {
                VpnController.disconnect(this)
                updateTile(VpnStatus.DISCONNECTED)
            }

            else -> scope.launch {
                when (val action = VpnController.connect(this@FfhTileService)) {
                    is VpnController.Action.NeedPermission -> launchIntent(action.intent)
                    is VpnController.Action.Failed -> {
                        LogStore.append("ui", "tile: ${action.message}")
                        if (action.message.contains("no server", true)) {
                            launchIntent(Intent(this@FfhTileService, MainActivity::class.java))
                        }
                    }

                    VpnController.Action.Started -> Unit
                }
            }
        }
    }

    private fun launchIntent(intent: Intent) {
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        intent.addFlags(flags)
        if (isLocked) {
            unlockAndRun { open(intent) }
        } else {
            open(intent)
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(intent: Intent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pending = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pending)
            } else {
                startActivityAndCollapse(intent)
            }
        }.onFailure {
            runCatching { startActivity(intent) }
                .onFailure { error -> LogStore.append("ui", "tile: ${error.message}") }
        }
    }

    private fun updateTile(status: VpnStatus) {
        val tile = qsTile ?: return
        tile.state = when (status) {
            VpnStatus.CONNECTED -> Tile.STATE_ACTIVE
            VpnStatus.CONNECTING, VpnStatus.DISCONNECTING -> Tile.STATE_UNAVAILABLE
            else -> Tile.STATE_INACTIVE
        }
        val name = VpnStateHolder.state.value.serverName
        tile.label = if (status == VpnStatus.CONNECTED && !name.isNullOrBlank()) {
            name
        } else {
            getString(R.string.tile_label)
        }
        tile.updateTile()
    }
}
