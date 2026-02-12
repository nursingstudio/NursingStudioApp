package com.example.nursingstudio.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class BiometricSettingsManager(context: Context) {

    // World-class AES256 encryption setup
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "nursing_studio_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Check if biometric is enabled by user
    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)

    // Save preference
    fun setBiometricEnabled(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", isEnabled).apply()
    }

    // Save encrypted credentials (Returning user logic)
    fun saveCredentials(email: String, pass: String) {
        sharedPreferences.edit().putString("saved_email", email).apply()
        sharedPreferences.edit().putString("saved_pass", pass).apply()
    }

    fun getSavedEmail(): String? = sharedPreferences.getString("saved_email", null)
    fun getSavedPass(): String? = sharedPreferences.getString("saved_pass", null)


    // MPIN Save karne ke liye
    fun saveMPIN(mpin: String) {
        sharedPreferences.edit().putString("user_mpin", mpin).apply()
    }

    // MPIN Get karne ke liye
    fun getMPIN(): String? = sharedPreferences.getString("user_mpin", null)

    // Check if MPIN is set
    fun isMPINSet(): Boolean = sharedPreferences.contains("user_mpin")

}