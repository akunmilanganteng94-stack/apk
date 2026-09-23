package com.company.azrylvsmark.data.model

import androidx.annotation.Keep

@Keep
data class CallSession(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhoto: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhoto: String = "",
    val type: String = TYPE_VOICE, // "voice", "video"
    val status: String = STATUS_RINGING, // "ringing", "accepted", "rejected", "ended", "missed"
    val startedAt: Long = 0L,
    val endedAt: Long = 0L,
    val durationSeconds: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = true
) {
    val isVideo: Boolean get() = type == TYPE_VIDEO
    val createdAt: Long get() = timestamp

    companion object {
        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"

        const val STATUS_RINGING = "ringing"
        const val STATUS_ACCEPTED = "accepted"
        const val STATUS_CONNECTED = "accepted"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_ENDED = "ended"
        const val STATUS_MISSED = "missed"
    }
}
