package com.ffh.vpn.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restores the tunnel after a reboot when "connect on boot" is enabled.
 * The real work happens in [FfhVpnService], this receiver only forwards.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        FfhVpnService.handleSystemEvent(context)
    }
}
