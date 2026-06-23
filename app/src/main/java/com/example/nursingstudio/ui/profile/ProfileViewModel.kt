package com.example.nursingstudio.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Scope-Aware Eager Hot-Cache ViewModel
 * Sanitized and fully refactored background network operation sync streams.
 */
class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userData = MutableLiveData<Map<String, Any>?>()
    val userData: LiveData<Map<String, Any>?> get() = _userData

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _uploadProgress = MutableLiveData<Boolean>()
    val uploadProgress: LiveData<Boolean> get() = _uploadProgress

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        if (_userData.value != null) return

        repository.getUserProfile()?.addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                _userData.value = document.data
            } else {
                _error.value = "Profile not found!"
            }
        }?.addOnFailureListener { exception ->
            _error.value = exception.localizedMessage ?: "Failed to fetch data"
        }
    }

    /**
     * 🚀 REFACTORED STORAGE PIPELINE: Strict type handling for Cloud Bucket Sync
     */
    fun uploadProfileImage(fileUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        _uploadProgress.value = true

        val storageRef = storage.reference.child("Users/$uid/profile_avatar.jpg")

        storageRef.putFile(fileUri)
            .addOnSuccessListener { _ ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val updateMap = mapOf("profileImageUrl" to downloadUri.toString())
                    repository.updateProfile(updateMap)?.addOnSuccessListener {
                        val currentMap = _userData.value?.toMutableMap() ?: mutableMapOf()
                        currentMap["profileImageUrl"] = downloadUri.toString()
                        _userData.value = currentMap

                        // 🚀 RESOLVED WARNING & FORCE REFRESH: Re-syncing database network states instantly
                        forceRefreshProfile()

                        _uploadProgress.value = false
                    }?.addOnFailureListener { firestoreEx ->
                        _error.value = "Firestore Link Update Error: ${firestoreEx.localizedMessage}"
                        _uploadProgress.value = false
                    }
                }.addOnFailureListener { urlEx ->
                    _error.value = "URL Resolution Fault: ${urlEx.localizedMessage}"
                    _uploadProgress.value = false
                }
            }
            .addOnFailureListener { storageEx ->
                _error.value = "Storage Engine Fault: ${storageEx.localizedMessage}"
                _uploadProgress.value = false
            }
    }

    fun forceRefreshProfile() {
        _userData.value = null
        fetchProfile()
    }
}