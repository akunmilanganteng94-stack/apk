package com.company.azrylvsmark.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

object FirebaseService {
    val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val firestore: FirebaseFirestore by lazy {
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
        db
    }

    val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance("gs://markgj-80508.firebasestorage.app")
    }

    val messaging: FirebaseMessaging by lazy { FirebaseMessaging.getInstance() }

    val currentUid: String?
        get() = auth.currentUser?.uid
}
