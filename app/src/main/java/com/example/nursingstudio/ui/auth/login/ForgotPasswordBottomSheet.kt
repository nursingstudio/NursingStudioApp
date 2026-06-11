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
import com.example.nursingstudio.ui.auth.AuthActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutForgotPasswordBinding
import com.example.nursingstudio.utils.AppSettings
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
        // 🚀 FIXED: 2026 Absolute Zero-Warning Keyboard Architecture Validation Gate
        val currentDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = currentDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        // Clean programmatic system window-insets handling that perfectly adjusts inner layout padding reactively
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { _, insets ->
            val imeInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())

            // Apply precise safe padding limits without jumping components text layouts
            _binding?.root?.setPadding(0, 0, 0, imeInsets.bottom - systemBars.bottom)
            insets
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        // Safe hide keyboard vector sequence using direct clean reference maps without redundant lets
        val currentWindow = dialog?.window
        if (currentWindow != null) {
            androidx.core.view.WindowCompat.getInsetsController(currentWindow, binding.root).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            }
        }

        if (!isNetworkAvailable()) {
            showError(getString(R.string.no_internet))
            return
        }

        if (email.isEmpty()) {
            binding.tilForgotEmail.error = getString(R.string.error_email_empty)
            AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotEmail)
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilForgotEmail.error = getString(R.string.error_email_invalid)
            AppSettings.triggerErrorEffect(requireContext(), binding.tilForgotEmail)
            return
        }

        binding.btnResetPassword.isEnabled = false
        binding.btnResetPassword.text = ""
        binding.progressLoading.visibility = View.VISIBLE
        sendResetEmail(email)
    }

    private fun sendResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            // Structural null safety initialization verification gate
            val b = _binding ?: return@addOnCompleteListener

            b.progressLoading.visibility = View.GONE
            b.btnResetPassword.isEnabled = true
            b.btnResetPassword.text = getString(R.string.send_reset_link)

            if (task.isSuccessful) {
                Toast.makeText(context, getString(R.string.reset_link_sent), Toast.LENGTH_LONG).show()
                dismiss()
            } else {
                val exception = task.exception
                if (exception is FirebaseAuthInvalidUserException) {
                    b.tilForgotEmail.isErrorEnabled = true
                    b.tilForgotEmail.error = getString(R.string.email_not_linked)
                    AppSettings.triggerErrorEffect(requireContext(), b.tilForgotEmail)

                    Toast.makeText(context, getString(R.string.redirecting), Toast.LENGTH_SHORT).show()

                    viewLifecycleOwner.lifecycleScope.launch {
                        delay(3500.milliseconds)
                        if (isAdded) {
                            dismiss()
                            (activity as? AuthActivity)?.showRegister()
                        }
                    }
                } else {
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
        _binding?.let { b ->
            AppSettings.triggerErrorEffect(requireContext(), b.btnResetPassword)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}