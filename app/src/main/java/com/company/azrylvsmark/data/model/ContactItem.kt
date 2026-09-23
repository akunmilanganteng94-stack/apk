package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class ContactItem(
    val contactUid: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val photoUrl: String = "",
    val phoneHash: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isMutual: Boolean = false,
    val online: Boolean = false,
    val lastSeen: Long = 0L
)
