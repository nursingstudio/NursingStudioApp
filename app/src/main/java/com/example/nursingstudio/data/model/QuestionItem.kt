package com.example.nursingstudio.data.model

import androidx.annotation.Keep

/**
 * Represents an individual Question fetched from Firestore.
 */
@Keep
data class QuestionItem(
    val questionId: String = "",
    val questionText: String = "",
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val explanation: String = ""
)