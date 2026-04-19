package com.example.nursingstudio.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow // ✅ New Import
import kotlinx.coroutines.flow.StateFlow        // ✅ New Import
import kotlinx.coroutines.flow.asStateFlow     // ✅ New Import
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // 1. Single StateFlow for the entire Register Screen
    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    // 2. Start Registration Process
    fun startRegistration(email: String, pass: String, userData: Map<String, Any>) {
        _uiState.value = RegisterState.Loading

        viewModelScope.launch {
            repository.createUser(email, pass).onSuccess { authResult ->
                val user = authResult.user
                val uid = user?.uid ?: ""

                // ✅ Fix: UID match ensure karna aur data sync karna
                val finalData = userData.toMutableMap().apply {
                    put("uid", uid)
                    put("authProvider", "email")
                }

                repository.saveUserData(uid, finalData).onSuccess {
                    _uiState.value = RegisterState.Success
                }.onFailure { e ->
                    // Rollback: Agar Firestore fail hua toh Auth user delete karo
                    user?.delete()
                    _uiState.value = RegisterState.Error("Database Error: ${e.localizedMessage}")
                }
            }.onFailure { e ->
                _uiState.value = RegisterState.Error(e.localizedMessage ?: "Registration Failed")
            }
        }
    }


    // 3. OTP Verification
    fun verifyOtp(credential: AuthCredential) {
        viewModelScope.launch {
            _uiState.value = RegisterState.Loading
            repository.verifyOtp(credential).onSuccess {
                _uiState.value = RegisterState.Success
            }.onFailure { e ->
                _uiState.value = RegisterState.Error(e.localizedMessage ?: "Invalid OTP")
            }
        }
    }

    // 4. Reset State (Essential for Clean Architecture)
    fun resetState() {
        _uiState.value = RegisterState.Idle
    }

    // ⭐ 2026 Gold Standard: Unified Sealed Interface
    sealed interface RegisterState {
        object Idle : RegisterState
        object Loading : RegisterState
        object Success : RegisterState
        data class Error(val message: String) : RegisterState
    }
}