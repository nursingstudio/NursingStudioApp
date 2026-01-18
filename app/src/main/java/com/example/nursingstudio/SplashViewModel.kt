package com.example.nursingstudio

import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.AuthRepository

class SplashViewModel : ViewModel() {
    private val repository = AuthRepository() // Bilkul sahi!

    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }
}