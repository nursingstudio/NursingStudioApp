package com.example.nursingstudio.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutForgotMpinBinding
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
        // Explicit structural compatibility enforcement logic overridden here cleanly
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutForgotMpinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bioManager = BiometricSettingsManager(requireContext())

        binding.btnVerifyPasswordForMpinReset.setOnClickListener {
            val passwordText = binding.etForgotMpinPassword.text.toString().trim()
            val savedPassword = bioManager.getSavedPass()

            // Reset errors smoothly on click events
            binding.tilForgotMpinPassword.error = null

            if (passwordText.isNotEmpty()) {
                val isVerificationValid = passwordText == savedPassword ||
                        (runtimePasswordFallback.isNotEmpty() && passwordText == runtimePasswordFallback)

                if (isVerificationValid) {
                    // 🚀 2026 GOLD STANDARD FIX: Asynchronous atomic execution loop ensures dialog transition is never dropped or glitched
                    binding.root.post {
                        dismissAllowingStateLoss()
                        // Safe post-execution layer prevents view transaction freezing crashes
                        onResetVerified.invoke()
                    }
                } else {
                    binding.tilForgotMpinPassword.error = "Incorrect Account Password"
                }
            } else {
                binding.tilForgotMpinPassword.error = "Password cannot be blank"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}