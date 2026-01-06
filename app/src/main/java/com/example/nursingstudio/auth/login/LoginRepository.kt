package com.example.nursingstudio.auth.login

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class LoginRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun signInWithEmail(email: String, pass: String) = auth.signInWithEmailAndPassword(email, pass)

    fun signInWithCredential(credential: AuthCredential) = auth.signInWithCredential(credential)

    fun checkUserInFirestore(uid: String): Task<DocumentSnapshot> {
        return db.collection("Users").document(uid).get()
    }

    fun sendResetPassword(email: String) = auth.sendPasswordResetEmail(email)
}