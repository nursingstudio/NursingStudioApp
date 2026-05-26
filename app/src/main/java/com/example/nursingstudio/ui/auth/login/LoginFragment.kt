package com.example.nursingstudio.ui.auth.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentLoginBinding
import com.example.nursingstudio.ui.auth.AuthActivity
import com.example.nursingstudio.ui.main.MainActivity
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_TIME_HOURS = 6
    }

    private var _binding: FragmentLoginBinding? = null
    internal val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    // 🚀 2026 Modern Permission Launcher for Application Alerts
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            toast("Please enable notifications for alerts 🔔")
        }
    }

    override fun onResume() {
        super.onResume()
        // ⭐ 2026 UI Focus Optimization
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(200)
            binding.etEmail.clearFocus()
            binding.etPassword.clearFocus()
            hideKeyboard()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialUI()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()

        val bioManager = BiometricSettingsManager(requireContext())
        if (bioManager.isBiometricEnabled()) {
            binding.root.postDelayed({ showBiometricPrompt() }, 500)
        }

        // 🛡️ Post Notification Runtime Compliance Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupInitialUI() {
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(0.85f).setDuration(1000).start()
        AppSettings.setPushEffect(binding.btnLoginAction)
    }

    // --- 🛡️ ACCOUNT SECURITY LOCK FRAMEWORK ---
    private fun isUserLocked(email: String): Boolean {
        if (email.isEmpty()) return false
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong("lock_timestamp_$email", 0)
        return System.currentTimeMillis() < lockUntil
    }

    private fun getRemainingLockTime(email: String): String {
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val diff = prefs.getLong("lock_timestamp_$email", 0) - System.currentTimeMillis()
        if (diff <= 0) return "00:00:00"
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun recordFailedAttempt(email: String) {
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("attempts_$email", 0) + 1

        if (attempts >= MAX_ATTEMPTS) {
            val lockUntil = System.currentTimeMillis() + (LOCK_TIME_HOURS * 60 * 60 * 1000)
            prefs.edit(action = {
                putLong("lock_timestamp_$email", lockUntil)
                putInt("attempts_$email", 0)
            })
        } else {
            prefs.edit {
                putInt("attempts_$email", attempts)
            }
        }
    }

    private fun checkAndHandleLock(email: String): Boolean {
        val isLocked = isUserLocked(email)

        if (isLocked) {
            val timeLeft = getRemainingLockTime(email)
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Account locked! Try after $timeLeft"

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isUserLocked(email)) {
                        val currentTimeLeft = getRemainingLockTime(email)
                        binding.tilEmail.error = "Account locked! Try after $currentTimeLeft"
                        kotlinx.coroutines.delay(1000)
                    }
                    binding.tilEmail.isErrorEnabled = false
                    binding.tilEmail.error = null
                }
            }
        } else {
            binding.tilEmail.isErrorEnabled = false
            binding.tilEmail.error = null
        }
        return isLocked
    }

    // --- 📊 HIGH-PERFORMANCE DATA STREAMS MONITOR ---
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    // Automated Non-blocking UI Toggle
                    binding.loadingOverlay.visibility = if (state is LoginViewModel.LoginState.Loading) View.VISIBLE else View.GONE

                    when (state) {
                        is LoginViewModel.LoginState.Idle -> { /* State Rest Container */ }
                        is LoginViewModel.LoginState.Loading -> { /* Framework Operational */ }
                        is LoginViewModel.LoginState.Success -> {
                            AppSettings.startNewUserSession(requireContext())
                            proceedToHome()
                        }
                        is LoginViewModel.LoginState.NoProfile -> {
                            toast("No profile found. Redirecting to Register...")
                            binding.root.postDelayed({
                                if (isAdded) (activity as? AuthActivity)?.showRegister()
                            }, 1000)
                        }
                        is LoginViewModel.LoginState.Error -> {
                            handleLoginError(state.message)
                        }
                    }
                }
            }
        }
    }

    private fun handleLoginError(msg: String) {
        val email = binding.etEmail.text.toString().trim()
        val userFriendlyMsg = when {
            msg.contains("network", true) || msg.contains("timeout", true) -> getString(R.string.no_internet)
            msg.contains("password", true) || msg.contains("credential", true) -> "Incorrect Credentials! Please try again.🔑"
            msg.contains("too-many-requests", true) -> "Too many attempts! Please try after some time.⏳"
            else -> msg
        }

        if (msg.contains("password", true) || msg.contains("credential", true)) {
            if (email.isNotEmpty()) recordFailedAttempt(email)
            binding.tilPassword.error = userFriendlyMsg
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
        } else {
            toast(userFriendlyMsg)
            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
        }
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.isErrorEnabled = false
                binding.tilEmail.error = null
                checkAndHandleLock(s.toString().trim())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                binding.tilPassword.isErrorEnabled = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnLoginAction.setOnClickListener {
            hideKeyboard()
            performEmailLogin()
        }

        binding.tvGoRegister.setOnClickListener {
            (activity as? AuthActivity)?.showRegister()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordSheet()
        }
    }

    private fun performEmailLogin() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (!isNetworkAvailable()) {
            toast(getString(R.string.no_internet))
            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
            return
        }

        if (isUserLocked(email)) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "This account is locked. Try after ${getRemainingLockTime(email)} hrs"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_email_empty)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = getString(R.string.error_email_invalid)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
                return
            }
        }

        when {
            pass.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_password_empty)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                return
            }
            pass.length < 8 -> {
                binding.tilPassword.error = getString(R.string.error_password_length)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                return
            }
        }
        viewModel.loginWithEmail(email, pass)
    }

    internal fun proceedToHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.withResumed {
                if (isAdded && activity != null) {
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.finish()
                }
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }

    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false
        binding.tilPassword.error = null
        binding.tilPassword.isErrorEnabled = false

        binding.btnLoginAction.post {
            val params = binding.btnLoginAction.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = (24 * resources.displayMetrics.density).toInt()
            binding.btnLoginAction.layoutParams = params
        }
    }

    private fun showForgotPasswordSheet() {
        val bottomSheet = ForgotPasswordBottomSheet()
        bottomSheet.show(childFragmentManager, "ForgotPasswordSheet")
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val manager = BiometricSettingsManager(requireContext())
                    val email = manager.getSavedEmail()
                    val password = manager.getSavedPass()

                    if (email != null) {
                        viewModel.loginWithEmail(email, password)
                    } else {
                        proceedToHome()
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        showMpinBottomSheet()
                    }
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Login")
            .setSubtitle("Scan fingerprint or use MPIN")
            .setNegativeButtonText("Use MPIN")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showMpinBottomSheet() {
        val mpinSheet = MpinBottomSheet(
            onMpinSuccess = { email, pass ->
                if (email != null && pass != null) {
                    viewModel.loginWithEmail(email, pass)
                } else {
                    proceedToHome()
                }
            },
            onBiometricRequest = {
                showBiometricPrompt()
            }
        )
        mpinSheet.show(childFragmentManager, "MpinSheet")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        viewModel.resetState()
        _binding = null
        super.onDestroyView()
    }
}