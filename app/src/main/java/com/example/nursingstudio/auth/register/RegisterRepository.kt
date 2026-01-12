package com.example.nursingstudio.auth.register

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Create New User
    fun createUser(email: String, pass: String): Task<AuthResult> {
        return auth.createUserWithEmailAndPassword(email, pass)
    }

    // Link Phone Credential to the created account
    fun linkPhone(credential: AuthCredential): Task<AuthResult>? {
        return auth.currentUser?.linkWithCredential(credential)
    }

    // Save All details (including Material Date Picker selection)
    fun saveUserData(uid: String, userData: Map<String, Any>): Task<Void> {
        // "Users" collection mein professional structure ke sath data save hoga
        return db.collection("Users").document(uid).set(userData)
    }
}