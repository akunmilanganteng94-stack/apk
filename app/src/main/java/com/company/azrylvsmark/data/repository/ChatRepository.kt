package com.company.azrylvsmark.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.company.azrylvsmark.data.model.ChatConversation
import com.company.azrylvsmark.data.model.ChatMessage
import com.company.azrylvsmark.utils.ImageCompressor
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File

class ChatRepository(private val context: Context) {
    private val db = FirebaseService.firestore
    private val storage = FirebaseService.storage
    private val auth = FirebaseService.auth

    fun getChatId(otherUid: String): String {
        val myUid = auth.currentUser?.uid ?: ""
        return if (myUid < otherUid) "${myUid}_${otherUid}" else "${otherUid}_${myUid}"
    }

    fun observeConversations(): Flow<List<ChatConversation>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("chats")
            .whereArrayContains("participants", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Conversations listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val conversations = snapshot.documents.mapNotNull { doc ->
                        val participants = (doc.get("participants") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        val otherUid = participants.firstOrNull { it != currentUid } ?: ""
                        val lastMessage = doc.getString("lastMessage") ?: ""
                        val lastMessageType = doc.getString("lastMessageType") ?: "text"
                        val lastMessageSenderId = doc.getString("lastMessageSenderId") ?: ""
                        val lastMessageAt = doc.getLong("lastMessageAt") ?: 0L
                        val unreadCount = (doc.getLong("unreadCount_$currentUid") ?: 0L).toInt()
                        val typingList = (doc.get("typing") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                        val otherName = doc.getString("name_$otherUid") ?: "Azmassange User"
                        val otherPhoto = doc.getString("photo_$otherUid") ?: ""
                        val otherOnline = doc.getBoolean("online_$otherUid") ?: false
                        val otherLastSeen = doc.getLong("lastSeen_$otherUid") ?: 0L

                        ChatConversation(
                            chatId = doc.id,
                            participants = participants,
                            otherUserUid = otherUid,
                            otherUserName = otherName,
                            otherUserPhoto = otherPhoto,
                            otherUserOnline = otherOnline,
                            otherUserLastSeen = otherLastSeen,
                            lastMessage = lastMessage,
                            lastMessageType = lastMessageType,
                            lastMessageSenderId = lastMessageSenderId,
                            lastMessageAt = lastMessageAt,
                            unreadCount = unreadCount,
                            typingUserIds = typingList
                        )
                    }.sortedByDescending { it.lastMessageAt }
                    trySend(conversations)
                }
            }

        awaitClose { registration.remove() }
    }

    fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Messages listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ChatMessage::class.java)
                    }
                    trySend(messages)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun sendTextMessage(
        chatId: String,
        receiverId: String,
        text: String,
        replyToMessage: ChatMessage? = null
    ): Result<Unit> {
        val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val messageId = db.collection("chats").document(chatId).collection("messages").document().id
        val message = ChatMessage(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            type = ChatMessage.TYPE_TEXT,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            status = ChatMessage.STATUS_SENT,
            replyToId = replyToMessage?.messageId ?: "",
            replyToText = replyToMessage?.text ?: "",
            replyToSender = replyToMessage?.senderId ?: ""
        )

        return try {
            val batch = db.batch()
            val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
            batch.set(msgRef, message)

            val chatRef = db.collection("chats").document(chatId)
            val chatUpdates = hashMapOf<String, Any>(
                "participants" to listOf(senderId, receiverId),
                "lastMessage" to text.trim(),
                "lastMessageType" to ChatMessage.TYPE_TEXT,
                "lastMessageSenderId" to senderId,
                "lastMessageAt" to message.timestamp,
                "unreadCount_$receiverId" to FieldValue.increment(1)
            )
            batch.set(chatRef, chatUpdates, SetOptions.merge())
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending text message", e)
            Result.failure(e)
        }
    }

    suspend fun sendImageMessage(
        chatId: String,
        receiverId: String,
        imageUri: Uri,
        caption: String = "",
        replyToMessage: ChatMessage? = null
    ): Result<Unit> {
        val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val messageId = db.collection("chats").document(chatId).collection("messages").document().id
        return try {
            val compressedBytes = ImageCompressor.compressImageUri(context, imageUri)
            val storagePath = "chat/$chatId/images/$messageId.jpg"
            val ref = storage.reference.child(storagePath)
            ref.putBytes(compressedBytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            val message = ChatMessage(
                messageId = messageId,
                chatId = chatId,
                senderId = senderId,
                receiverId = receiverId,
                type = ChatMessage.TYPE_IMAGE,
                text = caption.trim(),
                mediaUrl = downloadUrl,
                storagePath = storagePath,
                timestamp = System.currentTimeMillis(),
                status = ChatMessage.STATUS_SENT,
                replyToId = replyToMessage?.messageId ?: "",
                replyToText = replyToMessage?.text ?: "",
                replyToSender = replyToMessage?.senderId ?: ""
            )

            val batch = db.batch()
            val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
            batch.set(msgRef, message)

            val chatRef = db.collection("chats").document(chatId)
            val chatUpdates = hashMapOf<String, Any>(
                "participants" to listOf(senderId, receiverId),
                "lastMessage" to if (caption.isNotBlank()) "📷 $caption" else "📷 Photo",
                "lastMessageType" to ChatMessage.TYPE_IMAGE,
                "lastMessageSenderId" to senderId,
                "lastMessageAt" to message.timestamp,
                "unreadCount_$receiverId" to FieldValue.increment(1)
            )
            batch.set(chatRef, chatUpdates, SetOptions.merge())
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending image message", e)
            Result.failure(e)
        }
    }

    suspend fun sendVoiceMessage(
        chatId: String,
        receiverId: String,
        audioFile: File,
        durationSeconds: Int
    ): Result<Unit> {
        val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val messageId = db.collection("chats").document(chatId).collection("messages").document().id
        return try {
            val storagePath = "voice/$chatId/$messageId.m4a"
            val ref = storage.reference.child(storagePath)
            ref.putFile(Uri.fromFile(audioFile)).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            val message = ChatMessage(
                messageId = messageId,
                chatId = chatId,
                senderId = senderId,
                receiverId = receiverId,
                type = ChatMessage.TYPE_VOICE,
                text = "Voice message",
                mediaUrl = downloadUrl,
                storagePath = storagePath,
                voiceDurationSeconds = durationSeconds,
                timestamp = System.currentTimeMillis(),
                status = ChatMessage.STATUS_SENT
            )

            val batch = db.batch()
            val msgRef = db.collection("chats").document(chatId).collection("messages").document(messageId)
            batch.set(msgRef, message)

            val chatRef = db.collection("chats").document(chatId)
            val chatUpdates = hashMapOf<String, Any>(
                "participants" to listOf(senderId, receiverId),
                "lastMessage" to "🎤 Voice note (${durationSeconds}s)",
                "lastMessageType" to ChatMessage.TYPE_VOICE,
                "lastMessageSenderId" to senderId,
                "lastMessageAt" to message.timestamp,
                "unreadCount_$receiverId" to FieldValue.increment(1)
            )
            batch.set(chatRef, chatUpdates, SetOptions.merge())
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending voice message", e)
            Result.failure(e)
        }
    }

    suspend fun markMessagesAsRead(chatId: String) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            db.collection("chats").document(chatId)
                .set(mapOf("unreadCount_$currentUid" to 0), SetOptions.merge())

            val unreadDocs = db.collection("chats").document(chatId).collection("messages")
                .whereEqualTo("receiverId", currentUid)
                .whereNotEqualTo("status", ChatMessage.STATUS_READ)
                .get().await()

            if (!unreadDocs.isEmpty) {
                val batch = db.batch()
                for (doc in unreadDocs) {
                    batch.update(doc.reference, "status", ChatMessage.STATUS_READ)
                }
                batch.commit().await()
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error marking messages as read", e)
        }
    }

    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> {
        return try {
            db.collection("chats").document(chatId).collection("messages").document(messageId)
                .update("isDeleted", true, "text", "Pesan ini telah dihapus", "mediaUrl", "")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setTyping(chatId: String, isTyping: Boolean) {
        val currentUid = auth.currentUser?.uid ?: return
        val chatRef = db.collection("chats").document(chatId)
        if (isTyping) {
            chatRef.update("typing", FieldValue.arrayUnion(currentUid))
        } else {
            chatRef.update("typing", FieldValue.arrayRemove(currentUid))
        }
    }
}
