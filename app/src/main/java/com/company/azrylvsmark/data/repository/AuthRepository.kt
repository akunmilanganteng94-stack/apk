package com.company.azrylvsmark.data.repository

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import com.company.azrylvsmark.data.firebase.FirebaseService
import com.company.azrylvsmark.data.model.UserProfile
import com.company.azrylvsmark.utils.CryptoUtils
import com.company.azrylvsmark.utils.ImageCompressor
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository(private val context: Context) {
    private val auth = FirebaseService.auth
    private val db = FirebaseService.firestore
    private val storage = FirebaseService.storage
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentUserProfile = MutableStateFlow<UserProfile?>(null)
    val currentUserProfile: StateFlow<UserProfile?> = _currentUserProfile.asStateFlow()

    private var profileListener: ListenerRegistration? = null

    // Cache the latest resending token for resending SMS
    var lastResendingToken: PhoneAuthProvider.ForceResendingToken? = null
        private set

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid != null) {
                listenToUserProfile(uid)
                updateFcmTokenInternal()
            } else {
                profileListener?.remove()
                profileListener = null
                _currentUserProfile.value = null
            }
        }
    }

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    val isLoggedIn: Boolean
        get() = isUserLoggedIn

    fun updatePresence(online: Boolean) {
        setOnlineStatus(online)
    }

    val currentUid: String?
        get() = auth.currentUser?.uid

    private fun listenToUserProfile(uid: String) {
        profileListener?.remove()
        profileListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AuthRepository", "Error listening to profile", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    _currentUserProfile.value = profile
                } else {
                    _currentUserProfile.value = null
                }
            }
    }

    /**
     * Sends SMS OTP code to the given phone number via Firebase PhoneAuthProvider.
     */
    fun sendOtp(
        activity: Activity,
        phoneNumber: String,
        forceResendingToken: PhoneAuthProvider.ForceResendingToken? = lastResendingToken,
        onCodeSent: (verificationId: String, token: PhoneAuthProvider.ForceResendingToken) -> Unit,
        onVerificationFailed: (errorMessage: String) -> Unit,
        onAutoVerified: () -> Unit
    ) {
        val normalized = CryptoUtils.normalizePhoneNumber(phoneNumber)
        if (normalized.isBlank() || normalized.length < 8) {
            onVerificationFailed("Format nomor telepon tidak valid. Pastikan nomor sudah lengkap.")
            return
        }

        Log.d("AuthRepository", "Sending SMS verification to: $normalized")

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("AuthRepository", "Auto verification completed for $normalized")
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { onAutoVerified() }
                    .addOnFailureListener {
                        onVerificationFailed(parseAuthError(it))
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("AuthRepository", "Phone verification failed for $normalized", e)
                onVerificationFailed(parseAuthError(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("AuthRepository", "SMS verification code sent successfully to $normalized")
                lastResendingToken = token
                onCodeSent(verificationId, token)
            }
        }

        try {
            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(normalized)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (forceResendingToken != null) {
                builder.setForceResendingToken(forceResendingToken)
            }

            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } catch (e: Exception) {
            Log.e("AuthRepository", "Exception calling verifyPhoneNumber", e)
            onVerificationFailed(parseAuthError(e))
        }
    }

    private fun parseAuthError(e: Throwable): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException -> {
                "Nomor telepon tidak valid atau kode salah. Periksa kembali nomor yang Anda masukkan."
            }
            is FirebaseTooManyRequestsException -> {
                "Terlalu banyak permintaan SMS ke nomor ini. Harap tunggu beberapa menit sebelum mencoba lagi."
            }
            else -> {
                val msg = e.localizedMessage ?: "Gagal mengirim SMS verifikasi."
                if (msg.contains("quota", ignoreCase = true)) {
                    "Kuota SMS Firebase harian tercapai. Silakan coba nomor lain atau nomor uji Firebase."
                } else if (msg.contains("blocked", ignoreCase = true)) {
                    "Perangkat atau nomor diblokir sementara karena aktivitas mencurigakan."
                } else if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                    "Koneksi internet bermasalah. Pastikan perangkat terhubung ke internet."
                } else {
                    msg
                }
            }
        }
    }

    suspend fun verifyOtp(verificationId: String, code: String): Result<Boolean> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code.trim())
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                // Check if profile exists
                val doc = db.collection("users").document(user.uid).get().await()
                Result.success(doc.exists())
            } else {
                Result.failure(Exception("Login gagal, tidak dapat mengautentikasi pengguna."))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error verifying OTP", e)
            Result.failure(e)
        }
    }

    suspend fun saveProfile(
        displayName: String,
        username: String,
        bio: String,
        photoUri: Uri?,
        rawPhoneNumber: String
    ): Result<UserProfile> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("Not authenticated"))
        val normalizedPhone = CryptoUtils.normalizePhoneNumber(
            if (rawPhoneNumber.isNotBlank()) rawPhoneNumber else (auth.currentUser?.phoneNumber ?: "")
        )
        val phoneHash = CryptoUtils.hashPhoneNumber(normalizedPhone)

        return try {
            var photoUrl = _currentUserProfile.value?.photoUrl ?: ""
            if (photoUri != null) {
                val compressedBytes = ImageCompressor.compressImageUri(context, photoUri)
                val ref = storage.reference.child("profile/$uid/avatar_${System.currentTimeMillis()}.jpg")
                ref.putBytes(compressedBytes).await()
                photoUrl = ref.downloadUrl.await().toString()
            }

            val profile = UserProfile(
                uid = uid,
                phoneHash = phoneHash,
                phoneNumber = normalizedPhone,
                displayName = displayName.trim(),
                username = username.trim().removePrefix("@"),
                bio = bio.trim(),
                photoUrl = photoUrl,
                createdAt = _currentUserProfile.value?.createdAt ?: System.currentTimeMillis(),
                lastSeen = System.currentTimeMillis(),
                online = true
            )

            // Save user profile
            db.collection("users").document(uid).set(profile).await()

            // Save phone directory lookup entry (hashed index for safe lookup)
            val directoryEntry = hashMapOf(
                "uid" to uid,
                "phoneHash" to phoneHash,
                "displayName" to profile.displayName,
                "username" to profile.username,
                "bio" to profile.bio,
                "photoUrl" to profile.photoUrl
            )
            db.collection("phone_directory").document(phoneHash).set(directoryEntry).await()

            _currentUserProfile.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving profile", e)
            Result.failure(e)
        }
    }

    fun setOnlineStatus(online: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val updates = hashMapOf<String, Any>(
            "online" to online,
            "lastSeen" to System.currentTimeMillis()
        )
        db.collection("users").document(uid).update(updates)
    }

    private fun updateFcmTokenInternal() {
        FirebaseService.messaging.token.addOnSuccessListener { token ->
            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
            db.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }

    fun signOut() {
        setOnlineStatus(false)
        profileListener?.remove()
        profileListener = null
        _currentUserProfile.value = null
        auth.signOut()
    }
}
