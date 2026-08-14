package com.example.nursingstudio.data.model

import androidx.annotation.Keep

/**
 * 🚀 2026 Industry Standard Test Model
 * Represents an individual Test Series inside a Category.
 */
@Keep
data class TestItem(
    val testId: String = "",
    val title: String = "",
    val categoryId: String = "",
    val seriesName: String = "",
    val totalDurationMinutes: Int = 60,
    val totalQuestions: Int = 100,
    val batchType: String = "FREE",
    @field:JvmField val isLocked: Boolean = false,
    @field:JvmField val isFree: Boolean = true
)