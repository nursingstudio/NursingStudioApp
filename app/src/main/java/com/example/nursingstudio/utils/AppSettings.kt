package com.example.nursingstudio.utils

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.edit
import com.example.nursingstudio.R

object AppSettings {
    private const val PREF_SETTINGS = "settings_prefs"
    private const val KEY_VIBRATION = "enable_vibration"

    fun isVibrationEnabled(context: Context): Boolean {
        val sp = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        return sp.getBoolean(KEY_VIBRATION, true)
    }

    fun setVibration(context: Context, isEnabled: Boolean) {
        val sp = context.getSharedPreferences(PREF_SETTINGS, Context.MODE_PRIVATE)
        sp.edit { putBoolean(KEY_VIBRATION, isEnabled) }
    }

    fun triggerVibration(context: Context, ms: Long) {
        if (!isVibrationEnabled(context)) return

        // 1. Android 12+ (API 31) direct VibratorManager usage
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        val vibrator = vibratorManager.defaultVibrator

        // 2. Simple, modern vibration execution
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    /**
     * CENTRAL AC: Shake + Vibrate Error Animation
     * Kisi bhi view par error aane par ise call karein
     */
    fun triggerErrorEffect(context: Context, view: View) {
        // 1. Shake Animation load aur start karein
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
        view.startAnimation(shake)

        // 2. Stronger Vibration for Error (50ms)
        triggerVibration(context, 50)

        // 3. Request focus taaki cursor wahan chala jaye
        view.requestFocus()
    }

    fun setPushEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start()
                    triggerVibration(v.context, 10)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (event.action == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                }
            }
            true
        }
    }
    fun startNewUserSession(context: Context) {
        // 1. Purana shared preferences poori tarah saaf (Suresh vs Alok issue fix)
        val session = context.getSharedPreferences("session", Context.MODE_PRIVATE)
        session.edit { clear() }

        // 2. Clear other temporary caches if needed
        val motivationPref = context.getSharedPreferences("daily_motivation", Context.MODE_PRIVATE)
        motivationPref.edit { clear() }
    }
}