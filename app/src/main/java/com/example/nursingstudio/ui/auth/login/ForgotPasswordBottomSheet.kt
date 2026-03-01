package com.example.nursingstudio.ui.auth.login

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.nursingstudio.AuthActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutForgotPasswordBinding
import com.example.nursingstudio.utils.AppSettings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { d ->
            // Get the internal Material BottomSheet container
            val bottomSheet = d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet)

            // 1. Keep it professional: Height should be WRAP_CONTENT
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

            // 2. Behavior Settings
            behavior.skipCollapsed = true
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED

            // 3. TOP-TIER KEYBOARD HANDLING:
            // This adds padding to the BOTTOM of the sheet equal to the Keyboard height.
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, insets ->
                val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())

                // We apply the keyboard height as padding so the "Send Link" button
                // is pushed exactly above the keyboard.
                view.setPadding(0, 0, 0, imeInsets.bottom + systemBars.bottom)
                insets
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ensure the view scrolls to show the button when keyboard pops up
        binding.etForgotEmail.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.root.postDelayed({
                    binding.root.smoothScrollTo(0, binding.btnResetPassword.bottom)
                }, 200)
            }
        }

        binding.etForgotEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                binding.tilForgotEmail.error = null
                binding.tilForgotEmail.isErrorEnabled = false
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnResetPassword.setOnClickListener {
            val email = binding.etForgotEmail.text.toString().trim()
            handlePasswordResetFlow(email)
        }
    }

    private fun handlePasswordResetFlow(email: String) {
        val controller = androidx.core.view.WindowCompat.getInsetsController(requireDialog().window!!, binding.root)
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.ime())

        // 1. Connectivity Check
        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet))
            return
        }

        // 2. Validation Check
        if (email.isEmpty()) {
            binding.tilForgotEmail.error = "Email cannot be empty"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotEmail)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilForgotEmail.error = "Please enter a valid email format"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotEmail)
            return
        }

        // 3. Modern 2026 Logic & modern progress handling
        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = ""
        binding.progressLoading.visibility = View.VISIBLE
        sendResetEmail(email)
    }

    private fun sendResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            binding.progressLoading.visibility = View.GONE
            binding.btnResetPassword.isEnabled = true
            binding.btnResetPassword.text = getString(R.string.send_reset_link)

            if (task.isSuccessful) {
                // ✅ Link sent successfully
                Toast.makeText(context, getString(R.string.reset_link_sent), Toast.LENGTH_LONG).show()
                dismiss()
            } else {
                val exception = task.exception
                // ⭐ INDUSTRY GOLD STANDARD: Handling Firebase Auth Error Codes
                if (exception is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                    // 🚨 User NOT Registered (ERROR_USER_NOT_FOUND)
                    binding.tilForgotEmail.isErrorEnabled = true
                    binding.tilForgotEmail.error = "This email is not linked to any account. Please Register first."
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotEmail)

                    Toast.makeText(context, "Redirecting to Register...", Toast.LENGTH_SHORT).show()

                    binding.root.postDelayed({
                        if (isAdded) {
                            dismiss()
                            (activity as? AuthActivity)?.showRegister()
                        }
                    }, 3500)
                } else {
                    // Other errors (like bad format, network, etc)
                    showError("Error: ${exception?.localizedMessage}")
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        AppSettings.triggerErrorEffect(requireContext(), binding.btnResetPassword)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}