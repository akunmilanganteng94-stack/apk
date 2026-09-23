package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class UserProfile(
    val uid: String = "",
    val phoneHash: String = "",
    val phoneNumber: String = "", // Kept private, visible only to self in profile
    val displayName: String = "",
    val username: String = "",
    val bio: String = "Hey there! I am using AZMASSANGE.",
    val photoUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSeen: Long = System.currentTimeMillis(),
    val online: Boolean = false,
    val fcmToken: String = ""
)
