package com.example.nursingstudio.data.model

/**
 * 🚀 2026 Gold Standard Question Palette Node States.
 * Tracks user interactions and determines node appearance in palette drawer.
 */
enum class QuestionStatus {
    UNVISITED,             // Gray (#64748B) - Page not opened yet
    UNANSWERED,            // Red (#DC2626) - Visited but no option selected
    ANSWERED,              // Green (#16A34A) - Option selected
    MARKED_FOR_REVIEW,     // Violet (#7C3AED) - Marked without selecting option
    ANSWERED_AND_MARKED    // Deep Violet (#6D28D9) - Option selected and marked for review
}