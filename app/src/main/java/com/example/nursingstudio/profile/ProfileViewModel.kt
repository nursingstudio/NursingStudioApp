package com.example.nursingstudio.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    // Isme hum user ka pura data Map ki tarah rakhenge
    private val _userData = MutableLiveData<Map<String, Any>?>()
    val userData: LiveData<Map<String, Any>?> get() = _userData

    // Error handle karne ke liye
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun fetchProfile() {
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

    fun updateProfile(newData: Map<String, Any>) {
        repository.updateProfile(newData)?.addOnSuccessListener {
            // Update hone ke baad phir se fetch karlo taaki UI refresh ho jaye
            fetchProfile()
        }
    }
}