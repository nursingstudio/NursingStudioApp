package com.example.nursingstudio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast

class SettingsFragment : Fragment() {

    companion object {
        private const val KEY_VIBRATION = "enable_vibration"
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "enable_notifications"
        private const val KEY_QUIZ_SOUND = "enable_quiz_sound"
        private const val KEY_MOTIVATION = "enable_motivation"

        // Yahi wale tum MainActivity me bhi use kar rahe ho (duplicate allowed)
        private const val URL_PLAYSTORE =
            "https://play.google.com/store/apps/details?id=com.example.nursingstudio"
        private const val URL_WHATSAPP_SUPPORT =
            "https://wa.me/919999999999?text=Hello%20Nursing%20Studio%20Support" // yahan apna number
    }

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchQuizSound: SwitchCompat
    private lateinit var switchMotivation: SwitchCompat

    private lateinit var btnRateApp: Button
    private lateinit var btnWhatsappSupport: Button
    private lateinit var btnPrivacyPolicy: Button
    private lateinit var btnTerms: Button
    private lateinit var btnDisclaimer: Button
    private lateinit var btnAbout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

        switchNotifications = view.findViewById(R.id.switch_notifications)
        switchQuizSound = view.findViewById(R.id.switch_quiz_sound)
        switchMotivation = view.findViewById(R.id.switch_motivation)

        btnRateApp = view.findViewById(R.id.btnRateApp)
        btnWhatsappSupport = view.findViewById(R.id.btnWhatsappSupport)
        btnPrivacyPolicy = view.findViewById(R.id.btnPrivacyPolicy)
        btnTerms = view.findViewById(R.id.btnTerms)
        btnDisclaimer = view.findViewById(R.id.btnDisclaimer)
        btnAbout = view.findViewById(R.id.btnAbout)

        // 🔹 Load saved states
        switchNotifications.isChecked = sp.getBoolean(KEY_NOTIFICATIONS, true)
        switchQuizSound.isChecked = sp.getBoolean(KEY_QUIZ_SOUND, true)
        switchMotivation.isChecked = sp.getBoolean(KEY_MOTIVATION, true)

        // 🔹 Save on change: Notifications
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply()
        }

        // 🔹 Save on change: Test sound
        switchQuizSound.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_QUIZ_SOUND, isChecked).apply()
        }

        // 🔹 Save on change: Motivation on Home
        switchMotivation.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_MOTIVATION, isChecked).apply()
        }

        // 🔹 Rate App – Play Store
        btnRateApp.setOnClickListener {
            openUrl(URL_PLAYSTORE)
        }

        // 🔹 WhatsApp support
        btnWhatsappSupport.setOnClickListener {
            openWhatsappSupport()
        }

        // 🔹 Legal & info pages
        btnPrivacyPolicy.setOnClickListener {
            openStaticPage("privacy")
        }

        btnTerms.setOnClickListener {
            openStaticPage("terms")
        }

        btnDisclaimer.setOnClickListener {
            openStaticPage("disclaimer")
        }

        btnAbout.setOnClickListener {
            openStaticPage("about")
        }

        // Vibration toggle
        // 1. Switch ko find karo
        val switchVibration = view.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.switch_vibration)

// 2. Saved state load karo (AppSettings manager ki madad se)
        switchVibration.isChecked = AppSettings.isVibrationEnabled(requireContext())

// 3. Click listener lagao jo data save kare
        switchVibration.setOnCheckedChangeListener { _, isChecked ->
            AppSettings.setVibration(requireContext(), isChecked)

            // Test karne ke liye chota sa vibration agar ON kiya hai toh
            if (isChecked) {
                AppSettings.triggerVibration(requireContext(), 50)
            }
        }
    }


    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsappSupport() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(URL_WHATSAPP_SUPPORT))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStaticPage(type: String) {
        val fragment = StaticPageFragment.newInstance(type)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
