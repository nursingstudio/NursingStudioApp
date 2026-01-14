package com.example.nursingstudio

import android.content.Context
import android.os.*
import android.view.MotionEvent
import android.view.View

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

    /**
     * WORLD-CLASS PUSH EFFECT:
     * Haptic Jhatka + Visual Shrink
     */
    fun setPushEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. Visual: Thoda chota karo
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start()
                    // 2. Haptic: Ek halka sa click feel (sirf 10ms)
                    triggerVibration(v.context, 10)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Wapas normal size
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick() // Standard click register karne ke liye
                    }
                }
            }
            true // True isliye taaki hum touch event consume karein
        }
    }
}