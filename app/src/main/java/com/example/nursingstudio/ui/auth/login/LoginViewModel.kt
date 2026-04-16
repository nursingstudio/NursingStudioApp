package com.example.nursingstudio.ui.auth.login

import androidx.lifecycle.ViewModel // ✅ Changed from AndroidViewModel
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
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository // ✅ Application context removed from constructor
) : ViewModel() {

    // 1. MutableStateFlow: Private for internal updates (Gold Standard 2026)
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)

    // 2. StateFlow: Public for UI to collect (Read-only)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            repository.signInWithEmail(email, pass).onSuccess { authResult ->
                val uid = authResult.user?.uid ?: ""
                val exists = repository.checkUserById(uid)

                if (exists) {
                    _loginState.value = LoginState.Success
                } else {
                    // ✅ SignOut logic now handled inside repository to keep ViewModel clean
                    repository.signOut()
                    _loginState.value = LoginState.NoProfile
                }
            }.onFailure { e ->
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Login Failed")
            }
        }
    }

    fun loginWithPhone(credential: AuthCredential) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            repository.verifyAndSyncUser(credential).onSuccess { (_, isRegistered) ->
                if (isRegistered) {
                    _loginState.value = LoginState.Success
                } else {
                    repository.signOut()
                    _loginState.value = LoginState.NoProfile
                }
            }.onFailure { e ->
                _loginState.value = LoginState.Error(e.localizedMessage ?: "OTP Failed")
            }
        }
    }

    // 3. Reset State: Helpful for 2026 complex UI flows
    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    // Modern Sealed Interface for state management
    sealed interface LoginState {
        object Idle : LoginState
        object Loading : LoginState
        object Success : LoginState
        object NoProfile : LoginState
        data class Error(val message: String) : LoginState
    }
}