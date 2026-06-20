package com.example.nursingstudio.data.repository

import android.content.Context
import com.example.nursingstudio.utils.AppSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    // 🚀 2026 Standard: Clean Result Wrapper for Authentication Pipeline
    suspend fun createUser(email: String, pass: String): Result<AuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 🚀 FIXED & SANITIZED: 2026 Enterprise Identity Generation & Synchronization Pipeline
    suspend fun saveUserData(uid: String, user: com.example.nursingstudio.data.model.User): Result<Void?> = try {

        // 🚀 Step 1: Generate high-scalability unique token identifier at server registration edge
        val targetNsId = com.example.nursingstudio.utils.IdGenerator.generateSecureNsId()

        val userWriteMap = hashMapOf(
            "uid" to uid,
            "fullName" to user.fullName,
            "gender" to user.gender,
            "dob" to user.dob,
            "maritalStatus" to user.maritalStatus,
            "religion" to user.religion,
            "education" to user.education,
            "occupation" to user.occupation,
            "mobile" to user.mobile,
            "email" to user.email,
            "country" to user.country,
            "state" to user.state,
            "district" to user.district,
            "address" to user.address,
            "pincode" to user.pincode,
            "regState" to user.regState,
            "regNumber" to user.regNumber,
            "isNursingRegistered" to user.isNursingRegistered,

            // 🚀 Step 2: Inject the generated unique ID directly inside cloud system payload matrix
            "uniqueNsId" to targetNsId,
            "subscriptionType" to "Free", // Default layout fallback subscription model

            // 🔥 SERVER CLOCK ENFORCEMENT: Enforces absolute data consistency over user settings alterations
            "createdAt" to FieldValue.serverTimestamp()
        )

        val result = db.collection("Users").document(uid).set(userWriteMap).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // 🚀 2026 Core Flow: Email Session Verification Check
    suspend fun signInWithEmail(email: String, pass: String): Result<AuthResult> = try {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun checkUserById(uid: String): Boolean = try {
        val doc = db.collection("Users").document(uid).get().await()
        doc.exists()
    } catch (_: Exception) {
        false
    }

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
        AppSettings.startNewUserSession(context)
    }
}