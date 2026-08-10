package com.example.nursingstudio.data.model

// 🚀 2026 INDUSTRY GOLD STANDARD: Root Quiz Structure for Series & Subject Filtering
data class QuizMetadata(
    val quizId: String = "",
    val title: String = "",
    val subject: String = "",
    val seriesName: String = "",
    val batchType: String = "FREE", // "FREE" or "PAID"
    val allowedBatches: List<String> = emptyList(),
    val totalQuestions: Int = 0,
    val timePerQuestionSeconds: Int = 60,
    val totalDurationMinutes: Int = 0
)

// 🚀 2026 Rich-Media Content Type Safety Enumeration
enum class QuizMediaType {
    NONE, IMAGE, VIDEO
}

// 🚀 CBT Question Object with Support for Inline Media3 Players & Glide Caching
data class QuestionItem(
    val questionId: String = "",
    val questionIndex: Int = 0,
    val questionText: String = "",
    val mediaType: String = "NONE", // "NONE", "IMAGE", "VIDEO"
    val mediaUrl: String = "",
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1,
    var selectedOptionIndex: Int = -1 // 🔒 Local State Tracker: Stores student response in RAM
)