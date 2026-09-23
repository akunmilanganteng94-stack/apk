package com.company.azrylvsmark

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.company.azrylvsmark.notifications.AzmassangeMessagingService

class AzmassangeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val messagesChannel = NotificationChannel(
                AzmassangeMessagingService.CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "AZMASSANGE chat message notifications"
                enableVibration(true)
            }

            val callsChannel = NotificationChannel(
                AzmassangeMessagingService.CHANNEL_CALLS,
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "AZMASSANGE incoming voice and video calls"
                enableVibration(true)
            }

            val storiesChannel = NotificationChannel(
                AzmassangeMessagingService.CHANNEL_STORIES,
                "Stories",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AZMASSANGE story updates from contacts"
            }

            notificationManager?.createNotificationChannels(
                listOf(messagesChannel, callsChannel, storiesChannel)
            )
        }
    }
}
