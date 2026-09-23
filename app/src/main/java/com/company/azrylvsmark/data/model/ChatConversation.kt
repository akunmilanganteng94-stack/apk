package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class ChatConversation(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val otherUserUid: String = "",
    val otherUserName: String = "",
    val otherUserPhoto: String = "",
    val otherUserOnline: Boolean = false,
    val otherUserLastSeen: Long = 0L,
    val lastMessage: String = "",
    val lastMessageType: String = "text",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Long = 0L,
    val unreadCount: Int = 0,
    val typingUserIds: List<String> = emptyList()
)
