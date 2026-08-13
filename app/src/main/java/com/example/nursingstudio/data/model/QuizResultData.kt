package com.example.nursingstudio.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuizResultData(
    val quizId: String = "",
    val userId: String = "",
    val userName: String = "",
    val isPassed: Boolean = false,
    val finalScore: Float = 0f,
    val totalPossibleMarks: Float = 0f,
    val scorePercentage: Float = 0f,
    val globalRank: Int = 0,
    val totalParticipants: Int = 0,
    val timeTakenSeconds: Int = 0,
    val totalDurationSeconds: Int = 0,
    val attemptedQuestions: Int = 0,
    val unattemptedQuestions: Int = 0,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0
) : Parcelable