package com.example.nursingstudio.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class ProfileRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Current logged-in user ki ID
    private val userId: String?
        get() = auth.currentUser?.uid

    // Firestore se pura document lene ke liye
    fun getUserProfile(): Task<DocumentSnapshot>? {
        return userId?.let {
            db.collection("Users").document(it).get()
        }
    }

    // Agar future mein data update karna ho (like Photo update)
    fun updateProfile(data: Map<String, Any>): Task<Void>? {
        return userId?.let {
            db.collection("Users").document(it).update(data)
        }
    }
}