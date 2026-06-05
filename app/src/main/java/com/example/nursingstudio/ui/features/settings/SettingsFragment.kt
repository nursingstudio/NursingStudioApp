package com.example.nursingstudio.ui.features.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import androidx.navigation.fragment.findNavController
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

    // 🚀 2026 Segregated Native Switch Bindings
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

        // Find standard views
        val switchNotifications = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val switchQuizSound = view.findViewById<SwitchCompat>(R.id.switch_quiz_sound)
        val switchMotivation = view.findViewById<SwitchCompat>(R.id.switch_motivation)
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switch_vibration)

        // Find 2026 Premium Modular Security Views
        switchFingerprint = view.findViewById(R.id.switch_fingerprint)
        switchFaceUnlock = view.findViewById(R.id.switch_face_unlock)
        switchMpin = view.findViewById(R.id.switch_mpin)

        // Load states
        switchNotifications.isChecked = sp.getBoolean(KEY_NOTIFICATIONS, true)
        switchQuizSound.isChecked = sp.getBoolean(KEY_QUIZ_SOUND, true)
        switchMotivation.isChecked = sp.getBoolean(KEY_MOTIVATION, true)
        switchVibration.isChecked = AppSettings.isVibrationEnabled(requireContext())

        // Sync Initial Biometric Security Values
        syncSecuritySwitchStates()

        // Core Listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_NOTIFICATIONS, isChecked) } }
        switchQuizSound.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_QUIZ_SOUND, isChecked) } }
        switchMotivation.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_MOTIVATION, isChecked) } }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setVibration(requireContext(), isChecked)
            if (isChecked) AppSettings.triggerVibration(requireContext(), 50)
        }

        // --- 🛡️ EXPLICIT GRANULAR FINGERPRINT COMPONENT AUTH ---
        switchFingerprint?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                evaluateSecurityCascade {
                    // 🚀 2026 HARDWARE REQUIREMENT: Force hardware layer biometric evaluation before checking flag
                    triggerNativeHardwareVerification("Verify Fingerprint identity to complete initialization") {
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

        // --- 🛡️ EXPLICIT GRANULAR FACE UNLOCK COMPONENT AUTH ---
        switchFaceUnlock?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                evaluateSecurityCascade {
                    // 🚀 Trigger native device execution for hardware identification verification
                    triggerNativeHardwareVerification("Authenticate face geometric matrix profile") {
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
        // --- 🛡️ EXPLICIT FALLBACK MPIN DEPLOYMENT MATRIX ---
        switchMpin?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                it.isChecked = false
                showPasswordVerificationSheet()
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    dataStoreManager.saveMpinStatus(false)
                    switchMpin?.isChecked = false
                    toast("Secure Fallback MPIN deconfigured")
                }
            }
        }

        // Setup Standard Static Links
        view.findViewById<Button>(R.id.btnRateApp).setOnClickListener { openUrl(URL_PLAYSTORE) }
        view.findViewById<Button>(R.id.btnWhatsappSupport).setOnClickListener { openWhatsappSupport() }
        view.findViewById<Button>(R.id.btnPrivacyPolicy).setOnClickListener { openStaticPage("privacy") }
        view.findViewById<Button>(R.id.btnTerms).setOnClickListener { openStaticPage("terms") }
        view.findViewById<Button>(R.id.btnDisclaimer).setOnClickListener { openStaticPage("disclaimer") }
        view.findViewById<Button>(R.id.btnAbout).setOnClickListener { openStaticPage("about") }
    }

    private fun syncSecuritySwitchStates() {
        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        switchFingerprint?.isChecked = bioManager.isBiometricEnabled()
        switchFaceUnlock?.isChecked = sp.getBoolean("enable_face_identity_secure", false)

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
                showPasswordVerificationSheet()
            } else {
                onVerificationSuccess.invoke()
            }
        }
    }

    private fun showPasswordVerificationSheet() {
        val type = bioManager.getLoginType()
        if (type == 1 || bioManager.getSavedPass() == "OTP_USER") {
            showMPINSetupDialogFromSettings()
            return
        }

        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val parentView = view as? ViewGroup
        val sheetView = layoutInflater.inflate(R.layout.layout_verify_for_biometric, parentView, false)
        val etPass = sheetView.findViewById<TextInputEditText>(R.id.etVerifyPassword)
        val btnVerify = sheetView.findViewById<MaterialButton>(R.id.btnVerifyAndSetMPIN)

        btnVerify.setOnClickListener {
            val password = etPass.text.toString().trim()
            if (password.isNotEmpty()) {
                val savedPass = bioManager.getSavedPass()
                if (password == savedPass || savedPass == "OTP_USER") {
                    dialog.dismiss()
                    showMPINSetupDialogFromSettings()
                } else {
                    toast("Incorrect Account Password")
                }
            }
        }
        dialog.setContentView(sheetView)
        dialog.show()
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

                        // Auto-enable fingerprint on MPIN initialization for smoother experience
                        bioManager.setBiometricEnabled(true)

                        syncSecuritySwitchStates()
                        toast("Security Profile Updated Successfully! 🔒")
                    }
                } else {
                    toast("MPIN execution requires exactly 4 digits")
                }
            }
            .setNegativeButton("Dismiss", null)
            .show()
    }

    private fun openUrl(url: String) {
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

    private fun triggerNativeHardwareVerification(promptTitle: String, onAuthSuccess: () -> Unit) {
        val executor = androidx.core.content.ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    activity?.runOnUiThread { onAuthSuccess.invoke() }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity?.runOnUiThread { toast("Hardware verification failed: $errString") }
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle(promptTitle)
            .setSubtitle("Required authentication component sequence active.")
            .setNegativeButtonText("Cancel Execution")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}