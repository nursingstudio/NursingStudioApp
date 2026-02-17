package com.example.nursingstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText

class SettingsFragment : Fragment() {

    companion object {
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "enable_notifications"
        private const val KEY_QUIZ_SOUND = "enable_quiz_sound"
        private const val KEY_MOTIVATION = "enable_motivation"

        private const val URL_PLAYSTORE = "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
        private const val URL_WHATSAPP_SUPPORT = "https://wa.me/919999999999?text=Hello%20Nursing%20Studio%20Support"
    }

    // Class level par define karein taaki har function access kar sake
    private lateinit var bioManager: BiometricSettingsManager
    private var switchBiometric: SwitchMaterial? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bioManager = BiometricSettingsManager(requireContext())
        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

        // Find views
        val switchNotifications = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val switchQuizSound = view.findViewById<SwitchCompat>(R.id.switch_quiz_sound)
        val switchMotivation = view.findViewById<SwitchCompat>(R.id.switch_motivation)
        val switchVibration = view.findViewById<SwitchMaterial>(R.id.switch_vibration)
        switchBiometric = view.findViewById(R.id.switch_biometric)

        // Load states
        switchNotifications.isChecked = sp.getBoolean(KEY_NOTIFICATIONS, true)
        switchQuizSound.isChecked = sp.getBoolean(KEY_QUIZ_SOUND, true)
        switchMotivation.isChecked = sp.getBoolean(KEY_MOTIVATION, true)
        switchVibration.isChecked = AppSettings.isVibrationEnabled(requireContext())
        switchBiometric?.isChecked = bioManager.isBiometricEnabled()

        // Listeners
        switchNotifications.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply() }
        switchQuizSound.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(KEY_QUIZ_SOUND, isChecked).apply() }
        switchMotivation.setOnCheckedChangeListener { _, isChecked -> sp.edit().putBoolean(KEY_MOTIVATION, isChecked).apply() }

        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setVibration(requireContext(), isChecked)
            if (isChecked) AppSettings.triggerVibration(requireContext(), 50)
        }

        // --- WORLD CLASS BIOMETRIC LOGIC ---
        switchBiometric?.setOnClickListener {
            val isChecked = (it as SwitchMaterial).isChecked
            if (isChecked) {
                // Pehle switch wapas OFF karo (jab tak success na ho)
                it.isChecked = false
                showPasswordVerificationSheet()
            } else {
                bioManager.setBiometricEnabled(false)
                Toast.makeText(requireContext(), "Biometric security disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Other buttons
        view.findViewById<Button>(R.id.btnRateApp).setOnClickListener { openUrl(URL_PLAYSTORE) }
        view.findViewById<Button>(R.id.btnWhatsappSupport).setOnClickListener { openWhatsappSupport() }
        view.findViewById<Button>(R.id.btnPrivacyPolicy).setOnClickListener { openStaticPage("privacy") }
        view.findViewById<Button>(R.id.btnTerms).setOnClickListener { openStaticPage("terms") }
        view.findViewById<Button>(R.id.btnDisclaimer).setOnClickListener { openStaticPage("disclaimer") }
        view.findViewById<Button>(R.id.btnAbout).setOnClickListener { openStaticPage("about") }
    }

    private fun showPasswordVerificationSheet() {
        val type = bioManager.getLoginType()

        // Professional Check: Agar Mobile user hai toh password mangne ka logic skip karo
        if (type == 1 || bioManager.getSavedPass() == "OTP_USER") {
            showMPINSetupDialogFromSettings()
            return
        }

        // Email user ke liye purana sheet logic...
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.layout_verify_for_biometric, null)

        val etPass = sheetView.findViewById<TextInputEditText>(R.id.etVerifyPassword)
        val btnVerify = sheetView.findViewById<MaterialButton>(R.id.btnVerifyAndSetMPIN)

        btnVerify.setOnClickListener {
            val password = etPass.text.toString().trim()
            if (password.isNotEmpty()) {
                // Professional Check: saved password se match karein
                val savedPass = bioManager.getSavedPass()
                if (password == savedPass || savedPass == "OTP_USER") {
                    dialog.dismiss()
                    showMPINSetupDialogFromSettings()
                } else {
                    Toast.makeText(requireContext(), "Incorrect Password", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun showMPINSetupDialogFromSettings() {
        // Banking UI ke liye hum custom layout use karenge
        val etMpin = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            hint = "Enter 4-Digit MPIN"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Set Secure PIN")
            .setMessage("Create a 4-digit MPIN for biometric fallback.")
            .setView(etMpin)
            .setCancelable(false)
            .setPositiveButton("Set PIN") { _, _ ->
                val mpin = etMpin.text.toString()
                if (mpin.length == 4) {
                    bioManager.setBiometricEnabled(true)
                    bioManager.saveMPIN(mpin)
                    switchBiometric?.isChecked = true // Ab final ON kardo
                    Toast.makeText(requireContext(), "Biometric & MPIN Set! 🔒", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "MPIN must be 4 digits", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show() }
    }

    private fun openWhatsappSupport() {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_WHATSAPP_SUPPORT))) }
        catch (e: Exception) { Toast.makeText(requireContext(), "WhatsApp not available", Toast.LENGTH_SHORT).show() }
    }

    private fun openStaticPage(type: String) {
        val fragment = StaticPageFragment.newInstance(type)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment).addToBackStack(null).commit()
    }
}