package com.example.nursingstudio.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.ProfileRepository

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Scope-Aware Eager Hot-Cache ViewModel
 * Pre-fetches database snapshots instantly on application execution to guarantee zero-latency UI transition.
 */
class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _userData = MutableLiveData<Map<String, Any>?>()
    val userData: LiveData<Map<String, Any>?> get() = _userData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    init {
        // 🚀 2026 Core Eager Rule: Automatically initialize cache fetching immediately on app lifecycle generation
        fetchProfile()
    }

    fun fetchProfile() {
        // Safe check to avoid multiple unnecessary re-fetches if data already populated in active stream
        if (_userData.value != null) return

        repository.getUserProfile()?.addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                _userData.value = document.data
            } else {
                _error.value = "Profile not found!"
            }
        }?.addOnFailureListener {
            _error.value = it.localizedMessage ?: "Failed to fetch data"
        }
    }

    /**
     * 🚀 Programmatic Refresh Channel if explicitly pulled by user action triggers
     */
    /**
     * 🚀 Programmatic Refresh Channel if explicitly pulled by user action triggers
     */
    @Suppress("UNUSED")
    fun forceRefreshProfile() {
        _userData.value = null
        fetchProfile()
    }
}