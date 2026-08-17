package com.example.nursingstudio.data.model

import androidx.annotation.Keep

/**
 * 🚀 2026 Industry Standard Test Model
 * Represents an individual Test Series inside a Category.
 */
@Keep
data class TestItem(
    val testId: String = "",
    val testTitle: String = "",
    val categoryId: String = "",
    val seriesName: String = "",
    val description: String = "",
    val timePerQuestionSeconds: Int = 60,
    val totalDurationMinutes: Int = 100,
    val totalQuestions: Int = 100,
    val batchType: String = "FREE", // "FREE" or "PAID"
    @field:JvmField val isLocked: Boolean = false,
    @field:JvmField val isFree: Boolean = true
) {
    /**
     * 🆔 Backward & Forward Compatible ID Alias
     */
    val id: String
        get() = testId

    /**
     * 🔐 2026 Gold Standard Computed Property
     * Ensures tests with batchType "PAID" are strictly locked regardless of default boolean fallbacks.
     */
    val computedIsLocked: Boolean
        get() = isLocked || batchType.equals("PAID", ignoreCase = true) || !isFree
}