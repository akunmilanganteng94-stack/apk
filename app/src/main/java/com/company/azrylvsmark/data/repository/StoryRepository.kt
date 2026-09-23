package com.company.azrylvsmark.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.company.azrylvsmark.data.model.StoryItem
import com.company.azrylvsmark.data.model.StoryViewer
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.utils.ImageCompressor
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class StoryRepository(
    private val context: Context,
    private val contactRepository: ContactRepository
) {
    private val db = FirebaseService.firestore
    private val storage = FirebaseService.storage
    private val auth = FirebaseService.auth

    suspend fun createStory(
        type: String,
        mediaUri: Uri?,
        caption: String,
        backgroundColor: String,
        currentUser: UserProfile
    ): Result<StoryItem> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val storyId = db.collection("stories").document().id
        return try {
            var mediaUrl = ""
            var storagePath = ""
            if (mediaUri != null && type == StoryItem.TYPE_IMAGE) {
                val compressedBytes = ImageCompressor.compressImageUri(context, mediaUri)
                storagePath = "stories/$uid/$storyId.jpg"
                val ref = storage.reference.child(storagePath)
                ref.putBytes(compressedBytes).await()
                mediaUrl = ref.downloadUrl.await().toString()
            }

            val now = System.currentTimeMillis()
            val expiresAt = now + (24 * 60 * 60 * 1000L) // 24 hours

            val story = StoryItem(
                storyId = storyId,
                ownerId = uid,
                ownerName = currentUser.displayName,
                ownerPhoto = currentUser.photoUrl,
                mediaUrl = mediaUrl,
                storagePath = storagePath,
                type = type,
                caption = caption.trim(),
                backgroundColor = backgroundColor,
                createdAt = now,
                expiresAt = expiresAt,
                viewersCount = 0,
                isViewedByMe = true
            )

            db.collection("stories").document(storyId).set(story).await()
            Result.success(story)
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error creating story", e)
            Result.failure(e)
        }
    }

    /**
     * Observes active stories from:
     * 1. The user themselves
     * 2. Only MUTUAL contacts (both parties have saved each other's contact)
     */
    fun observeStories(myContacts: List<com.company.azrylvsmark.data.model.ContactItem>): Flow<List<StoryItem>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val mutualContactUids = myContacts.filter { it.isMutual }.map { it.contactUid }
        val allowedUids = (mutualContactUids + currentUid).distinct()

        val now = System.currentTimeMillis()
        val registration = db.collection("stories")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StoryRepository", "Stories listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val stories = snapshot.documents.mapNotNull { doc ->
                        val story = doc.toObject(StoryItem::class.java)
                        if (story != null && allowedUids.contains(story.ownerId)) {
                            story
                        } else {
                            null
                        }
                    }
                    trySend(stories)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun getStoryFeed(myContacts: List<com.company.azrylvsmark.data.model.ContactItem>): List<StoryItem> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        val mutualContactUids = myContacts.filter { it.isMutual }.map { it.contactUid }
        val allowedUids = (mutualContactUids + currentUid).distinct()
        val now = System.currentTimeMillis()
        return try {
            val snapshot = db.collection("stories")
                .whereGreaterThan("expiresAt", now)
                .orderBy("expiresAt")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                val story = doc.toObject(StoryItem::class.java)
                if (story != null && allowedUids.contains(story.ownerId)) story else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun recordStoryView(storyId: String, user: UserProfile) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            val viewRef = db.collection("stories").document(storyId).collection("views").document(currentUid)
            val doc = viewRef.get().await()
            if (!doc.exists()) {
                val viewer = StoryViewer(
                    uid = currentUid,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl,
                    viewedAt = System.currentTimeMillis()
                )
                viewRef.set(viewer).await()
                db.collection("stories").document(storyId)
                    .update("viewersCount", FieldValue.increment(1))
                    .await()
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error recording story view", e)
        }
    }

    fun observeStoryViewers(storyId: String): Flow<List<StoryViewer>> = callbackFlow {
        val registration = db.collection("stories")
            .document(storyId)
            .collection("views")
            .orderBy("viewedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(StoryViewer::class.java) }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }
}
