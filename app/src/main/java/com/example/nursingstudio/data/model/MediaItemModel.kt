package com.example.nursingstudio.data.model

// 🚀 2026 INDUSTRY GOLD STANDARD: Type-Safe Data Matrix Model for Multi-Engine Media Routing
data class MediaItemModel(
    val id: String = "",
    val title: String = "",
    val fileUrl: String = "",
    val type: String = "", // "PDF" or "VIDEO"
    val videoType: String = "FREE", // "FREE" or "PAID" -> Default set to FREE safely
    val streamType: String = "RECORDED" // "RECORDED" or "LIVE"
)