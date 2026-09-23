package com.company.azrylvsmark.data.repository

import android.util.Log
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.company.azrylvsmark.data.model.ContactItem
import com.company.azrylvsmark.utils.CryptoUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ContactRepository {
    private val db = FirebaseService.firestore
    private val auth = FirebaseService.auth

    /**
     * Search user by phone number using hashed lookup.
     * Raw phone numbers and entire directory are NEVER queried or listed.
     */
    suspend fun searchUserByPhone(phone: String): Result<ContactItem?> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            val phoneHash = CryptoUtils.hashPhoneNumber(phone)
            val doc = db.collection("phone_directory").document(phoneHash).get().await()
            if (!doc.exists()) {
                Result.success(null) // "Nomor tidak terdaftar di Azmassange."
            } else {
                val targetUid = doc.getString("uid") ?: ""
                if (targetUid == currentUid) {
                    return Result.failure(Exception("Tidak dapat menambahkan nomor sendiri sebagai kontak."))
                }

                // Check if target user has saved current user (mutual)
                val targetSavedMe = db.collection("users")
                    .document(targetUid)
                    .collection("contacts")
                    .document(currentUid)
                    .get().await().exists()

                // Check if already in my contacts
                val myContactDoc = db.collection("users")
                    .document(currentUid)
                    .collection("contacts")
                    .document(targetUid)
                    .get().await()

                val contact = ContactItem(
                    contactUid = targetUid,
                    displayName = doc.getString("displayName") ?: "Azmassange User",
                    username = doc.getString("username") ?: "",
                    bio = doc.getString("bio") ?: "",
                    photoUrl = doc.getString("photoUrl") ?: "",
                    phoneHash = phoneHash,
                    isMutual = targetSavedMe,
                    isBlocked = myContactDoc.getBoolean("isBlocked") ?: false
                )
                Result.success(contact)
            }
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error searching user by phone", e)
            Result.failure(e)
        }
    }

    suspend fun saveContact(contact: ContactItem): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            // Check mutual status
            val targetSavedMe = db.collection("users")
                .document(contact.contactUid)
                .collection("contacts")
                .document(currentUid)
                .get().await().exists()

            val contactData = hashMapOf(
                "contactUid" to contact.contactUid,
                "displayName" to contact.displayName,
                "username" to contact.username,
                "bio" to contact.bio,
                "photoUrl" to contact.photoUrl,
                "phoneHash" to contact.phoneHash,
                "addedAt" to System.currentTimeMillis(),
                "isBlocked" to false,
                "isMutual" to targetSavedMe
            )

            db.collection("users")
                .document(currentUid)
                .collection("contacts")
                .document(contact.contactUid)
                .set(contactData)
                .await()

            // Also update other side's isMutual flag if they had saved me
            if (targetSavedMe) {
                db.collection("users")
                    .document(contact.contactUid)
                    .collection("contacts")
                    .document(currentUid)
                    .update("isMutual", true)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error saving contact", e)
            Result.failure(e)
        }
    }

    suspend fun deleteContact(contactUid: String): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            db.collection("users")
                .document(currentUid)
                .collection("contacts")
                .document(contactUid)
                .delete()
                .await()

            // Update other user's isMutual to false
            db.collection("users")
                .document(contactUid)
                .collection("contacts")
                .document(currentUid)
                .update("isMutual", false)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ContactRepository", "Error deleting contact", e)
            Result.failure(e)
        }
    }

    suspend fun blockContact(contactUid: String, block: Boolean): Result<Unit> {
        val currentUid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        return try {
            db.collection("users")
                .document(currentUid)
                .collection("contacts")
                .document(contactUid)
                .update("isBlocked", block)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeContacts(): Flow<List<ContactItem>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("users")
            .document(currentUid)
            .collection("contacts")
            .orderBy("displayName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ContactRepository", "Contacts listener error", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(ContactItem::class.java)
                    }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun isMutualContact(otherUid: String): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        return try {
            val iSavedThem = db.collection("users")
                .document(currentUid)
                .collection("contacts")
                .document(otherUid)
                .get().await().exists()
            val theySavedMe = db.collection("users")
                .document(otherUid)
                .collection("contacts")
                .document(currentUid)
                .get().await().exists()
            iSavedThem && theySavedMe
        } catch (e: Exception) {
            false
        }
    }
}
