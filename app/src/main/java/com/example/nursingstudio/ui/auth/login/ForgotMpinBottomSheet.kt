package com.example.nursingstudio.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.nursingstudio.R
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ForgotMpinBottomSheet(
    private val runtimePasswordFallback: String = "", // Optional safe validation bridge parameter
    private val onResetVerified: () -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_forgot_mpin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bioManager = BiometricSettingsManager(requireContext())

        val etPassword = view.findViewById<TextInputEditText>(R.id.etForgotMpinPassword)
        val btnVerify = view.findViewById<MaterialButton>(R.id.btnVerifyPasswordForMpinReset)

        btnVerify.setOnClickListener {
            val passwordText = etPassword.text.toString().trim()
            val savedPassword = bioManager.getSavedPass()

            if (passwordText.isNotEmpty()) {
                /**
                 * 🚀 2026 Fail-Safe Verification Subsystem Matrix
                 * Matches input against local encrypted keystore string AND allows direct fallback routing
                 * to prevent legacy desync bugs if the password isn't committed locally yet.
                 */
                val isVerificationValid = passwordText == savedPassword ||
                        (runtimePasswordFallback.isNotEmpty() && passwordText == runtimePasswordFallback)

                if (isVerificationValid) {
                    dismiss()
                    onResetVerified.invoke()
                } else {
                    Toast.makeText(requireContext(), "Incorrect Account Password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Password cannot be blank", Toast.LENGTH_SHORT).show()
            }
        }
    }
}