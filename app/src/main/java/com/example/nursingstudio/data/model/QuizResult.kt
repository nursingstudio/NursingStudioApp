package com.example.nursingstudio.data.model

import androidx.annotation.Keep

/**
 * 🚀 2026 Gold Standard Quiz Submission Result Object.
 */
@Keep
data class QuizResult(
    val resultId: String = "",
    val userId: String = "",
    val userName: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val unvisitedCount: Int = 0,
    val scoreObtained: Double = 0.0,
    val totalMaxMarks: Double = 0.0,
    val accuracyPercentage: Double = 0.0,
    val timeTakenSeconds: Long = 0L,
    val submittedAtTimestamp: Long = System.currentTimeMillis()
)