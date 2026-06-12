package com.example.nursingstudio.data.model

import com.google.firebase.Timestamp

// ✅ 2026 Industry Model Style
data class User(
    val uid: String = "",
    val fullName: String = "",
    val gender: String = "",
    val dob: String = "",
    val maritalStatus: String = "",
    val religion: String = "",
    val education: String = "",
    val occupation: String = "",
    val mobile: String = "",
    val email: String = "",
    val country: String = "",
    val state: String = "",
    val district: String = "",
    val address: String = "",
    val pincode: String = "",
    val isNursingRegistered: Boolean? = null,
    val regState: String? = null,
    val regNumber: String? = null,
    // 🚀 2026 Gold Standard: Architecture-safe nullable timestamp container
    val createdAt: Timestamp? = null
)