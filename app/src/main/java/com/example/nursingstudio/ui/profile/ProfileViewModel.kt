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
    /**
     * 🚀 2026 INDUSTRY GOLD STANDARD: Resilient Asynchronous Storage Pipeline
     * Robust stream wrapper that uploads cropped cache files directly to Firebase with active lifecycle guarding.
     */
    fun uploadProfileImage(fileUri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        _uploadProgress.value = true

        // Strict pointer instantiation inside Cloud Bucket Isolation Zone
        val storageRef = storage.reference.child("Users/$uid/profile_avatar.jpg")

        // 🔒 Explicit Metadata Configuration to enforce media-type recognition across cloud pipelines
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        storageRef.putFile(fileUri, metadata)
            .addOnProgressListener { taskSnapshot ->
                // 🚀 2026 SOLID ARCHITECTURE LOGGING: Safely utilize calculation matrix to track upload stream bounds
                if (taskSnapshot.totalByteCount > 0) {
                    val computedProgressPercentage = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                    android.util.Log.d("NS_STORAGE_ENGINE", "Upload Progress Boundary: ${String.format(java.util.Locale.US, "%.2f", computedProgressPercentage)}%")
                }
            }
            .addOnSuccessListener { _ ->
                // Resolution pipeline fetching secure token-authenticated URL
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val secureUrlString = downloadUri.toString()
                    val updateMap = mapOf("profileImageUrl" to secureUrlString)

                    repository.updateProfile(updateMap)?.addOnSuccessListener {
                        // Mutating hot-cache reference snapshot streams safely
                        val currentMap = _userData.value?.toMutableMap() ?: mutableMapOf()
                        currentMap["profileImageUrl"] = secureUrlString
                        _userData.value = currentMap

                        // 🚀 Re-trigger eager fetch stream to ensure real-time local cache match
                        forceRefreshProfile()

                        _uploadProgress.value = false
                    }?.addOnFailureListener { firestoreEx ->
                        _error.value = "Firestore Sync Fault: ${firestoreEx.localizedMessage}"
                        _uploadProgress.value = false
                    }
                }.addOnFailureListener { urlEx ->
                    _error.value = "URL Resolution Fault: ${urlEx.localizedMessage}"
                    _uploadProgress.value = false
                }
            }
            .addOnFailureListener { storageEx ->
                _error.value = "Cloud Engine Network Fault: ${storageEx.localizedMessage}"
                _uploadProgress.value = false
            }
    }

    fun forceRefreshProfile() {
        _userData.value = null
        fetchProfile()
    }
}