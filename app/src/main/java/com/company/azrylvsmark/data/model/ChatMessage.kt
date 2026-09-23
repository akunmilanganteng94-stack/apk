package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class ChatMessage(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val type: String = TYPE_TEXT, // "text", "image", "voice"
    val text: String = "",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_SENT, // "sent", "delivered", "read"
    val replyToId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val voiceDurationSeconds: Int = 0,
    val isDeleted: Boolean = false
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_VOICE = "voice"

        const val STATUS_SENT = "sent"
        const val STATUS_DELIVERED = "delivered"
        const val STATUS_READ = "read"
    }
}
