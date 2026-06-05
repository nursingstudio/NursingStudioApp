package com.example.nursingstudio.ui.auth.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.databinding.FragmentLoginBinding
import com.example.nursingstudio.ui.auth.AuthActivity
import com.example.nursingstudio.ui.main.MainActivity
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_TIME_HOURS = 6
    }

    private var _binding: FragmentLoginBinding? = null
    internal val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var dataStoreManager: DataStoreManager
    private var isAdaptiveUiOverridden = false

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            toast("Please enable notifications for alerts 🔔")
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            kotlinx.coroutines.delay(200.milliseconds)
            binding.etEmail.clearFocus()
            binding.etPassword.clearFocus()
            hideKeyboard()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialUI()
        checkAdaptiveSessionState()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupInitialUI() {
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(0.85f).setDuration(1000).start()
        AppSettings.setPushEffect(binding.btnLoginAction)
    }

    // 🚀 2026 DYNAMIC SESSION ROUTER: Checks layout mode instantly on start
    private fun checkAdaptiveSessionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (isAdaptiveUiOverridden) return@repeatOnLifecycle

                val isMpinConfigured = dataStoreManager.isMpinSet.firstOrNull() ?: false
                val cachedName = dataStoreManager.userName.firstOrNull() ?: "User"

                if (isMpinConfigured) {
                    binding.layoutDefaultForm.visibility = View.GONE
                    binding.layoutAdaptiveMpinForm.visibility = View.VISIBLE
                    binding.tvWelcomeUser.text = getString(R.string.welcome_back, cachedName)

                    // Instant biometric prompt initialization if active
                    val bioManager = BiometricSettingsManager(requireContext())
                    if (bioManager.isBiometricEnabled()) {
                        showBiometricPrompt()
                    }
                } else {
                    binding.layoutDefaultForm.visibility = View.VISIBLE
                    binding.layoutAdaptiveMpinForm.visibility = View.GONE
                }
            }
        }
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
            prefs.edit { putInt("attempts_$email", attempts) }
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
                        kotlinx.coroutines.delay(1000.milliseconds)
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

    // --- 📊 REACTION SUBSYSTEM LIVE STREAMS ---
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    binding.loadingOverlay.visibility = if (state is LoginViewModel.LoginState.Loading) View.VISIBLE else View.GONE

                    when (state) {
                        is LoginViewModel.LoginState.Idle, is LoginViewModel.LoginState.Loading -> {}
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
                        is LoginViewModel.LoginState.Error -> handleLoginError(state.message)
                    }
                }
            }
        }
    }

    private fun handleLoginError(msg: String) {
        val email = binding.etEmail.text.toString().trim()
        val userFriendlyMsg = when {
            msg.contains("network", true) || msg.contains("timeout", true) -> getString(R.string.no_internet)
            msg.contains("password", true) || msg.contains("credential", true) -> "Incorrect Credentials!🔑"
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

        // 🚀 ADAPTIVE UI TRIGGERS
        binding.tvDifferentUser.setOnClickListener {
            isAdaptiveUiOverridden = true
            binding.layoutAdaptiveMpinForm.visibility = View.GONE
            binding.layoutDefaultForm.visibility = View.VISIBLE
        }

        // 🚀 ROUTING FORGOT MPIN FLOW INDEPENDENT OF PASSWORD RESET LAYOUT
        // 🚀 RESOLVED FIXED LINE 270 ERRORS:
        binding.tvForgotMpinAction.setOnClickListener {
            val forgotMpinSheet = ForgotMpinBottomSheet(onResetVerified = {
                showLocalMpinSetupDialog()
            })
            forgotMpinSheet.show(childFragmentManager, "ForgotMpinTag")
        }

        binding.btnFingerprintTrigger.setOnClickListener { showBiometricPrompt() }
        binding.btnFaceUnlockTrigger.setOnClickListener { showBiometricPrompt() }
        binding.layoutActiveDots.setOnClickListener { showMpinBottomSheet() }
    }
    // 🚀 NEW SECURE LAYER: Independent setup configuration specifically for Login context
    private fun showLocalMpinSetupDialog() {
        val etMpin = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Create 4-Digit PIN"
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Reset Security PIN")
            .setMessage("Set a secure fallback 4-digit pin profile to access account operations.")
            .setView(etMpin)
            .setCancelable(false)
            .setPositiveButton("Save PIN") { _, _ ->
                val mpin = etMpin.text.toString()
                if (mpin.length == 4) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val bioManager = BiometricSettingsManager(requireContext())
                        bioManager.saveMPIN(mpin)
                        dataStoreManager.saveMpinStatus(true)
                        toast("Security PIN updated successfully! 🔒")
                        checkAdaptiveSessionState()
                    }
                } else {
                    toast("PIN validation requires exactly 4 digits")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            binding.tilPassword.error = "Account locked! Try after ${getRemainingLockTime(email)}"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            return
        }
        if (pass.isEmpty() || pass.length < 8) {
            binding.tilPassword.error = getString(R.string.error_password_length)
            return
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
    }

    private fun showForgotPasswordSheet() {
        val bottomSheet = ForgotPasswordBottomSheet()
        bottomSheet.show(childFragmentManager, "ForgotPasswordSheet")
    }

    // 🚀 RESOLVED FIXED LINE 351 LOOP CANCELLATION CRITICAL BUG
    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())

        // Declare prompt instance up-front to correctly bind access scoped logic blocks
        var biometricPromptInstance: BiometricPrompt? = null

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                // 🛡️ Safe termination lock access to stop infinite hardware loop instances
                biometricPromptInstance?.cancelAuthentication()

                val manager = BiometricSettingsManager(requireContext())
                val savedEmail = manager.getSavedEmail()
                val savedPassword = manager.getSavedPass()

                if (!savedEmail.isNullOrEmpty() && savedPassword.isNotEmpty()) {
                    viewModel.loginWithEmail(savedEmail, savedPassword)
                    proceedToHome()
                } else {
                    proceedToHome()
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    showMpinBottomSheet()
                }
            }
        }

        biometricPromptInstance = BiometricPrompt(this, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Login")
            .setSubtitle("Scan fingerprint or face identity")
            .setNegativeButtonText("Use MPIN")
            .build()

        biometricPromptInstance.authenticate(promptInfo)
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
            onBiometricRequest = { showBiometricPrompt() }
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