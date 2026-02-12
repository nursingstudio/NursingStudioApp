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

    // Check if biometric is enabled
    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)

    // Save preference
    fun setBiometricEnabled(isEnabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", isEnabled).apply()
        // Agar disable kiya hai, toh purana MPIN ya data clear nahi karenge jab tak user logout na kare
        // Taaki next time ON karte waqt convenience rahe
    }

    // Save encrypted credentials
    fun saveCredentials(email: String, pass: String) {
        sharedPreferences.edit()
            .putString("saved_email", email)
            .putString("saved_pass", pass)
            .apply()
    }

    fun getSavedEmail(): String? = sharedPreferences.getString("saved_email", null)

    // Yahan default "OTP_USER" diya hai safety ke liye
    fun getSavedPass(): String = sharedPreferences.getString("saved_pass", "OTP_USER") ?: "OTP_USER"

    // MPIN Logic
    fun saveMPIN(mpin: String) {
        sharedPreferences.edit().putString("user_mpin", mpin).apply()
    }

    fun getMPIN(): String? = sharedPreferences.getString("user_mpin", null)

    fun isMPINSet(): Boolean = sharedPreferences.contains("user_mpin")

    // World-Class Security: Logout par credentials clear karne ke liye
    fun clearAllSecureData() {
        sharedPreferences.edit().clear().apply()
    }
}