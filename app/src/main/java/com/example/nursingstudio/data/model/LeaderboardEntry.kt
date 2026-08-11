package com.example.nursingstudio.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class LeaderboardEntry(
    val userId: String = "",
    val userName: String = "",
    val finalScore: Double = 0.0,
    val totalPossibleMarks: Double = 0.0,
    val timeTakenSeconds: Long = 0L,
    val accuracyPercentage: Float = 0.0f,
    val rank: Int = 0,
    val submittedAt: Long = System.currentTimeMillis()
) : Parcelable