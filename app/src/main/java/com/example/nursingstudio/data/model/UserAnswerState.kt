package com.example.nursingstudio.data.model

/**
 * State tracking for user interaction on an individual question.
 */
data class UserAnswerState(
    var selectedOptionIndex: Int? = null,
    var isMarkedForReview: Boolean = false,
    var status: QuestionStatus = QuestionStatus.UNVISITED
)