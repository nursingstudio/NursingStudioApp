package com.example.nursingstudio.ui.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.User
import com.example.nursingstudio.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val uiState: StateFlow<RegisterState> = _uiState.asStateFlow()

    fun startRegistration(email: String, pass: String, userModel: User) {
        _uiState.value = RegisterState.Loading

        viewModelScope.launch {
            repository.createUser(email, pass).onSuccess { authResult ->
                val uid = authResult.user?.uid ?: ""
                val finalUser = userModel.copy(uid = uid)

                repository.saveUserData(uid, finalUser).onSuccess {
                    _uiState.value = RegisterState.Success
                }.onFailure { e ->
                    // 🚀 CRITICAL CLEANUP: Independent transaction fallback layer preventing ghost auth accumulation
                    try {
                        authResult.user?.delete()
                    } catch (cleanupException: Exception) {
                        cleanupException.printStackTrace()
                    }
                    _uiState.value = RegisterState.Error("Database Error: ${e.localizedMessage}")
                }
            }.onFailure { e ->
                _uiState.value = RegisterState.Error(e.localizedMessage ?: "Auth Failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterState.Idle
    }

    sealed interface RegisterState {
        object Idle : RegisterState
        object Loading : RegisterState
        object Success : RegisterState
        data class Error(val message: String) : RegisterState
    }
}