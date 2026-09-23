package com.company.azrylvsmark.data.model

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val enterToSend: Boolean = true,
    val mediaAutoDownload: Boolean = true,
    val messageNotifications: Boolean = true,
    val callNotifications: Boolean = true,
    val storyNotifications: Boolean = true
)

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}
