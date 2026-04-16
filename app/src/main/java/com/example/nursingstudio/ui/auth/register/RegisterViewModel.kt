package com.example.nursingstudio.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel // ✅ 2026 Gold Standard Annotation
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository // ✅ Injecting Repository via Hilt
) : ViewModel() {

    private val _regStatus = MutableLiveData<RegResult>()
    val regStatus: LiveData<RegResult> get() = _regStatus

    private val _otpStatus = MutableLiveData<OtpResult>()
    val otpStatus: LiveData<OtpResult> get() = _otpStatus

    fun startRegistration(email: String, pass: String, userData: MutableMap<String, Any>) {
        viewModelScope.launch {
            _regStatus.value = RegResult.Loading

            // Using the Result pattern from Repository
            repository.createUser(email, pass).onSuccess { authResult ->
                val uid = authResult.user?.uid ?: ""
                userData["uid"] = uid

                repository.saveUserData(uid, userData).onSuccess {
                    _regStatus.value = RegResult.Success
                }.onFailure { e ->
                    _regStatus.value = RegResult.Error(e.localizedMessage ?: "Firestore Save Failed")
                }
            }.onFailure { e ->
                _regStatus.value = RegResult.Error(e.localizedMessage ?: "Auth Failed")
            }
        }
    }

    fun verifyOtp(credential: AuthCredential) {
        viewModelScope.launch {
            _otpStatus.value = OtpResult.Loading
            repository.verifyOtp(credential).onSuccess {
                _otpStatus.value = OtpResult.Success
            }.onFailure { e ->
                _otpStatus.value = OtpResult.Error(e.localizedMessage ?: "Invalid OTP")
            }
        }
    }
}

// Sealed classes for UI State (Keep them as you have)
sealed class RegResult {
    object Loading : RegResult()
    object Success : RegResult()
    data class Error(val message: String) : RegResult()
}

sealed class OtpResult {
    object Loading : OtpResult()
    object Success : OtpResult()
    data class Error(val message: String) : OtpResult()
}