package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.model.QuizTestItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Fetches test series list by category ID for TestListFragment
     */
    suspend fun getTestsByCategory(categoryId: String): Result<List<QuizTestItem>> = runCatching {
        val querySnapshot = firestore.collection("quizzes")
            .whereEqualTo("categoryId", categoryId)
            .get()
            .await()

        querySnapshot.documents.map { doc ->
            QuizTestItem(
                testId = doc.getString("quizId") ?: doc.id,
                title = doc.getString("title") ?: "NORCET Test",
                categoryId = categoryId,
                totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0,
                durationMinutes = doc.getLong("totalDurationMinutes")?.toInt() ?: 60,
                isLocked = doc.getBoolean("isLocked") ?: false,
                isAttempted = false,
                userScore = "--"
            )
        }
    }

    /**
     * Fetches Quiz Metadata by business 'quizId' field
     */
    suspend fun getQuizMetadata(quizId: String): Result<QuizMetadata> = runCatching {
        val querySnapshot = firestore.collection("quizzes")
            .whereEqualTo("quizId", quizId)
            .limit(1)
            .get()
            .await()

        val document = if (!querySnapshot.isEmpty) {
            querySnapshot.documents.first()
        } else {
            val testSnapshot = firestore.collection("tests")
                .whereEqualTo("quizId", quizId)
                .limit(1)
                .get()
                .await()

            if (!testSnapshot.isEmpty) {
                testSnapshot.documents.first()
            } else {
                throw IllegalStateException("Quiz metadata not found for ID: $quizId")
            }
        }

        QuizMetadata(
            quizId = document.getString("quizId") ?: quizId,
            title = document.getString("title") ?: "NORCET Test",
            subject = document.getString("subject") ?: "General",
            totalQuestions = document.getLong("totalQuestions")?.toInt() ?: 0,
            totalDurationMinutes = document.getLong("totalDurationMinutes")?.toInt() ?: 60
        )
    }

    /**
     * Fetches Questions list from subcollection using parent 'quizId'
     */
    suspend fun getQuizQuestions(quizId: String): Result<List<QuestionItem>> = runCatching {
        val querySnapshot = firestore.collection("quizzes")
            .whereEqualTo("quizId", quizId)
            .limit(1)
            .get()
            .await()

        val parentDocument = if (!querySnapshot.isEmpty) {
            querySnapshot.documents.first()
        } else {
            firestore.collection("tests")
                .whereEqualTo("quizId", quizId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull() ?: throw IllegalStateException("Parent quiz document not found.")
        }

        val questionsSnapshot = parentDocument.reference.collection("questions")
            .orderBy("questionIndex")
            .get()
            .await()

        questionsSnapshot.documents.mapNotNull { doc ->
            val optionsList = when (val optionsRaw = doc.get("options")) {
                is List<*> -> optionsRaw.map { it.toString() }
                is String -> optionsRaw.removeSurrounding("[", "]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                else -> emptyList()
            }

            QuestionItem(
                questionId = doc.getString("questionId") ?: doc.id,
                questionIndex = doc.getLong("questionIndex")?.toInt() ?: 1,
                questionText = doc.getString("questionText") ?: "",
                mediaType = doc.getString("mediaType") ?: "NONE",
                mediaUrl = doc.getString("mediaUrl") ?: "",
                options = optionsList,
                selectedOptionIndex = -1
            )
        }
    }

    /**
     * Unified fetch method for UI callers expecting Pair
     */
    suspend fun fetchQuizMetadataAndQuestions(quizId: String): Pair<QuizMetadata, List<QuestionItem>> {
        val metadata = getQuizMetadata(quizId).getOrThrow()
        val questions = getQuizQuestions(quizId).getOrThrow()
        return Pair(metadata, questions)
    }
}