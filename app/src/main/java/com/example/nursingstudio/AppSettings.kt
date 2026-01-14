package com.example.nursingstudio

import android.content.Context
import android.os.*

object AppSettings {
    private const val PREF_SETTINGS = "settings_prefs"
    private const val KEY_VIBRATION = "enable_vibration"

    fun isVibrationEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_VIBRATION, true)
    }

    fun setVibration(context: Context, isEnabled: Boolean) {
        val sp = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        sp.edit().putBoolean(KEY_VIBRATION, isEnabled).apply()
    }

    // --- YE WALA AB UNIVERSAL HO GAYA ---
    fun triggerVibration(context: Context, ms: Long) {
        if (!isVibrationEnabled(context)) return

        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms)
            }
        }
    }
}