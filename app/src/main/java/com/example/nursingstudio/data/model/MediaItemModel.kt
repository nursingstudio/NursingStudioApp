package com.example.nursingstudio.data.model

import androidx.annotation.Keep

@Keep
data class MediaItemModel(
    val id: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val type: String = "", // "PDF" or "VIDEO"
    val videoType: String = "FREE", // "FREE" or "PAID"
    val streamType: String = "RECORDED"
) {
    /**
     * 🔐 Safe Lock Computed Evaluation
     */
    val computedIsLocked: Boolean
        get() = videoType.equals("PAID", ignoreCase = true)
}