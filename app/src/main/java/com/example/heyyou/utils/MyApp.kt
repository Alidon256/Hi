package com.example.heyyou.utils

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Enable Firestore offline persistence
        val firestore = FirebaseFirestore.getInstance()

        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enable offline persistence
            .build()

        firestore.firestoreSettings = settings
    }
}
