package com.example.nursingstudio.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class BiometricSettingsManager(context: Context) {

    // World-class constants setup using Companion Object
    companion object {
        private const val KEY_LOGIN_TYPE = "login_type" // 0 for Email, 1 for Mobile
        private const val SECURE_PREFS_NAME = "nursing_studio_secure_prefs"
    }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        SECURE_PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getLoginType(): Int = sharedPreferences.getInt(KEY_LOGIN_TYPE, 0)

    // Biometric Preferences
    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)

    fun setBiometricEnabled(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", isEnabled).apply()
    }

    fun getSavedEmail(): String? = sharedPreferences.getString("saved_email", null)

    fun getSavedPass(): String = sharedPreferences.getString("saved_pass", "OTP_USER") ?: "OTP_USER"

    // MPIN Logic
    fun saveMPIN(mpin: String) {
        sharedPreferences.edit().putString("user_mpin", mpin).apply()
    }

    fun getMPIN(): String? = sharedPreferences.getString("user_mpin", null)

    fun isMPINSet(): Boolean = sharedPreferences.contains("user_mpin")

    // 1. Biometric Preferences ko MPIN ke sath link karein
    fun isSecurityEnabled(): Boolean {
        // Agar MPIN set hai aur biometric enabled hai
        return sharedPreferences.getBoolean("biometric_enabled", false) && isMPINSet()
    }

    // 2. Setup logic ko clean karein
    fun enableFullSecurity(emailOrMobile: String, pass: String, mpin: String) {
        sharedPreferences.edit().apply {
            putBoolean("biometric_enabled", true)
            putString("user_mpin", mpin)
            putString("saved_email", emailOrMobile)
            putString("saved_pass", pass)
            apply()
        }
    }
}