package com.ffh.vpn.ui

import com.ffh.vpn.data.model.Subscription
import com.ffh.vpn.data.model.TrafficInfo
import com.ffh.vpn.i18n.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Format {

    fun bytes(value: Long): String {
        if (value <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        var size = value.toDouble()
        var unit = 0
        while (size >= 1024.0 && unit < units.lastIndex) {
            size /= 1024.0
            unit++
        }
        val digits = when {
            unit == 0 -> 0
            size < 10 -> 2
            size < 100 -> 1
            else -> 0
        }
        return String.format(Locale.US, "%.${digits}f %s", size, units[unit])
    }

    /** "1.20 GB of 100 GB · 98.8 GB left" */
    fun traffic(info: TrafficInfo?, strings: Strings): String? {
        if (info == null || !info.hasTraffic) return null
        return buildString {
            append(bytes(info.used))
            append(" ")
            append(strings.of)
            append(" ")
            append(bytes(info.total))
            append(" · ")
            append(bytes(info.left))
            append(" ")
            append(strings.trafficLeft)
        }
    }

    fun duration(millis: Long): String {
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis)
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    fun dateTime(epochMillis: Long): String {
        if (epochMillis <= 0) return "—"
        return SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epochMillis))
    }

    fun date(epochSeconds: Long): String {
        if (epochSeconds <= 0) return "—"
        return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(epochSeconds * 1000))
    }

    fun relativeTime(epochMillis: Long): String {
        if (epochMillis <= 0) return "—"
        val diff = System.currentTimeMillis() - epochMillis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(abs(diff))
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "$minutes min"
            minutes < 60 * 24 -> "${minutes / 60} h"
            minutes < 60 * 24 * 30 -> "${minutes / (60 * 24)} d"
            else -> date(epochMillis / 1000)
        }
    }

    /** "12 d left" / "5 h left" / "expired" */
    fun expire(epochSeconds: Long, strings: Strings): String? {
        if (epochSeconds <= 0) return null
        val millisLeft = epochSeconds * 1000 - System.currentTimeMillis()
        if (millisLeft <= 0) return strings.expired
        val days = TimeUnit.MILLISECONDS.toDays(millisLeft)
        val hours = TimeUnit.MILLISECONDS.toHours(millisLeft)
        return when {
            hours < 24 -> "$hours ${strings.hoursLeft}"
            else -> "$days ${strings.daysLeft}"
        }
    }

    fun subscriptionSummary(sub: Subscription, strings: Strings): String = buildString {
        append(sub.servers.size)
        append(" ")
        append(strings.servers)
        sub.traffic?.let { info ->
            if (info.hasTraffic) {
                append(" · ")
                append(bytes(info.left))
                append(" ")
                append(strings.trafficLeft)
            }
        }
        sub.lastError?.let {
            append(" · ")
            append(it)
        }
    }
}
