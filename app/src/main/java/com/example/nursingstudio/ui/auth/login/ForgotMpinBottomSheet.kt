package com.example.nursingstudio.ui.auth.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutForgotMpinBinding
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ForgotMpinBottomSheet(
    private val runtimePasswordFallback: String = "",
    private val onResetVerified: () -> Unit
) : BottomSheetDialogFragment() {

    // 🚀 FIXED: Upgraded legacy layout systems to modern programmatic ViewBinding configuration to prevent reflection slowdowns
    private var _binding: LayoutForgotMpinBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Styled dynamically using your unified system theme
        setStyle(STYLE_NORMAL, R.style.GlassBottomSheetDialogTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutForgotMpinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bioManager = BiometricSettingsManager(requireContext())

        // 🚀 FIXED: Replaced brittle implicit findViewById calls with lightning fast pre-compiled binding layers reference parameters
        binding.btnVerifyPasswordForMpinReset.setOnClickListener {
            val passwordText = binding.etForgotMpinPassword.text.toString().trim()
            val savedPassword = bioManager.getSavedPass()

            if (passwordText.isNotEmpty()) {
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

    override fun onDestroyView() {
        // 🚀 FIXED: Explicit memory isolation mechanism applied here to fully intercept any potential garbage collection leaks
        super.onDestroyView()
        _binding = null
    }
}