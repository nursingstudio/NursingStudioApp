package com.example.nursingstudio.ui.features.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputFilter
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    companion object {
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "enable_notifications"
        private const val KEY_QUIZ_SOUND = "enable_quiz_sound"
        private const val KEY_MOTIVATION = "enable_motivation"

        private const val URL_PLAYSTORE = "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
        private const val URL_WHATSAPP_SUPPORT = "https://wa.me/919999999999?text=Hello%20Nursing%20Studio%20Support"
    }

    private lateinit var bioManager: BiometricSettingsManager
    private lateinit var dataStoreManager: DataStoreManager

    private var switchFingerprint: SwitchMaterial? = null
    private var switchFaceUnlock: SwitchMaterial? = null
    private var switchMpin: SwitchMaterial? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bioManager = BiometricSettingsManager(requireContext())
        dataStoreManager = DataStoreManager(requireContext())
        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

        val switchNotifications = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val switchQuizSound = view.findViewById<SwitchCompat>(R.id.switch_quiz_sound)
        val switchMotivation = view.findViewById<SwitchCompat>(R.id.switch_motivation)
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switch_vibration)

        switchFingerprint = view.findViewById(R.id.switch_fingerprint)
        switchFaceUnlock = view.findViewById(R.id.switch_face_unlock)
        switchMpin = view.findViewById(R.id.switch_mpin)

        switchNotifications.isChecked = sp.getBoolean(KEY_NOTIFICATIONS, true)
        switchQuizSound.isChecked = sp.getBoolean(KEY_QUIZ_SOUND, true)
        switchMotivation.isChecked = sp.getBoolean(KEY_MOTIVATION, true)
        switchVibration.isChecked = AppSettings.isVibrationEnabled(requireContext())

        syncSecuritySwitchStates()

        switchNotifications.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_NOTIFICATIONS, isChecked) } }
        switchQuizSound.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_QUIZ_SOUND, isChecked) } }
        switchMotivation.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_MOTIVATION, isChecked) } }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setVibration(requireContext(), isChecked)
            if (isChecked) AppSettings.triggerVibration(requireContext(), 50)
        }

        switchFingerprint?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                if (!isBiometricHardwareEnrolled()) {
                    showHardwareEnrollmentDialog("Fingerprint")
                    return@setOnClickListener
                }
                evaluateSecurityCascade {
                    triggerNativeHardwareVerification(
                        promptTitle = "Confirm Fingerprint",
                        promptSubtitle = "Please scan your fingerprint to enable quick access.",
                        isFaceVerification = false
                    ) {
                        bioManager.setBiometricEnabled(true)
                        switchFingerprint?.isChecked = true
                        toast("Fingerprint Authentication Enabled 🔒")
                    }
                }
            } else {
                bioManager.setBiometricEnabled(false)
                toast("Fingerprint validation offline")
            }
        }

        switchFaceUnlock?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                if (!isBiometricHardwareEnrolled()) {
                    showHardwareEnrollmentDialog("Face Lock")
                    return@setOnClickListener
                }
                evaluateSecurityCascade {
                    triggerNativeHardwareVerification(
                        promptTitle = "Confirm Face Identity",
                        promptSubtitle = "Please scan your face to enable quick access.",
                        isFaceVerification = true
                    ) {
                        bioManager.setFaceUnlockEnabled(true)
                        switchFaceUnlock?.isChecked = true
                        toast("Secure Face ID Profile Synchronized 🎯")
                    }
                }
            } else {
                bioManager.setFaceUnlockEnabled(false)
                switchFaceUnlock?.isChecked = false
                toast("Face ID profile detached")
            }
        }

        switchMpin?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                showMPINSetupDialogFromSettings()
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    bioManager.clearSecurityHardwareSettings()
                    dataStoreManager.saveMpinStatus(false)
                    switchMpin?.isChecked = false
                    switchFingerprint?.isChecked = false
                    switchFaceUnlock?.isChecked = false
                    toast("Secure Fallback MPIN & Hardware Tokens Cleared")
                }
            }
        }

        view.findViewById<Button>(R.id.btnRateApp).setOnClickListener { openUrl() }
        view.findViewById<Button>(R.id.btnWhatsappSupport).setOnClickListener { openWhatsappSupport() }
        view.findViewById<Button>(R.id.btnPrivacyPolicy).setOnClickListener { openStaticPage("privacy") }
        view.findViewById<Button>(R.id.btnTerms).setOnClickListener { openStaticPage("terms") }
        view.findViewById<Button>(R.id.btnDisclaimer).setOnClickListener { openStaticPage("disclaimer") }
        view.findViewById<Button>(R.id.btnAbout).setOnClickListener { openStaticPage("about") }
    }

    private fun syncSecuritySwitchStates() {
        switchFingerprint?.isChecked = bioManager.isBiometricEnabled()
        switchFaceUnlock?.isChecked = bioManager.isFaceUnlockEnabled()

        viewLifecycleOwner.lifecycleScope.launch {
            val isMpinActive = dataStoreManager.isMpinSet.firstOrNull() ?: false
            switchMpin?.isChecked = isMpinActive
        }
    }

    private fun evaluateSecurityCascade(onVerificationSuccess: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val isMpinActive = dataStoreManager.isMpinSet.firstOrNull() ?: false
            if (!isMpinActive) {
                toast("Please set your Fallback MPIN first!")
                showMPINSetupDialogFromSettings()
            } else {
                onVerificationSuccess.invoke()
            }
        }
    }

    private fun showMPINSetupDialogFromSettings() {
        val etMpin = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Create 4-Digit PIN"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Configure Security PIN")
            .setMessage("This PIN serves as your identity validation backup.")
            .setView(etMpin)
            .setCancelable(false)
            .setPositiveButton("Save PIN") { _, _ ->
                val mpin = etMpin.text.toString()
                if (mpin.length == 4) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        bioManager.saveMPIN(mpin)
                        dataStoreManager.saveMpinStatus(true)
                        syncSecuritySwitchStates()
                        toast("Security PIN Created Successfully! 🔒")
                    }
                } else {
                    toast("MPIN execution requires exactly 4 digits")
                }
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun openUrl(url: String = URL_PLAYSTORE) {
        try { startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        catch (_: Exception) { toast("Unable to open dynamic redirection asset") }
    }

    private fun openWhatsappSupport() {
        try { startActivity(Intent(Intent.ACTION_VIEW, URL_WHATSAPP_SUPPORT.toUri())) }
        catch (_: Exception) { toast("WhatsApp communication platform mismatch") }
    }

    private fun openStaticPage(type: String) {
        if (!isAdded) return
        try {
            val bundle = Bundle().apply { putString("page_type", type) }
            findNavController().navigate(R.id.nav_static_page, bundle)
        } catch (_: Exception) {
            toast("Redirection fault inside layout navigation manager")
        }
    }

    private fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isBiometricHardwareEnrolled(authenticatorType: Int = BiometricManager.Authenticators.BIOMETRIC_STRONG): Boolean {
        val biometricManager = BiometricManager.from(requireContext())
        return biometricManager.canAuthenticate(authenticatorType) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showHardwareEnrollmentDialog(featureName: String, settingsAction: String = Settings.ACTION_SECURITY_SETTINGS) {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("$featureName Not Found")
            .setMessage("Your device doesn't have $featureName set up yet. Please configure it in your phone's system settings first.")
            .setCancelable(true)
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(settingsAction))
                } catch (_: Exception) {
                    try {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    } catch (_: Exception) {
                        toast("Unable to open device settings framework")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun triggerNativeHardwareVerification(
        promptTitle: String,
        promptSubtitle: String,
        isFaceVerification: Boolean,
        onAuthSuccess: () -> Unit
    ) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(requireContext())
        var wrongAttemptCount = 0

        var biometricPrompt: BiometricPrompt? = null

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)

                    val hardwareAuthenticationType = result.authenticationType

                    if (hardwareAuthenticationType == BiometricPrompt.AUTHENTICATION_RESULT_TYPE_DEVICE_CREDENTIAL) {
                        biometricPrompt?.cancelAuthentication()
                        activity?.runOnUiThread {
                            toast("Security Policy: Device PIN/Pattern fallback authentication restricted here.")
                        }
                        return
                    }

                    wrongAttemptCount = 0
                    activity?.runOnUiThread { onAuthSuccess.invoke() }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    wrongAttemptCount++
                    if (wrongAttemptCount >= 3) {
                        biometricPrompt?.cancelAuthentication()
                        wrongAttemptCount = 0
                        activity?.runOnUiThread {
                            toast("Too many failed attempts. Security prompt dismissed.")
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity?.runOnUiThread { toast("Verification cancelled: $errString") }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle(promptSubtitle)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}