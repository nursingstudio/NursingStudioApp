package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizResult
import com.example.nursingstudio.data.model.TestItem
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    data class RankData(
        val testRank: Int = 1,
        val totalTestParticipants: Int = 1,
        val globalRank: Int = 1,
        val totalGlobalUsers: Int = 1
    )

    /**
     * Fetches tests filtered by Category ID with robust Multi-Alias Fallback.
     */
    suspend fun getTestsByCategory(categoryId: String): Result<List<TestItem>> {
        return try {
            val targetCategory = categoryId.lowercase().trim()
            val shortCategory = targetCategory.replace("category_", "")

            val categoryVariants = mutableSetOf(
                targetCategory,
                shortCategory,
                shortCategory.replace("_", "")
            )

            val queryList = categoryVariants.toList().take(10)

            val snapshot = firestore.collection("quizzes")
                .whereIn("categoryId", queryList)
                .get()
                .await()

            val testList = snapshot.toObjects(TestItem::class.java)
            Result.success(testList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 🚀 2026 Gold Standard Question Collection Retrieval
     * Resolves parent quiz document by field `testId`, then retrieves `questions` sub-collection.
     */
    suspend fun getQuestionsForTest(testId: String): Result<List<QuestionItem>> = runCatching {
        // 1. Locate the quiz document where field "testId" == target testId
        val parentQuizSnapshot = firestore.collection("quizzes")
            .whereEqualTo("testId", testId)
            .limit(1)
            .get()
            .await()

        if (parentQuizSnapshot.isEmpty) {
            // Fallback: Check if testId was directly used as Document ID
            val directSubcollection = firestore.collection("quizzes")
                .document(testId)
                .collection("questions")
                .get()
                .await()

            return@runCatching directSubcollection.toObjects(QuestionItem::class.java)
        }

        // 2. Extract actual Firestore parent Document ID
        val parentDocumentId = parentQuizSnapshot.documents.first().id

        // 3. Fetch questions sub-collection using the resolved document ID
        val questionsSnapshot = firestore.collection("quizzes")
            .document(parentDocumentId)
            .collection("questions")
            .get()
            .await()

        questionsSnapshot.toObjects(QuestionItem::class.java)
    }

    /**
     * 🚀 2026 Gold Standard Result Submission
     * Uses composite index matching fields: finalScore & timeTakenSeconds
     */
    suspend fun submitQuizResult(result: QuizResult): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext try {
            val batch = firestore.batch()

            val resultDocRef = firestore.collection("test_results").document()
            val finalResult = result.copy(resultId = resultDocRef.id)
            batch.set(resultDocRef, finalResult)

            // Matching Cloud Shell Leaderboard Index Fields
            val rankMap = hashMapOf(
                "userId" to result.userId,
                "userName" to result.userName,
                "finalScore" to result.scoreObtained,
                "accuracy" to result.accuracyPercentage,
                "timeTakenSeconds" to result.timeTakenSeconds,
                "submittedAt" to result.submittedAtTimestamp
            )

            val testRankRef = firestore.collection("quizzes")
                .document(result.testId)
                .collection("leaderboard")
                .document(result.userId)
            batch.set(testRankRef, rankMap)

            val globalRankRef = firestore.collection("global_leaderboard")
                .document(result.userId)
            batch.set(globalRankRef, rankMap)

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 🚀 2026 Gold Standard Server-Side Exact Rank Calculation
     * Calculates rank including time tie-breaker with O(1) payload footprint.
     */
    suspend fun fetchUserRanks(testId: String, userScore: Double, userTimeTakenSeconds: Long): Result<RankData> = withContext(Dispatchers.IO) {
        return@withContext try {
            val testLeaderboardRef = firestore.collection("quizzes")
                .document(testId)
                .collection("leaderboard")

            // 1. Strictly Higher Scores Count
            val higherTestScoresTask = testLeaderboardRef
                .whereGreaterThan("finalScore", userScore)
                .count()
                .get(AggregateSource.SERVER)

            // 2. Same Score but Faster Time Count
            val sameScoreFasterTask = testLeaderboardRef
                .whereEqualTo("finalScore", userScore)
                .whereLessThan("timeTakenSeconds", userTimeTakenSeconds)
                .count()
                .get(AggregateSource.SERVER)

            val totalTestCountTask = testLeaderboardRef.count().get(AggregateSource.SERVER)

            // Global Leaderboard Operations
            val globalLeaderboardRef = firestore.collection("global_leaderboard")
            val higherGlobalScoresTask = globalLeaderboardRef
                .whereGreaterThan("finalScore", userScore)
                .count()
                .get(AggregateSource.SERVER)

            val totalGlobalCountTask = globalLeaderboardRef.count().get(AggregateSource.SERVER)

            val higherTestCount = higherTestScoresTask.await().count
            val sameScoreFasterCount = sameScoreFasterTask.await().count
            val totalTestCount = totalTestCountTask.await().count

            val higherGlobalCount = higherGlobalScoresTask.await().count
            val totalGlobalCount = totalGlobalCountTask.await().count

            val exactTestRank = (higherTestCount + sameScoreFasterCount + 1).toInt()
            val exactGlobalRank = (higherGlobalCount + 1).toInt()

            val rankData = RankData(
                testRank = exactTestRank,
                totalTestParticipants = if (totalTestCount > 0) totalTestCount.toInt() else 1,
                globalRank = exactGlobalRank,
                totalGlobalUsers = if (totalGlobalCount > 0) totalGlobalCount.toInt() else 1
            )

            Result.success(rankData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}