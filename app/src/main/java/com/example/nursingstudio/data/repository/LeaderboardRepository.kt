package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.LeaderboardEntry
import com.example.nursingstudio.data.model.QuizResultData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    /**
     * Submits student score to Firestore with precise type mapping
     */
    suspend fun submitQuizScore(resultData: QuizResultData): Result<Unit> {
        return try {
            val entry = LeaderboardEntry(
                userId = resultData.userId.ifEmpty { "user_${System.currentTimeMillis()}" },
                userName = resultData.userName.ifEmpty { "Nursing Student" },
                finalScore = resultData.finalScore.toDouble(),
                totalPossibleMarks = resultData.totalPossibleMarks.toDouble(),
                timeTakenSeconds = resultData.timeTakenSeconds.toLong(),
                accuracyPercentage = resultData.scorePercentage,
                submittedAt = System.currentTimeMillis()
            )

            firestore.collection("quizzes")
                .document(resultData.quizId)
                .collection("leaderboard")
                .document(entry.userId)
                .set(entry)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Streams real-time leaderboard entries ordered by score (descending) and time (ascending)
     */
    fun getRealTimeLeaderboard(quizId: String, limit: Long = 50): Flow<Result<List<LeaderboardEntry>>> = callbackFlow {
        val query = firestore.collection("quizzes")
            .document(quizId)
            .collection("leaderboard")
            .orderBy("finalScore", Query.Direction.DESCENDING)
            .orderBy("timeTakenSeconds", Query.Direction.ASCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Result.failure(error))
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val entries = snapshot.documents.mapIndexedNotNull { index, doc ->
                    doc.toObject(LeaderboardEntry::class.java)?.copy(rank = index + 1)
                }
                trySend(Result.success(entries))
            }
        }

        awaitClose { listener.remove() }
    }
}