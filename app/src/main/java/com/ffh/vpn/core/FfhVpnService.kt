package com.ffh.vpn.core

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ffh.vpn.R
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.core.xray.XrayConfigBuilder
import com.ffh.vpn.ui.MainActivity

class FfhVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> stopTunnel()
            ACTION_CONNECT -> startTunnel()
            else -> Unit
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        LogStore.append("vpn", "vpn permission revoked")
        stopTunnel()
        super.onRevoke()
    }

    // ------------------------------------------------------------------ start

    private fun startTunnel() {
        val server = AppRepository.selectedServer
        if (server == null) {
            VpnStateHolder.update { it.copy(status = VpnStatus.ERROR, message = "no server selected") }
            stopSelf()
            return
        }
        if (!server.isSupported) {
            VpnStateHolder.update {
                it.copy(
                    status = VpnStatus.ERROR,
                    serverId = server.id,
                    serverName = server.displayName(),
                    message = "protocol is not supported by Xray"
                )
            }
            stopSelf()
            return
        }

        VpnStateHolder.update {
            it.copy(
                status = VpnStatus.CONNECTING,
                serverId = server.id,
                serverName = server.displayName(),
                subscriptionId = server.subscriptionId,
                message = null
            )
        }

        val config = runCatching { XrayConfigBuilder.build(server, AppRepository.settings()) }
            .getOrElse {
                VpnStateHolder.update { it.copy(status = VpnStatus.ERROR, message = it.message ?: "config error") }
                LogStore.append("vpn", "config error: ${it.message}")
                stopSelf()
                return
            }

        val fd = establish()
        if (fd == null) {
            VpnStateHolder.update { it.copy(status = VpnStatus.ERROR, message = "vpn interface failed") }
            stopSelf()
            return
        }

        if (!XrayProcess.start(this, config, fd)) {
            VpnStateHolder.update { it.copy(status = VpnStatus.ERROR, message = "core failed to start") }
            runCatching { tunnel?.close() }
            tunnel = null
            stopSelf()
            return
        }

        runCatching {
            ServiceCompat.startForeground(
                this,
                Notifications.ID_VPN,
                buildNotification(server.displayName(), getString(R.string.notification_connecting)),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
        }.onFailure { LogStore.append("vpn", "foreground failed: ${it.message}") }

        VpnStateHolder.update {
            it.copy(
                status = VpnStatus.CONNECTED,
                startedAt = System.currentTimeMillis(),
                message = null
            )
        }
        LogStore.append("vpn", "connected to ${server.displayName()}")
    }

    // ------------------------------------------------------------------- tun

    private fun establish(): Int? {
        val settings = AppRepository.settings()
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(settings.tunMtu)
            .addAddress(TUN_IPV4, TUN_PREFIX)
            .addRoute("0.0.0.0", 0)

        val dns = settings.dnsServers.map { it.trim() }.filter { it.isNotEmpty() && !it.contains("://") }
        (dns.firstOrNull() ?: "1.1.1.1").let { runCatching { builder.addDnsServer(it) } }

        if (settings.enableIpv6) {
            runCatching {
                builder.addAddress(TUN_IPV6, 64)
                builder.addRoute("::", 0)
            }
        }

        if (settings.excludeSelf) {
            runCatching { builder.addDisallowedApplication(packageName) }
        }

        when (settings.perAppMode) {
            "include" -> settings.perAppPackages.filter { it != packageName }.forEach {
                runCatching { builder.addAllowedApplication(it) }
            }

            "exclude" -> settings.perAppPackages.filter { it != packageName }.forEach {
                runCatching { builder.addDisallowedApplication(it) }
            }
        }

        val pfd = runCatching { builder.establish() }.getOrNull() ?: return null
        tunnel = pfd

        // The file descriptor has to survive the exec() of the core process,
        // so FD_CLOEXEC must be cleared before it is handed over.
        runCatching {
            val flags = Os.fcntlInt(pfd.fileDescriptor, OsConstants.F_GETFD)
            if (flags and OsConstants.FD_CLOEXEC != 0) {
                Os.fcntlInt(pfd.fileDescriptor, OsConstants.F_SETFD, flags and OsConstants.FD_CLOEXEC.inv())
            }
        }.onFailure { LogStore.append("vpn", "fcntl: ${it.message}") }

        return pfd.fd
    }

    // ------------------------------------------------------------------- stop

    fun stopTunnel() {
        VpnStateHolder.update { it.copy(status = VpnStatus.DISCONNECTING) }
        XrayProcess.stop()
        runCatching { tunnel?.close() }
        tunnel = null
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        VpnStateHolder.update { VpnState(status = VpnStatus.DISCONNECTED) }
        stopSelf()
    }

    // ----------------------------------------------------------- notification

    private fun buildNotification(title: String, text: String): Notification {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), flags
        )
        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, FfhVpnService::class.java).setAction(ACTION_DISCONNECT), flags
        )

        return NotificationCompat.Builder(this, Notifications.CHANNEL_VPN)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(
                NotificationCompat.Action.Builder(
                    0, getString(R.string.notification_action_disconnect), stopIntent
                ).build()
            )
            .build()
    }

    companion object {
        const val ACTION_CONNECT = "com.ffh.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.ffh.vpn.DISCONNECT"

        private const val TUN_IPV4 = "10.89.15.1"
        private const val TUN_PREFIX = 24
        private const val TUN_IPV6 = "fd6e:6f66:6866::1"

        /** Called by [BootReceiver] after boot / package update. */
        @JvmStatic
        fun handleSystemEvent(context: android.content.Context) {
            val settings = AppRepository.settings()
            if (!settings.connectOnBoot && !settings.autoConnect) return
            val server = AppRepository.selectedServer ?: return
            if (VpnService.prepare(context) != null) return
            val intent = Intent(context, FfhVpnService::class.java).setAction(ACTION_CONNECT)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { LogStore.append("vpn", "autostart failed: ${it.message}") }
        }
    }
}
