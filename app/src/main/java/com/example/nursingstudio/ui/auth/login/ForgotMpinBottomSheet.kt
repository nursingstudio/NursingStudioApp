package com.example.nursingstudio.ui.auth.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutForgotMpinBinding
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ForgotMpinBottomSheet(
    private val runtimePasswordFallback: String = "",
    private val onResetVerified: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutForgotMpinBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutForgotMpinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bioManager = BiometricSettingsManager(requireContext())

        // 🚀 2026 Core Flow: Dynamic push motion animations initialized onto actions button vector parameters
        AppSettings.setPushEffect(binding.btnVerifyPasswordForMpinReset)

        // 🚀 FIXED: Dynamic event interceptors clearing validation failures parameters instantly as user starts typing
        binding.etForgotMpinPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilForgotMpinPassword.error = null
                binding.tilForgotMpinPassword.isErrorEnabled = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnVerifyPasswordForMpinReset.setOnClickListener {
            val passwordText = binding.etForgotMpinPassword.text.toString().trim()
            val savedPassword = bioManager.getSavedPass()

            if (passwordText.isNotEmpty()) {
                val isVerificationValid = passwordText == savedPassword ||
                        (runtimePasswordFallback.isNotEmpty() && passwordText == runtimePasswordFallback)

                if (isVerificationValid) {
                    binding.root.post {
                        dismissAllowingStateLoss()
                        onResetVerified.invoke()
                    }
                } else {
                    binding.tilForgotMpinPassword.isErrorEnabled = true
                    binding.tilForgotMpinPassword.error = "Incorrect Account Password"
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotMpinPassword)
                }
            } else {
                binding.tilForgotMpinPassword.isErrorEnabled = true
                binding.tilForgotMpinPassword.error = "Password cannot be blank"
                AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotMpinPassword)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}