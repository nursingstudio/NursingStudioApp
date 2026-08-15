package com.example.nursingstudio.data.model

import androidx.annotation.Keep

/**
 * 🚀 2026 Industry Gold Standard: Unified Access Control Model
 * Fully backward-compatible and multi-field safe data layer.
 */
@Keep
data class MediaItemModel(
    val id: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val type: String = "", // "PDF" or "VIDEO"
    val accessType: String = "FREE", // "FREE" or "PAID"
    val videoType: String = "FREE",  // Legacy fallback support
    val pdfType: String = "FREE", // Legacy fallback support
    val batchType: String = "FREE",
    val streamType: String = "RECORDED"
) {
    /**
     * 🔐 Strict Multi-Field Lock Evaluation
     * Checks all potential Firestore field variants so paid PDFs and Videos are never leaked.
     */
    val computedIsLocked: Boolean
        get() = accessType.equals("PAID", ignoreCase = true) ||
                videoType.equals("PAID", ignoreCase = true) ||
                pdfType.equals("PAID", ignoreCase = true) ||
                batchType.equals("PAID", ignoreCase = true)
}
