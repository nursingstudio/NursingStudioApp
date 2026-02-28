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

    // --- LOGIN LOGIC ---
    suspend fun signInWithEmail(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    fun checkUserInFirestore(uid: String): Task<DocumentSnapshot> =
        db.collection("Users").document(uid).get()

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
    }

    // --- REGISTER LOGIC ---
    fun createUser(email: String, pass: String): Task<AuthResult> =
        auth.createUserWithEmailAndPassword(email, pass)

    fun linkPhone(credential: AuthCredential): Task<AuthResult>? =
        auth.currentUser?.linkWithCredential(credential)

    fun saveUserData(uid: String, userData: Map<String, Any>): Task<Void> =
        db.collection("Users").document(uid).set(userData)

    // --- OTP VERIFICATION (Ab ye class ke andar hai!) ---
    suspend fun verifyOtp(credential: AuthCredential): Result<Pair<AuthResult, Boolean>> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            // World-Class Addition: Check if user document exists
            var isProfileComplete = false
            if (user != null) {
                val doc = db.collection("Users").document(user.uid).get().await()
                isProfileComplete = doc.exists()
            }

            // Result ke saath profile status bhi bhej rahe hain
            Result.success(Pair(result, isProfileComplete))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}