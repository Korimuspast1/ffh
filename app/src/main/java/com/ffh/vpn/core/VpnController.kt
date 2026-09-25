package com.ffh.vpn.core

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.LogStore

object VpnController {

    sealed interface Action {
        data object Started : Action
        data class NeedPermission(val intent: Intent) : Action
        data class Failed(val message: String) : Action
    }

    suspend fun connect(context: Context, serverId: String? = null): Action {
        val server = if (serverId != null) AppRepository.server(serverId) else AppRepository.selectedServer
            ?: return Action.Failed("no server selected")

        if (serverId != null) {
            AppRepository.setSelection(server.subscriptionId, server.id)
        }

        val prepare = VpnService.prepare(context)
        if (prepare != null) return Action.NeedPermission(prepare)

        val intent = Intent(context, FfhVpnService::class.java).setAction(FfhVpnService.ACTION_CONNECT)
        return runCatching {
            ContextCompat.startForegroundService(context, intent)
            Action.Started
        }.getOrElse {
            LogStore.append("vpn", "start failed: ${it.message}")
            Action.Failed(it.message ?: "start failed")
        }
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, FfhVpnService::class.java).setAction(FfhVpnService.ACTION_DISCONNECT)
        runCatching { context.startService(intent) }
    }

    suspend fun toggle(context: Context, serverId: String? = null): Action {
        return when (VpnStateHolder.state.value.status) {
            VpnStatus.CONNECTED, VpnStatus.CONNECTING -> {
                disconnect(context)
                Action.Started
            }

            else -> connect(context, serverId)
        }
    }
}
