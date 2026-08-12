package com.example.nursingstudio.data.model

enum class QuestionStatus {
    ANSWERED,     // 🟢 Green
    SKIPPED,      // 🔘 Dark Gray
    UNANSWERED,   // ⚪ White
    REVIEW        // 🟣 Purple
}

data class QuestionPaletteItem(
    val questionIndex: Int, // 0-based index
    val displayIndex: Int,  // 1-based index (for UI display)
    val status: QuestionStatus,
    val isCurrent: Boolean = false
)