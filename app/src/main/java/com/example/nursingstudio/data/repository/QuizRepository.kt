package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.TestItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Fetches tests filtered by Category ID.
     * Sanitizes "category_pyq" -> "pyq" for robust backend matching.
     */
    suspend fun getTestsByCategory(categoryId: String): Result<List<TestItem>> {
        return try {
            val targetCategory = categoryId.lowercase().trim()
            val shortCategory = targetCategory.replace("category_", "")

            val snapshot = firestore.collection("quizzes")
                .whereIn("categoryId", listOf(targetCategory, shortCategory))
                .get()
                .await()

            val testList = snapshot.toObjects(TestItem::class.java)
            Result.success(testList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}