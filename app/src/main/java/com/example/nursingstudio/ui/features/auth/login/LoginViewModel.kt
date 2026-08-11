package com.example.nursingstudio.ui.features.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
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
                    repository.signOut()
                    _loginState.value = LoginState.NoProfile
                }
            }.onFailure { e ->
                _loginState.value = LoginState.Error(e.localizedMessage ?: "Login Failed")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }

    sealed interface LoginState {
        object Idle : LoginState
        object Loading : LoginState
        object Success : LoginState
        object NoProfile : LoginState
        data class Error(val message: String) : LoginState
    }
}