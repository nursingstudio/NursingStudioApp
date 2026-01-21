package com.example.nursingstudio.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginStatus = MutableLiveData<LoginResult>()
    val loginStatus: LiveData<LoginResult> get() = _loginStatus

    fun loginWithEmail(email: String, pass: String) {
        _loginStatus.value = LoginResult.Loading
        repository.signInWithEmail(email, pass).addOnSuccessListener { res ->
            // Email login ke liye abhi bhi purana verifyUser kaam karega
            verifyUser(res.user?.uid)
        }.addOnFailureListener { e ->
            _loginStatus.value = LoginResult.Error(e.localizedMessage ?: "Login Failed")
        }
    }

    fun loginWithPhone(credential: AuthCredential) {
        viewModelScope.launch {
            _loginStatus.value = LoginResult.Loading
            // Naya Repository function call
            val result = repository.verifyOtp(credential)

            result.onSuccess { pair ->
                val isProfileComplete = pair.second
                if (isProfileComplete) {
                    _loginStatus.value = LoginResult.Success
                } else {
                    _loginStatus.value = LoginResult.NoProfile
                }
            }.onFailure { e ->
                _loginStatus.value = LoginResult.Error(e.localizedMessage ?: "Verification Failed")
            }
        }
    }

    private fun verifyUser(uid: String?) {
        if (uid == null) { _loginStatus.value = LoginResult.Error("User ID not found"); return }
        repository.checkUserInFirestore(uid).addOnSuccessListener { doc ->
            if (doc.exists()) _loginStatus.value = LoginResult.Success
            else _loginStatus.value = LoginResult.NoProfile
        }.addOnFailureListener { _loginStatus.value = LoginResult.Error("Database Error") }
    }
}

sealed class LoginResult {
    object Loading : LoginResult()
    object Success : LoginResult()
    object NoProfile : LoginResult()
    data class Error(val message: String) : LoginResult()
}