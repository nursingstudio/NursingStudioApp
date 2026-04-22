package com.example.nursingstudio.data.model

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
    val isNursingRegistered: Boolean = false,
    val regState: String? = null,
    val regNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)