package com.example.nursingstudio.data.model

// 🚀 2026 INDUSTRY GOLD STANDARD: Root Quiz Metadata Structure
data class QuizMetadata(
    val quizId: String = "",
    val title: String = "",
    val subject: String = "",
    val seriesName: String = "",
    val categoryId: String = "",
    val batchType: String = "FREE", // "FREE" or "PAID"
    val allowedBatches: List<String> = emptyList(),
    val totalQuestions: Int = 0,
    val timePerQuestionSeconds: Int = 60,
    val totalDurationMinutes: Int = 0,
    val isLocked: Boolean = false
)

// 🚀 Lightweight Model for Test List RecyclerView (item_test.xml)
data class QuizTestItem(
    val testId: String = "",
    val title: String = "",
    val categoryId: String = "",
    val totalQuestions: Int = 0,
    val durationMinutes: Int = 0,
    val isLocked: Boolean = true,
    val isAttempted: Boolean = false,
    val userScore: String = "--"
)

// 🚀 Rich-Media Content Type Safety Enumeration
enum class QuizMediaType {
    NONE, IMAGE, VIDEO
}

// 🚀 CBT Question Object
data class QuestionItem(
    val questionId: String = "",
    val questionIndex: Int = 0,
    val questionText: String = "",
    val mediaType: String = "NONE", // "NONE", "IMAGE", "VIDEO"
    val mediaUrl: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1,
    var selectedOptionIndex: Int = -1, // Stores student response in RAM
    var isMarkedForReview: Boolean = false // Track review state
)