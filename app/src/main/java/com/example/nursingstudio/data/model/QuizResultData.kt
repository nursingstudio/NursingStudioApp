package com.example.nursingstudio.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SubjectPerformance(
    val subjectName: String = "",
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val incorrectCount: Int = 0
) : Parcelable

@Parcelize
data class QuizResultData(
    val quizId: String = "",
    val userId: String = "",
    val userName: String = "",
    val totalQuestions: Int = 0,
    val attemptedQuestions: Int = 0,
    val unattemptedQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val totalPossibleMarks: Double = 0.0,
    val finalScore: Double = 0.0,
    val scorePercentage: Float = 0.0f,
    val isPassed: Boolean = false,
    val totalDurationSeconds: Long = 0L,
    val timeTakenSeconds: Long = 0L,
    val globalRank: Int = -1,
    val totalParticipants: Int = -1,
    val subjectBreakdown: List<SubjectPerformance> = emptyList()
) : Parcelable