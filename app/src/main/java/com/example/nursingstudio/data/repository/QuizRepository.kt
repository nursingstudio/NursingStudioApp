package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun getQuizMetadata(quizId: String): Result<QuizMetadata> {
        return try {
            val snapshot = firestore.collection("quizzes")
                .document(quizId)
                .get()
                .await()

            val metadata = snapshot.toObject(QuizMetadata::class.java)
                ?: throw Exception("Quiz metadata not found")
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuizQuestions(quizId: String): Result<List<QuestionItem>> {
        return try {
            val snapshot = firestore.collection("quizzes")
                .document(quizId)
                .collection("questions")
                .orderBy("questionIndex")
                .get()
                .await()

            val questions = snapshot.toObjects(QuestionItem::class.java)
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}