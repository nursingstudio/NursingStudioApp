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

class ForgotMpinBottomSheet(private val onResetVerified: () -> Unit) : BottomSheetDialogFragment() {

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
                // 🚀 SANITIZED 2026 LOGIC: OTP dependencies completely wiped out. Pure credential matching.
                if (passwordText == savedPassword) {
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