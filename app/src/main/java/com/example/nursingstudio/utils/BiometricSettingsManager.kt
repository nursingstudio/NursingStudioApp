package com.example.nursingstudio.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

class BiometricSettingsManager(private val context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "nursing_studio_secure_prefs"
        private const val KEY_LOGIN_TYPE = "login_type"
    }

    // ⭐ 2026 Professional Self-Healing Prefs Engine
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            createSafePrefs()
        } catch (_: Exception) {
            // Agar Keystore corrupt hai, toh purana file delete karke naya banao
            context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
            createSafePrefs()
        }
    }

    private fun createSafePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getLoginType() = sharedPreferences.getInt(KEY_LOGIN_TYPE, 0)

    fun isBiometricEnabled() = sharedPreferences.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) = sharedPreferences.edit {
        putBoolean("biometric_enabled", enabled)
    }

    // 🚀 2026 SECURE FACE IDENTITY SUBSYSTEM FLAGS
    fun isFaceUnlockEnabled() = sharedPreferences.getBoolean("face_unlock_enabled", false)
    fun setFaceUnlockEnabled(enabled: Boolean) = sharedPreferences.edit {
        putBoolean("face_unlock_enabled", enabled)
    }

    fun getSavedEmail() = sharedPreferences.getString("saved_email", null)
    fun getSavedPass() = sharedPreferences.getString("saved_pass", "OTP_USER") ?: "OTP_USER"

    fun saveMPIN(mpin: String) = sharedPreferences.edit { putString("user_mpin", mpin) }
    fun getMPIN() = sharedPreferences.getString("user_mpin", null)
    fun isMPINSet() = sharedPreferences.contains("user_mpin")

    fun clearSecurityHardwareSettings() = sharedPreferences.edit {
        remove("user_mpin")
        remove("biometric_enabled")
        remove("face_unlock_enabled")
    }
}