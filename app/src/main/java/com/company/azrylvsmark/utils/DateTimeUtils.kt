package com.company.azrylvsmark.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    fun formatMessageTime(timestamp: Long): String {
        if (timestamp <= 0L) return ""
        val now = Calendar.getInstance()
        val msgTime = Calendar.getInstance().apply { timeInMillis = timestamp }
        return if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR)
        ) {
            timeFormat.format(Date(timestamp))
        } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            "Yesterday"
        } else if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) < 7
        ) {
            dayFormat.format(Date(timestamp))
        } else {
            dateFormat.format(Date(timestamp))
        }
    }

    fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%02d:%02d".format(mins, secs)
    }

    fun formatLastSeen(lastSeenTimestamp: Long, online: Boolean): String {
        if (online) return "Online"
        if (lastSeenTimestamp <= 0L) return "Offline"
        val diffMillis = System.currentTimeMillis() - lastSeenTimestamp
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        return when {
            diffMinutes < 1 -> "Last seen just now"
            diffMinutes < 60 -> "Last seen $diffMinutes min ago"
            diffHours < 24 -> "Last seen $diffHours hr ago"
            else -> "Last seen ${formatMessageTime(lastSeenTimestamp)}"
        }
    }

    fun formatStoryTime(timestamp: Long): String {
        val diffMillis = System.currentTimeMillis() - timestamp
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val diffHours = TimeUnit.MILLISECONDS.toHours(diffMillis)
        return when {
            diffMinutes < 1 -> "Just now"
            diffMinutes < 60 -> "${diffMinutes}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            else -> "Yesterday"
        }
    }
}
