package com.example.nursingstudio.utils

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 🚀 2026 World-Class High-Fidelity Cryptography Controller
 * Line 15: Removed 'private val' keyword from 'context: Context' parameter.
 * This instantly resolves the grayed-out variable and removes the compiler property warning.
 */
class BiometricSettingsManager(context: Context) {

    companion object {
        private const val SECURE_PREFS_NAME = "nursing_studio_secure_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FACE_UNLOCK_ENABLED = "face_unlock_enabled"
        private const val KEY_USER_MPIN = "user_mpin"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASS = "saved_pass"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nursing_studio_crypt_alias"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    init {
        // Initializes hardware keystore container securely during class initialization phase
        initSecureHardwareKey()
    }

    private fun initSecureHardwareKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun encryptText(text: String): String {
        if (text.isEmpty()) return ""
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, secretKey)
            }
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv

            val combined = ByteArray(1 + iv.size + encryptedBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 1 + iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) { "" }
    }

    private fun decryptText(encryptedText: String?): String? {
        if (encryptedText.isNullOrEmpty()) return null
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val ivSize = combined[0].toInt()
            val iv = ByteArray(ivSize)
            System.arraycopy(combined, 1, iv, 0, ivSize)

            val encryptedBytes = ByteArray(combined.size - 1 - ivSize)
            System.arraycopy(combined, 1 + ivSize, encryptedBytes, 0, encryptedBytes.size)

            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            }
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (_: Exception) { null }
    }

    // --- Pristine Clean Operational Functional APIs ---

    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setBiometricEnabled(enabled: Boolean) = sharedPreferences.edit {
        putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
    }

    fun isFaceUnlockEnabled(): Boolean = sharedPreferences.getBoolean(KEY_FACE_UNLOCK_ENABLED, false)

    fun setFaceUnlockEnabled(enabled: Boolean) = sharedPreferences.edit {
        putBoolean(KEY_FACE_UNLOCK_ENABLED, enabled)
    }

    fun getSavedEmail(): String? {
        val encryptedEmail = sharedPreferences.getString(KEY_SAVED_EMAIL, null)
        return decryptText(encryptedEmail)
    }

    fun getSavedPass(): String {
        val encryptedPass = sharedPreferences.getString(KEY_SAVED_PASS, "")
        return decryptText(encryptedPass) ?: ""
    }

    fun saveMPIN(mpin: String) = sharedPreferences.edit {
        putString(KEY_USER_MPIN, encryptText(mpin))
    }

    fun getMPIN(): String? {
        val encryptedMpin = sharedPreferences.getString(KEY_USER_MPIN, null)
        return decryptText(encryptedMpin)
    }

    fun isMPINSet(): Boolean = sharedPreferences.contains(KEY_USER_MPIN)

    fun clearSecurityHardwareSettings() = sharedPreferences.edit {
        remove(KEY_USER_MPIN)
        remove(KEY_BIOMETRIC_ENABLED)
        remove(KEY_FACE_UNLOCK_ENABLED)
    }
}