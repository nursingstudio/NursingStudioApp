package com.example.nursingstudio.utils

import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
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
    /**
     * 🚀 2026 Gold Standard: Universal View Error Styler
     * Ye function automatic pehchanta hai ki view kya hai aur us par error theme apply karta hai
     */
    fun triggerErrorEffect(context: Context, view: View, message: String? = null) {
        val errorColor = ContextCompat.getColor(context, R.color.error_red)

        // 🚀 2026 Gold Standard: Hierarchy-Aware Styling
        when (// 1. Agar view TextInputLayout hai (Jaise Name, Email, etc.)
            view) {
            is com.google.android.material.textfield.TextInputLayout -> {
                if (message != null) {
                    view.isErrorEnabled = true
                    view.error = message
                }
                // ✨ 2026 Gold Standard: Dynamic Cursor Coloring
                // TextInputLayout ke andar ke EditText ko nikaal kar uska tint badalna
                view.editText?.let { et ->
                    et.textCursorDrawable?.setTint(errorColor)
                    et.backgroundTintList = ColorStateList.valueOf(errorColor) // Underline ko bhi Red karna
                }
                view.setErrorTextColor(ColorStateList.valueOf(errorColor))
                view.setErrorIconTintList(ColorStateList.valueOf(errorColor))
            }

            // 2. Agar view Spinner hai
            is android.widget.Spinner -> {
                view.background = ContextCompat.getDrawable(context, R.drawable.spinner_error_bg)
            }

            // 3. Agar view CheckBox hai
            is android.widget.CheckBox -> {
                view.buttonTintList = ColorStateList.valueOf(errorColor)
            }

            // 4. Special Case: etMobile (Jo direct EditText hai, TIL ke andar nahi hai)
            is android.widget.EditText -> {
                // Agar mobile field hai toh border red kar sakte hain ya sirf animation rehne de sakte hain
                // Kyunki iska error label (TextView) Fragment handle kar raha hai.
                view.backgroundTintList = ColorStateList.valueOf(errorColor)
            }

            // 5. Custom Error Labels (TextViews)
            is android.widget.TextView if message != null -> {
                view.text = message
                view.visibility = View.VISIBLE
                view.setTextColor(errorColor)
            }
        }

        // --- Standard Effects (Sabhi Views ke liye) ---

        // 1. Shake Animation
        val shake = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.shake)
        view.startAnimation(shake)

        // 2. Strong Vibration
        triggerVibration(context, 50)

        // 3. Focus
        // 🚀 World-Class Fix for Cursor: Post the focus to next frame
        view.post {
            view.requestFocus()
        }
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