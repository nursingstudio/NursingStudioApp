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
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.databinding.FragmentSettingsBinding
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricAuthHelper
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var bioManager: BiometricSettingsManager
    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var biometricHelper: BiometricAuthHelper

    private var isSyncingSwitches = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bioManager = BiometricSettingsManager(requireContext())
        dataStoreManager = DataStoreManager(requireContext())
        biometricHelper = BiometricAuthHelper(this)

        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

        // 🚀 FIXED: Clean compiled bindings parameters to circumvent loose memory field references leaks
        binding.switchNotifications.isChecked = sp.getBoolean(KEY_NOTIFICATIONS, true)
        binding.switchQuizSound.isChecked = sp.getBoolean(KEY_QUIZ_SOUND, true)
        binding.switchMotivation.isChecked = sp.getBoolean(KEY_MOTIVATION, true)
        binding.switchVibration.isChecked = AppSettings.isVibrationEnabled(requireContext())

        syncSecuritySwitchStates()

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_NOTIFICATIONS, isChecked) } }
        binding.switchQuizSound.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_QUIZ_SOUND, isChecked) } }
        binding.switchMotivation.setOnCheckedChangeListener { _, isChecked -> sp.edit { putBoolean(KEY_MOTIVATION, isChecked) } }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setVibration(requireContext(), isChecked)
            if (isChecked) AppSettings.triggerVibration(requireContext(), 50)
        }

        binding.switchUnifiedBiometric.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isSyncingSwitches) return@setOnCheckedChangeListener
            if (isChecked) {
                val availabilityState = biometricHelper.checkBiometricAvailability()
                if (availabilityState != BiometricManager.BIOMETRIC_SUCCESS) {
                    isSyncingSwitches = true
                    buttonView.isChecked = false
                    isSyncingSwitches = false

                    if (availabilityState == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        showHardwareEnrollmentDialog()
                    } else {
                        toast("Biometric hardware is currently unavailable or unsupported.")
                    }
                    return@setOnCheckedChangeListener
                }

                evaluateSecurityCascade {
                    biometricHelper.triggerAuthentication(
                        title = "Confirm Identity",
                        subtitle = "Authenticate using your registered biometric scanner.",
                        onSuccess = {
                            bioManager.setBiometricAuthActive(true)
                            syncSecuritySwitchStates()
                            toast("Biometric Login Enabled Securely 🔒")
                        },
                        onError = { errString ->
                            isSyncingSwitches = true
                            buttonView.isChecked = false
                            isSyncingSwitches = false
                            toast("Authentication Failed: $errString")
                        }
                    )
                }
            } else {
                bioManager.setBiometricAuthActive(false)
                toast("Biometric authentication disabled.")
            }
        }

        binding.switchMpin.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isSyncingSwitches) return@setOnCheckedChangeListener
            if (isChecked) {
                isSyncingSwitches = true
                buttonView.isChecked = false
                isSyncingSwitches = false
                showMPINSetupDialogFromSettings()
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    bioManager.clearSecurityHardwareSettings()
                    dataStoreManager.saveMpinStatus(false)
                    syncSecuritySwitchStates()
                    toast("Secure MPIN & Hardware Tokens Cleared")
                }
            }
        }

        binding.btnRateApp.setOnClickListener { openUrl() }
        binding.btnWhatsappSupport.setOnClickListener { openWhatsappSupport() }
        binding.btnPrivacyPolicy.setOnClickListener { openStaticPage("privacy") }
        binding.btnTerms.setOnClickListener { openStaticPage("terms") }
        binding.btnDisclaimer.setOnClickListener { openStaticPage("disclaimer") }
        binding.btnAbout.setOnClickListener { openStaticPage("about") }
    }

    private fun syncSecuritySwitchStates() {
        val b = _binding ?: return
        isSyncingSwitches = true
        b.switchUnifiedBiometric.isChecked = bioManager.isBiometricAuthActive()

        viewLifecycleOwner.lifecycleScope.launch {
            val isMpinActive = dataStoreManager.isMpinSet.firstOrNull() ?: false || bioManager.isMPINSet()
            _binding?.switchMpin?.isChecked = isMpinActive
            isSyncingSwitches = false
        }
    }

    private fun evaluateSecurityCascade(onVerificationSuccess: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            val isMpinActive = dataStoreManager.isMpinSet.firstOrNull() ?: false || bioManager.isMPINSet()
            if (!isMpinActive) {
                toast("Please set your Secure MPIN first!")
                isSyncingSwitches = true
                binding.switchUnifiedBiometric.isChecked = false
                isSyncingSwitches = false
                showMPINSetupDialogFromSettings()
            } else {
                onVerificationSuccess.invoke()
            }
        }
    }

    private fun showMPINSetupDialogFromSettings() {
        // 🚀 2026 Gold Standard Layout Container for perfect margin encapsulation inside Material Dialog
        val containerFrame = android.widget.FrameLayout(requireContext()).apply {
            val paddingPx = (24 * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, (8 * resources.displayMetrics.density).toInt(), paddingPx, 0)
        }

        val styledContext = android.view.ContextThemeWrapper(
            requireContext(),
            com.google.android.material.R.style.Widget_Material3_TextInputLayout_OutlinedBox
        )

        val textInputLayout = com.google.android.material.textfield.TextInputLayout(
            styledContext,
            null,
            com.google.android.material.R.attr.textInputStyle
        ).apply {
            boxStrokeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_blue)
            hintTextColor = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.brand_blue)
            )
            setBoxCornerRadii(
                (12 * resources.displayMetrics.density), (12 * resources.displayMetrics.density),
                (12 * resources.displayMetrics.density), (12 * resources.displayMetrics.density)
            )
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }

        val etMpin = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Enter 4-Digit MPIN"
            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary))
        }

        textInputLayout.addView(etMpin)
        containerFrame.addView(textInputLayout)

        // 🚀 2026 Human-Centric Microcopy Integration
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Set Secure MPIN")
            .setMessage("Create a 4-digit secure MPIN to manage your biometric settings & Instant Login without password.")
            .setView(containerFrame)
            .setCancelable(false)
            .setPositiveButton("Save MPIN", null)
            .setNegativeButton("Cancel") { _, _ ->
                syncSecuritySwitchStates()
            }
            .create()

        dialog.show()

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val mpin = etMpin.text.toString().trim()
            if (mpin.length == 4) {
                textInputLayout.error = null
                viewLifecycleOwner.lifecycleScope.launch {
                    bioManager.saveMPIN(mpin)
                    dataStoreManager.saveMpinStatus(true)
                    dialog.dismiss()
                    syncSecuritySwitchStates()
                    toast("Secure MPIN saved successfully! 🔒")
                }
            } else {
                textInputLayout.error = "MPIN requires exactly 4 digits."
            }
        }
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

    private fun showHardwareEnrollmentDialog() {
        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Biometric Profiles Not Found")
            .setMessage("Your device doesn't have secure biometric tracking configured. Please set them up in system settings first.")
            .setCancelable(true)
            .setPositiveButton("Open Settings") { _, _ ->
                biometricHelper.launchHardwareEnrollment()
            }
            .setNegativeButton("Cancel") { _, _ ->
                syncSecuritySwitchStates()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}