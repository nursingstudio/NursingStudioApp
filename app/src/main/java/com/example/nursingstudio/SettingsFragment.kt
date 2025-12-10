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

class SettingsFragment : Fragment() {

    companion object {
        private const val PREF_SETTINGS = "settings_prefs"
        private const val KEY_NOTIFICATIONS = "enable_notifications"
        private const val KEY_QUIZ_SOUND = "enable_quiz_sound"
        private const val KEY_QUIZ_VIBRATE = "enable_quiz_vibrate"
        private const val KEY_MOTIVATION = "enable_motivation"

        // Ye links tumhare MainActivity ke jaisa rakhe:
        private const val URL_PLAYSTORE =
            "https://play.google.com/store/apps/details?id=com.example.nursingstudio"

        // Support ke liye WhatsApp – abhi channel link use kar rahe hain,
        // baad me chaho to direct chat link laga sakte ho.
        private const val URL_WHATSAPP_SUPPORT =
            "https://whatsapp.com/channel/0029Vb6Sjdq6BIEapKtNUE2L"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchNotifications =
            view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val switchQuizSound =
            view.findViewById<SwitchCompat>(R.id.switch_quiz_sound)
        val switchQuizVibrate =
            view.findViewById<SwitchCompat>(R.id.switch_quiz_vibrate)
        val switchMotivation =
            view.findViewById<SwitchCompat>(R.id.switch_motivation)

        val btnRateApp = view.findViewById<Button>(R.id.btnRateApp)
        val btnWhatsappSupport = view.findViewById<Button>(R.id.btnWhatsappSupport)

        val sp = requireContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)

        // ▸ SharedPreferences se values load karo
        val notificationsOn = sp.getBoolean(KEY_NOTIFICATIONS, true)
        val quizSoundOn = sp.getBoolean(KEY_QUIZ_SOUND, true)
        val quizVibrateOn = sp.getBoolean(KEY_QUIZ_VIBRATE, false)
        val motivationOn = sp.getBoolean(KEY_MOTIVATION, true)

        switchNotifications.isChecked = notificationsOn
        switchQuizSound.isChecked = quizSoundOn
        switchQuizVibrate.isChecked = quizVibrateOn
        switchMotivation.isChecked = motivationOn

        // ▸ Listeners – abhi sirf prefs save, actual use hum baad me karenge
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply()
            Toast.makeText(
                requireContext(),
                if (isChecked) "Notifications enabled" else "Notifications disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        switchQuizSound.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_QUIZ_SOUND, isChecked).apply()
        }

        switchQuizVibrate.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_QUIZ_VIBRATE, isChecked).apply()
        }

        switchMotivation.setOnCheckedChangeListener { _, isChecked ->
            sp.edit().putBoolean(KEY_MOTIVATION, isChecked).apply()
        }

        // ▸ Rate app button
        btnRateApp.setOnClickListener {
            openUrl(URL_PLAYSTORE, null)
        }

        // ▸ WhatsApp support button
        btnWhatsappSupport.setOnClickListener {
            // Direct WhatsApp try karo, warna normal browser
            openUrl(URL_WHATSAPP_SUPPORT, "com.whatsapp")
        }
    }

    private fun openUrl(url: String, packageName: String?) {
        val ctx = requireContext()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (packageName != null) {
                intent.setPackage(packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(ctx, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
