package com.example.nursingstudio.data.repository

import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizResult
import com.example.nursingstudio.data.model.TestItem
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
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
     * Saves result to `test_results` and updates global user aggregate leaderboard atomically.
     */
    suspend fun submitQuizResult(result: QuizResult): Result<Boolean> {
        return try {
            val batch = firestore.batch()

            val resultDocRef = firestore.collection("test_results").document()
            val finalResult = result.copy(resultId = resultDocRef.id)
            batch.set(resultDocRef, finalResult)

            val testRankRef = firestore.collection("quizzes")
                .document(result.testId)
                .collection("leaderboard")
                .document(result.userId)

            val rankMap = hashMapOf(
                "userId" to result.userId,
                "userName" to result.userName,
                "score" to result.scoreObtained,
                "accuracy" to result.accuracyPercentage,
                "timeTaken" to result.timeTakenSeconds,
                "submittedAt" to result.submittedAtTimestamp
            )
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
     * 🚀 2026 Gold Standard Server-Side Aggregate Rank Calculation.
     * Computes exact rank positions using Firestore .count() without downloading document payloads.
     */
    suspend fun fetchUserRanks(testId: String, userScore: Double): Result<RankData> {
        return try {
            // 1. Testwise Ranks
            val testLeaderboardRef = firestore.collection("quizzes")
                .document(testId)
                .collection("leaderboard")

            val higherTestScoresQuery = testLeaderboardRef.whereGreaterThan("score", userScore)
            val testHigherCountTask = higherTestScoresQuery.count().get(AggregateSource.SERVER)
            val totalTestCountTask = testLeaderboardRef.count().get(AggregateSource.SERVER)

            // 2. Global Ranks
            val globalLeaderboardRef = firestore.collection("global_leaderboard")
            val higherGlobalScoresQuery = globalLeaderboardRef.whereGreaterThan("score", userScore)
            val globalHigherCountTask = higherGlobalScoresQuery.count().get(AggregateSource.SERVER)
            val totalGlobalCountTask = globalLeaderboardRef.count().get(AggregateSource.SERVER)

            val testHigherCount = testHigherCountTask.await().count
            val totalTestCount = totalTestCountTask.await().count
            val globalHigherCount = globalHigherCountTask.await().count
            val totalGlobalCount = totalGlobalCountTask.await().count

            val rankData = RankData(
                testRank = (testHigherCount + 1).toInt(),
                totalTestParticipants = if (totalTestCount > 0) totalTestCount.toInt() else 1,
                globalRank = (globalHigherCount + 1).toInt(),
                totalGlobalUsers = if (totalGlobalCount > 0) totalGlobalCount.toInt() else 1
            )

            Result.success(rankData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}