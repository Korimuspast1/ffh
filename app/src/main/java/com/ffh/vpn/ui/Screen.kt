package com.ffh.vpn.ui

/** Navigation destinations of the client. */
sealed class Screen {
    data object Home : Screen()
    data object Subscriptions : Screen()
    data object Add : Screen()
    data class Info(val subscriptionId: String) : Screen()
    data object SettingsRoot : Screen()
    data object Appearance : Screen()
    data object Theme : Screen()
    data object Connection : Screen()
    data object Routing : Screen()
    data object Apps : Screen()
    data object Dns : Screen()
    data object SubscriptionsSettings : Screen()
    data object Core : Screen()
    data object Logs : Screen()
    data object Backup : Screen()
    data object About : Screen()
}
