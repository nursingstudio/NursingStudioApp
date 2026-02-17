package com.example.nursingstudio.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
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
}