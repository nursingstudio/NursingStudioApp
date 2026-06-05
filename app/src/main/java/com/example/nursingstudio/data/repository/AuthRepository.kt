package com.example.nursingstudio.data.repository

import android.content.Context
import com.example.nursingstudio.utils.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext // ✅ New Import
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    // 2026 Standard: Use Result wrapper for better error handling
    suspend fun createUser(email: String, pass: String): Result<AuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ✅ Update saveUserData to use User Model
    suspend fun saveUserData(uid: String, user: com.example.nursingstudio.data.model.User): Result<Void?> = try {
        val result = db.collection("Users").document(uid).set(user).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 1. ⭐ CHECK USER STATUS (For Smart Redirect)
    // Ye function check karega ki number database mein hai ya nahi
    suspend fun checkUserByPhone(phone: String): Boolean {
        return try {
            val query = db.collection("Users")
                .whereEqualTo("mobile", "+91$phone")
                .get()
                .await()
            !query.isEmpty
        } catch (_: Exception) {
            false
        }
    }

    // 2. ⭐ VERIFICATION & DATA SYNC
    // User login kare ya register, ye function dono handle karega
    suspend fun verifyAndSyncUser(credential: AuthCredential): Result<Pair<AuthResult, Boolean>> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            var isRegistered = false
            user?.let {
                val doc = db.collection("Users").document(it.uid).get().await()
                isRegistered = doc.exists()
            }

            Result.success(Pair(result, isRegistered))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. ⭐ EMAIL LOGIN (With Firestore Verification)
    suspend fun signInWithEmail(email: String, pass: String): Result<AuthResult> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkUserById(uid: String): Boolean {
        return try {
            val doc = db.collection("Users").document(uid).get().await()
            doc.exists()
        } catch (_: Exception) {
            false
        }
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null
    fun getCurrentUid(): String? = auth.currentUser?.uid

    fun signOut() { // ✅ Argument removed
        auth.signOut()
        // Ab hum context yahan direct use kar sakte hain
        AppSettings.startNewUserSession(context)
    }
}