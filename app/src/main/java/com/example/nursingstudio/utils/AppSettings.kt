package com.example.nursingstudio.utils

import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.nursingstudio.R

object AppSettings {
    private const val PREF_SETTINGS = "settings_prefs"
    private const val KEY_VIBRATION = "enable_vibration"

    // 🚀 2026 Performance: Memory Efficient Vibrator Retrieval
    private fun getVibrator(context: Context) =
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator

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
        val vibrator = getVibrator(context)
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(ms, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    /**
     * 🚀 2026 Gold Standard: Universal View Error Styler (Modularized)
     */
    fun triggerErrorEffect(context: Context, view: View, message: String? = null) {
        val errorColor = ContextCompat.getColor(context, R.color.error_red)

        // Step 1: Branching to specialized handlers for high scalability
        when (view) {
            is com.google.android.material.textfield.TextInputLayout ->
                handleTILError(context, view, message, errorColor)

            is android.widget.EditText ->
                handleEditTextError(view, errorColor)

            is android.widget.Spinner ->
                view.background = ContextCompat.getDrawable(context, R.drawable.spinner_error_bg)

            is android.widget.CheckBox ->
                view.buttonTintList = ColorStateList.valueOf(errorColor)

            is android.widget.TextView -> if (message != null) {
                view.apply {
                    text = message
                    visibility = View.VISIBLE
                    setTextColor(errorColor)
                }
            }
        }

        // Step 2: Apply common UX effects (Shake, Vibrate)
        applyStandardEffects(context, view)

        // Step 3: World-Class Focus & Frame Refresh Fix
        view.post {
            // 2026 Performance Tip: Force OS to re-calculate UI insets
            view.requestApplyInsets()

            view.postDelayed({
                if (view is com.google.android.material.textfield.TextInputLayout) {
                    view.editText?.let { et ->
                        et.requestFocus()
                        et.setSelection(et.text?.length ?: 0)
                        et.requestLayout()
                    }
                } else {
                    view.requestFocus()
                }
            }, 180)
        }
    }

    // --- Private World-Class Modular Handlers ---

    private fun handleTILError(context: Context, til: com.google.android.material.textfield.TextInputLayout, message: String?, color: Int) {
        til.isErrorEnabled = true
        til.error = message
        til.setErrorTextColor(ColorStateList.valueOf(color))
        til.setErrorIconTintList(ColorStateList.valueOf(color))

        til.editText?.let { et ->
            // ✨ Final Cursor Solution: Direct Injection
            et.textCursorDrawable = ContextCompat.getDrawable(context, R.drawable.cursor_error_red)
            et.backgroundTintList = ColorStateList.valueOf(color)

            if (et.isFocused) et.clearFocus()
            et.invalidate()
        }
    }

    private fun handleEditTextError(et: android.widget.EditText, color: Int) {
        et.backgroundTintList = ColorStateList.valueOf(color)
        et.invalidate()
    }

    private fun applyStandardEffects(context: Context, view: View) {
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake)
        view.startAnimation(shake)
        triggerVibration(context, 50)
    }

    // --- Interaction & Session Logic ---

    fun setPushEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start()
                    triggerVibration(v.context, 10)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    if (event.action == MotionEvent.ACTION_UP) v.performClick()
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