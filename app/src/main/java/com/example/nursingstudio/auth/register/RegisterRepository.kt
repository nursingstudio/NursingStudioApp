package com.example.nursingstudio.auth.register

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Email/Pass se account banana
    fun createUser(email: String, pass: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, pass)
    }

    // Phone link karna (World-class binding)
    fun linkPhone(credential: AuthCredential): Task<AuthResult>? {
        return auth.currentUser?.linkWithCredential(credential)
    }

    // Firestore mein data save karna
    fun saveUserData(uid: String, userData: Map<String, Any>): Task<Void> {
        return db.collection("Users").document(uid).set(userData)
    }
}