package com.company.azrylvsmark.data.repository

import android.util.Log
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.company.azrylvsmark.data.model.CallSession
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CallRepository {
    private val db = FirebaseService.firestore
    private val auth = FirebaseService.auth

    val currentUid: String?
        get() = auth.currentUser?.uid

    fun observeIncomingCall(): Flow<CallSession?> = observeIncomingCalls()

    suspend fun initiateCall(
        receiverId: String,
        receiverName: String,
        receiverPhoto: String,
        isVideo: Boolean
    ): Result<String> {
        val caller = auth.currentUser
        val callerName = caller?.displayName ?: "User"
        val callerPhoto = caller?.photoUrl?.toString() ?: ""
        val type = if (isVideo) CallSession.TYPE_VIDEO else CallSession.TYPE_VOICE
        val res = startCall(receiverId, receiverName, receiverPhoto, type, callerName, callerPhoto)
        return res.map { it.callId }
    }

    suspend fun sendOfferSdp(callId: String, sdp: String) {
        setOffer(callId, sdp)
    }

    suspend fun sendAnswerSdp(callId: String, sdp: String) {
        setAnswer(callId, sdp)
    }

    suspend fun sendIceCandidate(callId: String, isCaller: Boolean, candidateMap: Map<String, Any>) {
        addIceCandidate(callId, isCaller, candidateMap)
    }

    suspend fun startCall(
        receiverId: String,
        receiverName: String,
        receiverPhoto: String,
        type: String,
        callerName: String,
        callerPhoto: String
    ): Result<CallSession> {
        val callerId = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val callId = db.collection("calls").document().id
        val session = CallSession(
            callId = callId,
            callerId = callerId,
            callerName = callerName,
            callerPhoto = callerPhoto,
            receiverId = receiverId,
            receiverName = receiverName,
            receiverPhoto = receiverPhoto,
            type = type,
            status = CallSession.STATUS_RINGING,
            timestamp = System.currentTimeMillis(),
            isOutgoing = true
        )
        return try {
            db.collection("calls").document(callId).set(session).await()
            Result.success(session)
        } catch (e: Exception) {
            Log.e("CallRepository", "Error starting call", e)
            Result.failure(e)
        }
    }

    fun observeIncomingCalls(): Flow<CallSession?> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val registration = db.collection("calls")
            .whereEqualTo("receiverId", currentUid)
            .whereEqualTo("status", CallSession.STATUS_RINGING)
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CallRepository", "Incoming calls error", error)
                    return@addSnapshotListener
                }
                val incomingDoc = snapshot?.documents?.firstOrNull()
                val session = incomingDoc?.toObject(CallSession::class.java)?.copy(isOutgoing = false)
                trySend(session)
            }

        awaitClose { registration.remove() }
    }

    fun observeCallSession(callId: String): Flow<CallSession?> = callbackFlow {
        val registration = db.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val session = snapshot.toObject(CallSession::class.java)
                    val currentUid = auth.currentUser?.uid
                    trySend(session?.copy(isOutgoing = session.callerId == currentUid))
                } else {
                    trySend(null)
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun acceptCall(callId: String): Result<Unit> {
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to CallSession.STATUS_ACCEPTED,
                    "startedAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectCall(callId: String): Result<Unit> {
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to CallSession.STATUS_REJECTED,
                    "endedAt" to System.currentTimeMillis()
                )
            ).await()
            saveToHistory(callId, CallSession.STATUS_REJECTED, 0)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endCall(callId: String, durationSec: Int): Result<Unit> {
        return try {
            db.collection("calls").document(callId).update(
                mapOf(
                    "status" to CallSession.STATUS_ENDED,
                    "endedAt" to System.currentTimeMillis(),
                    "durationSeconds" to durationSec
                )
            ).await()
            saveToHistory(callId, CallSession.STATUS_ENDED, durationSec)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveToHistory(callId: String, status: String, durationSec: Int) {
        val currentUid = auth.currentUser?.uid ?: return
        try {
            val callDoc = db.collection("calls").document(callId).get().await()
            val session = callDoc.toObject(CallSession::class.java) ?: return
            val historyEntry = session.copy(status = status, durationSeconds = durationSec)
            db.collection("users").document(currentUid).collection("call_history").document(callId)
                .set(historyEntry).await()

            val otherUid = if (session.callerId == currentUid) session.receiverId else session.callerId
            db.collection("users").document(otherUid).collection("call_history").document(callId)
                .set(historyEntry.copy(isOutgoing = session.callerId == otherUid)).await()
        } catch (e: Exception) {
            Log.e("CallRepository", "Error saving call history", e)
        }
    }

    fun observeCallHistory(): Flow<List<CallSession>> = callbackFlow {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = db.collection("users").document(currentUid)
            .collection("call_history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { it.toObject(CallSession::class.java) }
                    trySend(list)
                }
            }

        awaitClose { registration.remove() }
    }

    // WebRTC Signaling methods
    suspend fun setOffer(callId: String, sdp: String) {
        db.collection("calls").document(callId).set(
            mapOf("offer" to mapOf("type" to "offer", "sdp" to sdp)),
            SetOptions.merge()
        ).await()
    }

    suspend fun setAnswer(callId: String, sdp: String) {
        db.collection("calls").document(callId).set(
            mapOf("answer" to mapOf("type" to "answer", "sdp" to sdp)),
            SetOptions.merge()
        ).await()
    }

    suspend fun addIceCandidate(callId: String, isCaller: Boolean, candidateMap: Map<String, Any>) {
        val subCollection = if (isCaller) "callerCandidates" else "receiverCandidates"
        db.collection("calls").document(callId).collection(subCollection).add(candidateMap).await()
    }

    fun observeOffer(callId: String): Flow<String?> = callbackFlow {
        val registration = db.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val offerMap = snapshot?.get("offer") as? Map<*, *>
                val sdp = offerMap?.get("sdp") as? String
                trySend(sdp)
            }
        awaitClose { registration.remove() }
    }

    fun observeAnswer(callId: String): Flow<String?> = callbackFlow {
        val registration = db.collection("calls").document(callId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val answerMap = snapshot?.get("answer") as? Map<*, *>
                val sdp = answerMap?.get("sdp") as? String
                trySend(sdp)
            }
        awaitClose { registration.remove() }
    }

    fun observeRemoteCandidates(callId: String, isCaller: Boolean): Flow<Map<String, Any>> = callbackFlow {
        val subCollection = if (isCaller) "receiverCandidates" else "callerCandidates"
        val registration = db.collection("calls").document(callId).collection(subCollection)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                        trySend(change.document.data)
                    }
                }
            }
        awaitClose { registration.remove() }
    }
}
