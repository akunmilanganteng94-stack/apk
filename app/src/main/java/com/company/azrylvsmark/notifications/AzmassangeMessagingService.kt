package com.company.azrylvsmark.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.company.azrylvsmark.MainActivity
import com.company.azrylvsmark.R
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AzmassangeMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseService.currentUid ?: return
        FirebaseService.firestore.collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "AZMASSANGE"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: "New notification"
        val type = remoteMessage.data["type"] ?: "message"
        val senderId = remoteMessage.data["senderId"] ?: ""
        val chatId = remoteMessage.data["chatId"] ?: ""
        val callId = remoteMessage.data["callId"] ?: ""

        showNotification(title, body, type, senderId, chatId, callId)
    }

    private fun showNotification(
        title: String,
        body: String,
        type: String,
        senderId: String,
        chatId: String,
        callId: String
    ) {
        val channelId = when (type) {
            "call_voice", "call_video" -> CHANNEL_CALLS
            "story" -> CHANNEL_STORIES
            else -> CHANNEL_MESSAGES
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when (type) {
                "call_voice", "call_video" -> "Calls"
                "story" -> "Stories"
                else -> "Messages"
            }
            val importance = if (type.startsWith("call")) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "AZMASSANGE $name notifications"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("notification_type", type)
            putExtra("sender_id", senderId)
            putExtra("chat_id", chatId)
            putExtra("call_id", callId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(
            if (type.startsWith("call")) RingtoneManager.TYPE_RINGTONE else RingtoneManager.TYPE_NOTIFICATION
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
            .setPriority(if (type.startsWith("call")) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)

        val notificationId = (chatId.hashCode() + callId.hashCode() + System.currentTimeMillis().toInt()).coerceAtLeast(1)
        notificationManager.notify(notificationId, builder.build())
    }

    companion object {
        const val CHANNEL_MESSAGES = "azmassange_messages"
        const val CHANNEL_CALLS = "azmassange_calls"
        const val CHANNEL_STORIES = "azmassange_stories"
    }
}
