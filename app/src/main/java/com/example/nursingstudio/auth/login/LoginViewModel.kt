package com.example.nursingstudio.auth.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential

class LoginViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _loginStatus = MutableLiveData<LoginResult>()
    val loginStatus: LiveData<LoginResult> get() = _loginStatus

    fun loginWithEmail(email: String, pass: String) {
        _loginStatus.value = LoginResult.Loading
        repository.signInWithEmail(email, pass).addOnSuccessListener { res ->
            verifyUser(res.user?.uid)
        }.addOnFailureListener { e ->
            _loginStatus.value = LoginResult.Error(e.localizedMessage ?: "Login Failed")
        }
    }

    fun loginWithPhone(credential: AuthCredential) {
        _loginStatus.value = LoginResult.Loading
        repository.signInWithCredential(credential).addOnSuccessListener { res ->
            verifyUser(res.user?.uid)
        }.addOnFailureListener { _loginStatus.value = LoginResult.Error("Verification Failed") }
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