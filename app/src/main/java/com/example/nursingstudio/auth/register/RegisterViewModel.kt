package com.example.nursingstudio.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential

class RegisterViewModel : ViewModel() {
    private val repository = RegisterRepository()
    private val _regStatus = MutableLiveData<RegResult>()
    val regStatus: LiveData<RegResult> get() = _regStatus

    fun startRegistration(email: String, pass: String, phoneCred: AuthCredential, userData: MutableMap<String, Any>) {
        _regStatus.value = RegResult.Loading
        repository.createUser(email, pass).addOnSuccessListener { res ->
            val user = res.user
            repository.linkPhone(phoneCred)?.addOnCompleteListener { linkTask ->
                if (linkTask.isSuccessful) {
                    userData["uid"] = user!!.uid
                    repository.saveUserData(user.uid, userData).addOnSuccessListener {
                        _regStatus.value = RegResult.Success
                    }.addOnFailureListener { e -> _regStatus.value = RegResult.Error(e.localizedMessage ?: "DB Error") }
                } else { _regStatus.value = RegResult.Error("Phone Linking Failed") }
            }
        }.addOnFailureListener { e -> _regStatus.value = RegResult.Error(e.localizedMessage ?: "Auth Error") }
    }
}

sealed class RegResult {
    object Loading : RegResult()
    object Success : RegResult()
    data class Error(val message: String) : RegResult()
}