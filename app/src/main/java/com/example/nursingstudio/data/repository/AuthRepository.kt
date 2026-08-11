package com.example.nursingstudio.data.repository

import android.content.Context
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.data.model.User
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.IdGenerator
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val dataStoreManager: DataStoreManager
) {
    suspend fun createUser(email: String, pass: String): Result<AuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun saveUserData(uid: String, user: User): Result<Void?> = try {
        val targetNsId = IdGenerator.generateSecureNsId()

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
            "uniqueNsId" to targetNsId,
            "subscriptionType" to "Free",
            "createdAt" to FieldValue.serverTimestamp()
        )

        val result = db.collection("Users").document(uid).set(userWriteMap).await()
        dataStoreManager.saveUniqueNsId(targetNsId)
        dataStoreManager.saveUser(user.fullName, user.mobile)
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

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