package com.example.nursingstudio.auth.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.AuthCredential

class RegisterViewModel : ViewModel() {
    private val repository = RegisterRepository()

    // UI ko batane ke liye variables
    private val _regStatus = MutableLiveData<RegResult>()
    val regStatus: LiveData<RegResult> get() = _regStatus

    fun startRegistration(email: String, pass: String, phoneCred: AuthCredential, userData: MutableMap<String, Any>) {
        _regStatus.value = RegResult.Loading

        // 1. Create User
        repository.createUser(email, pass).addOnSuccessListener { res ->
            val user = res.user

            // 2. Link Phone
            repository.linkPhone(phoneCred)?.addOnCompleteListener { linkTask ->

                // 3. Save to Firestore
                userData["uid"] = user!!.uid // UID update karo
                repository.saveUserData(user.uid, userData).addOnSuccessListener {
                    _regStatus.value = RegResult.Success
                }.addOnFailureListener { e ->
                    _regStatus.value = RegResult.Error(e.localizedMessage ?: "Firestore Error")
                }
            }
        }.addOnFailureListener { e ->
            _regStatus.value = RegResult.Error(e.localizedMessage ?: "Auth Error")
        }
    }
}

// Result handle karne ke liye ek sealed class
sealed class RegResult {
    object Loading : RegResult()
    object Success : RegResult()
    data class Error(val message: String) : RegResult()
}