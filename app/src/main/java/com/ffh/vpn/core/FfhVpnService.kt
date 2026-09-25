package com.ffh.vpn.core

import android.app.Notification
import android.app.NotificationManager
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
import com.ffh.vpn.core.xray.HostResolver
import com.ffh.vpn.core.xray.OutboundDial
import com.ffh.vpn.core.xray.XrayConfigBuilder
import com.ffh.vpn.data.AppRepository
import com.ffh.vpn.data.LogStore
import com.ffh.vpn.data.model.ServerProfile
import com.ffh.vpn.ui.MainActivity
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class FfhVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null
    private val generation = AtomicInteger(0)
    private val stopping = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        running = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> stopTunnel()
            ACTION_CONNECT -> startTunnel()
            else -> {
                if (VpnStateHolder.state.value.status != VpnStatus.CONNECTED) {
                    runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (running === this) running = null
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
        val gen = generation.incrementAndGet()
        stopping.set(false)
        // Must happen before any DNS or process work: a foreground service that
        // does not call startForeground within a few seconds is killed.
        ensureForeground(getString(R.string.notification_connecting))

        val server = AppRepository.selectedServer
        if (server == null) {
            fail(gen, "no server selected")
            return
        }
        if (!server.isSupported) {
            fail(gen, "protocol is not supported by Xray")
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

        Thread {
            if (generation.get() != gen) return@Thread
            val prepared = prepareServer(server)
            val config = runCatching { XrayConfigBuilder.build(prepared, AppRepository.settings()) }
                .getOrElse {
                    LogStore.append("vpn", "config error: ${it.message}")
                    fail(gen, "config error")
                    return@Thread
                }
            if (generation.get() != gen) return@Thread

            val fd = establish()
            if (fd == null) {
                fail(gen, "vpn interface failed")
                return@Thread
            }
            if (generation.get() != gen) {
                closeTunnel()
                return@Thread
            }

            XrayProcess.onUnexpectedExit = { code ->
                if (!stopping.get() && generation.get() == gen) {
                    fail(gen, "core exited ($code)")
                }
            }
            if (!XrayProcess.start(this, config, fd)) {
                fail(gen, "core failed to start")
                return@Thread
            }

            // The core can die immediately when the TUN fd is rejected. Don't
            // tell the user they are connected until it has stayed up.
            Thread.sleep(700)
            if (generation.get() != gen) return@Thread
            if (!XrayProcess.isRunning) {
                fail(gen, "core exited during startup")
                return@Thread
            }

            VpnStateHolder.update {
                it.copy(status = VpnStatus.CONNECTED, startedAt = System.currentTimeMillis(), message = null)
            }
            notify(prepared.displayName(), getString(R.string.notification_connected))
            LogStore.append("vpn", "connected to ${prepared.displayName()}")
        }.also { it.name = "ffh-vpn-start"; it.isDaemon = true; it.start() }
    }

    /** Dials the server by IP so the core does not need DNS to open the uplink. */
    private fun prepareServer(server: ServerProfile): ServerProfile {
        val outbound = server.outbound ?: return server
        val host = OutboundDial.hostOf(outbound) ?: return server
        if (OutboundDial.isIp(host)) return server
        val ip = HostResolver.resolveV4(host)
        if (ip == null) {
            LogStore.append("vpn", "could not resolve $host, core will use the system resolver")
            return server
        }
        LogStore.append("vpn", "resolved $host -> $ip")
        return server.copy(outbound = OutboundDial.rewrite(outbound, ip))
    }

    private fun fail(gen: Int, message: String) {
        if (!generation.compareAndSet(gen, gen + 1)) return
        LogStore.append("vpn", message)
        XrayProcess.onUnexpectedExit = null
        XrayProcess.stop()
        closeTunnel()
        VpnStateHolder.update {
            it.copy(status = VpnStatus.ERROR, message = message)
        }
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        stopSelf()
    }

    // ------------------------------------------------------------------- tun

    private fun establish(): Int? {
        val settings = AppRepository.settings()
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(settings.tunMtu)
            .addAddress(TUN_IPV4, TUN_PREFIX)
            .addRoute("0.0.0.0", 0)

        // Always capture IPv6. Without the route, a dual-stack phone sends
        // IPv6 around the tunnel and apps that prefer it look completely dead.
        runCatching {
            builder.addAddress(TUN_IPV6, 64)
            builder.addRoute("::", 0)
        }

        val dns = settings.dnsServers.map { it.trim() }.filter { it.isNotEmpty() && !it.contains("://") && !it.contains("/") }
        if (dns.isEmpty()) {
            runCatching { builder.addDnsServer("1.1.1.1") }
        } else {
            dns.forEach { runCatching { builder.addDnsServer(it) } }
        }

        // The core is a child of this app. If the app's own sockets enter the
        // tunnel they loop forever and nothing on the phone can connect.
        // include-mode already excludes every package that is not listed, and
        // Android rejects mixing allow and disallow lists. An empty include
        // list would capture the app too, so it is treated as "everyone else".
        val included = settings.perAppPackages.filter { it != packageName }
        val includeMode = settings.perAppMode == "include" && included.isNotEmpty()
        if (!includeMode) {
            runCatching { builder.addDisallowedApplication(packageName) }
                .onFailure { LogStore.append("vpn", "cannot exclude the app: ${it.message}") }
        }
        if (includeMode) {
            included.forEach { runCatching { builder.addAllowedApplication(it) } }
        } else if (settings.perAppMode == "exclude") {
            included.forEach { runCatching { builder.addDisallowedApplication(it) } }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { builder.setMetered(false) }
        }

        val pfd = runCatching { builder.establish() }.getOrElse {
            LogStore.append("vpn", "establish failed: ${it.message}")
            null
        } ?: return null
        tunnel = pfd

        // Inherited by fork(); CLOEXEC would drop it on exec inside the child
        // only if the launcher forgot to dup it. Cleared here as well.
        runCatching {
            val flags = Os.fcntlInt(pfd.fileDescriptor, OsConstants.F_GETFD, 0)
            if (flags and OsConstants.FD_CLOEXEC != 0) {
                Os.fcntlInt(pfd.fileDescriptor, OsConstants.F_SETFD, flags and OsConstants.FD_CLOEXEC.inv())
            }
        }.onFailure { LogStore.append("vpn", "fcntl: ${it.message}") }

        return pfd.fd
    }

    // ------------------------------------------------------------------- stop

    fun stopTunnel() {
        generation.incrementAndGet()
        stopping.set(true)
        XrayProcess.onUnexpectedExit = null
        VpnStateHolder.update { it.copy(status = VpnStatus.DISCONNECTING) }
        XrayProcess.stop()
        closeTunnel()
        runCatching { ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE) }
        VpnStateHolder.update { VpnState(status = VpnStatus.DISCONNECTED) }
        stopSelf()
    }

    private fun closeTunnel() {
        runCatching { tunnel?.close() }
        tunnel = null
    }

    // ----------------------------------------------------------- notification

    private fun ensureForeground(text: String) {
        val name = VpnStateHolder.state.value.serverName ?: getString(R.string.app_name)
        runCatching {
            ServiceCompat.startForeground(
                this,
                Notifications.ID_VPN,
                buildNotification(name, text),
                if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            )
        }.onFailure { LogStore.append("vpn", "foreground failed: ${it.message}") }
    }

    private fun notify(title: String, text: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.notify(Notifications.ID_VPN, buildNotification(title, text)) }
    }

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

        @Volatile
        private var running: FfhVpnService? = null

        /** Keeps a local probe socket out of the tunnel while a VPN is up. */
        fun protect(socket: java.net.Socket): Boolean {
            val service = running ?: return false
            return runCatching { service.protect(socket) }.getOrDefault(false)
        }

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
