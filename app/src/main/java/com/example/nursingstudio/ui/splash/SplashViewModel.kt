package com.example.nursingstudio.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel // ✅ Added
import kotlinx.coroutines.flow.MutableStateFlow // ✅ Added
import kotlinx.coroutines.flow.asStateFlow // ✅ Added
import kotlinx.coroutines.launch
import javax.inject.Inject // ✅ Added

@HiltViewModel // ✅ 2026 Gold Standard
class SplashViewModel @Inject constructor(
    private val repository: AuthRepository // ✅ Injected perfectly by Hilt
) : ViewModel() {

    private val _isLoggedIn = MutableStateFlow<Boolean?>(null)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    fun checkUserSession() {
        viewModelScope.launch {
            // Hum direct repo call karenge, Context ki chinta Hilt ne kar li hai
            _isLoggedIn.value = repository.isUserLoggedIn()
        }
    }
}