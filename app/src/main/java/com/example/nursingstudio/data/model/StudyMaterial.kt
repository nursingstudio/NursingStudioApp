package com.example.nursingstudio.data.model

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Unified Cloud Resource Metadata Structure
 */
data class StudyMaterial(
    val id: String = "",
    val title: String = "",
    val downloadUrl: String = "",
    val fileSize: String = "",
    val duration: String = "", // Used only for video structures
    val uploadedAt: Long = 0L,
    val type: String = "" // "PDF" or "VIDEO"
)