package com.example.nursingstudio.ui.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _regStatus = MutableLiveData<RegResult>()
    val regStatus: LiveData<RegResult> get() = _regStatus

    private val _otpStatus = MutableLiveData<OtpResult>()
    val otpStatus: LiveData<OtpResult> get() = _otpStatus

    fun startRegistration(email: String, pass: String, phoneCred: AuthCredential, userData: MutableMap<String, Any>) {
        viewModelScope.launch {
            _regStatus.value = RegResult.Loading
            try {
                val authResult = repository.createUser(email, pass).await()
                val user = authResult.user ?: throw Exception("User creation failed")

                repository.linkPhone(phoneCred)?.await() ?: throw Exception("Linking failed")

                userData["uid"] = user.uid
                repository.saveUserData(user.uid, userData).await()

                _regStatus.value = RegResult.Success
            } catch (e: Exception) {
                _regStatus.value = RegResult.Error(e.localizedMessage ?: "Something went wrong")
            }
        }
    }

    // --- YE WALA FUNCTION MISSING THA YA GALAT THA ---
    fun verifyOtp(credential: AuthCredential) {
        viewModelScope.launch {
            _otpStatus.value = OtpResult.Loading
            val result = repository.verifyOtp(credential) // AuthRepository wala call
            if (result.isSuccess) {
                _otpStatus.value = OtpResult.Success
            } else {
                _otpStatus.value = OtpResult.Error(result.exceptionOrNull()?.message ?: "Invalid OTP")
            }
        }
    }
}

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