package com.example.nursingstudio.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // --- SHARED LOGIC ---
    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getAuthInstance() = auth

    // --- LOGIN LOGIC ---
    fun signInWithEmail(email: String, pass: String) = auth.signInWithEmailAndPassword(email, pass)
    fun signInWithCredential(credential: AuthCredential) = auth.signInWithCredential(credential)
    fun checkUserInFirestore(uid: String): Task<DocumentSnapshot> = db.collection("Users").document(uid).get()
    fun sendResetPassword(email: String) = auth.sendPasswordResetEmail(email)

    // Ise Repository ke andar add kar lo
    fun resetPassword(email: String): Task<Void> = auth.sendPasswordResetEmail(email)
    // --- REGISTER LOGIC ---
    fun createUser(email: String, pass: String): Task<AuthResult> = auth.createUserWithEmailAndPassword(email, pass)
    fun linkPhone(credential: AuthCredential): Task<AuthResult>? = auth.currentUser?.linkWithCredential(credential)
    fun saveUserData(uid: String, userData: Map<String, Any>): Task<Void> = db.collection("Users").document(uid).set(userData)

    // --- OTP VERIFICATION (Ab ye class ke andar hai!) ---
    suspend fun verifyOtp(credential: AuthCredential): Result<AuthResult> {
        return try {
            // await() ab kaam karega kyunki kotlinx.coroutines.tasks.await import hai
            val result = auth.signInWithCredential(credential).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}