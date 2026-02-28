package com.example.nursingstudio.ui.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.nursingstudio.data.repository.AuthRepository

class SplashViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _navigateToNext = MutableLiveData<Boolean>()
    val navigateToNext: LiveData<Boolean> get() = _navigateToNext

    fun checkUserSession() {
        // Hum logic yahan rakhenge
        _navigateToNext.value = repository.isUserLoggedIn()
    }
}