package com.example.nursingstudio.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class BiometricAuthHelper(private val fragment: Fragment) {

    private val context: Context = fragment.requireContext()
    private val executor = ContextCompat.getMainExecutor(context)

    fun checkBiometricAvailability(): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    }

    fun launchHardwareEnrollment() {
        val enrollIntent = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
            putExtra(
                Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            )
        }
        try {
            fragment.startActivity(enrollIntent)
        } catch (_: Exception) {
            try {
                fragment.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            } catch (_: Exception) {
                fragment.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
    }

    fun triggerAuthentication(
        title: String,
        subtitle: String,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (CharSequence) -> Unit
    ) {
        // 🚀 FIXED: Enforced structured runtime verification checks to cleanly completely prevent dead asynchronous host callback exceptions
        if (!fragment.isAdded || fragment.view == null) {
            onError("Authentication host lifecycle state is invalid.")
            return
        }

        val biometricPrompt = BiometricPrompt(fragment, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Check instance attachment status before committing state transitions backwards
                    if (fragment.isAdded) onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (fragment.isAdded) onError(errString)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use App MPIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}