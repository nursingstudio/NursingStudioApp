package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val dataStoreManager: DataStoreManager
) {
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun getUserProfile(): Result<User?> = try {
        val uid = currentUserId ?: throw Exception("User is not authenticated")
        val snapshot = db.collection("Users").document(uid).get().await()
        val user = snapshot.toObject(User::class.java)

        // 🚀 2026 Offline-First Caching Strategy using DataStoreManager
        user?.let {
            dataStoreManager.saveUser(it.fullName, it.mobile)
        }

        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateProfile(data: Map<String, Any>): Result<Unit> = try {
        val uid = currentUserId ?: throw Exception("User is not authenticated")
        db.collection("Users").document(uid).update(data).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}