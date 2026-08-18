package com.example.nursingstudio.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

/**
 * Represents an individual Question fetched from Firestore.
 * Annotated with @Keep and @PropertyName for robust serialization.
 */
@Keep
data class QuestionItem(
    @get:PropertyName("questionId") @set:PropertyName("questionId") var questionId: String = "",
    @get:PropertyName("questionText") @set:PropertyName("questionText") var questionText: String = "",
    @get:PropertyName("mediaType") @set:PropertyName("mediaType") var rawMediaType: String = "NONE",
    @get:PropertyName("mediaUrl") @set:PropertyName("mediaUrl") var mediaUrl: String? = null,
    @get:PropertyName("options") @set:PropertyName("options") var options: List<String> = emptyList(),
    @get:PropertyName("correctAnswerIndex") @set:PropertyName("correctAnswerIndex") var correctAnswerIndex: Int = 0,
    @get:PropertyName("explanation") @set:PropertyName("explanation") var explanation: String = ""
) {

    /**
     * Helper getter to map raw Firestore String into typed MediaType Enum safely.
     */
    @get:Exclude
    val mediaType: MediaType
        get() = try {
            MediaType.valueOf(rawMediaType.uppercase().trim())
        } catch (_: Exception) {
            MediaType.NONE
        }
}