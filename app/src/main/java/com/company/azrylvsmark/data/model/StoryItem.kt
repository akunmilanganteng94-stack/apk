package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class StoryItem(
    val storyId: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerPhoto: String = "",
    val mediaUrl: String = "",
    val storagePath: String = "",
    val type: String = TYPE_IMAGE, // "image", "text", "video"
    val caption: String = "",
    val backgroundColor: String = "#101522",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86_400_000L, // 24 hours
    val viewersCount: Int = 0,
    val isViewedByMe: Boolean = false
) {
    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_TEXT = "text"
        const val TYPE_VIDEO = "video"
    }
}

@Keep
data class StoryViewer(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val viewedAt: Long = System.currentTimeMillis()
)
