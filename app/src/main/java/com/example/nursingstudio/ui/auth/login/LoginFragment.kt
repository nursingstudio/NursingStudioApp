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
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.core.content.edit
import androidx.core.view.isVisible
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
import com.example.nursingstudio.utils.BiometricAuthHelper
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
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
    private lateinit var bioSettingsManager: BiometricSettingsManager
    private lateinit var biometricHelper: BiometricAuthHelper

    private var isAdaptiveUiOverridden = false
    // 🚀 FIXED: Dynamic verification tracker container to cleanly kill the ticker loop anytime fragment context transitions
    private var securityLockTickerJob: Job? = null

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
            if (_binding != null) {
                binding.etEmail.clearFocus()
                binding.etPassword.clearFocus()
                hideKeyboard()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager(requireContext())
        bioSettingsManager = BiometricSettingsManager(requireContext())
        biometricHelper = BiometricAuthHelper(this)
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

                    if (bioSettingsManager.isBiometricAuthActive() &&
                        biometricHelper.checkBiometricAvailability() == BiometricManager.BIOMETRIC_SUCCESS) {
                        showBiometricPrompt()
                    }
                } else {
                    binding.layoutDefaultForm.visibility = View.VISIBLE
                    binding.layoutAdaptiveMpinForm.visibility = View.GONE
                }
            }
        }
    }

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
            prefs.edit {
                putLong("lock_timestamp_$email", lockUntil)
                putInt("attempts_$email", 0)
            }
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

            // 🚀 FIXED: Cleaned and structured background security tracking to terminate loops gracefully on lifecycle exit vectors
            securityLockTickerJob?.cancel()
            securityLockTickerJob = viewLifecycleOwner.lifecycleScope.launch {
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

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    binding.loadingOverlay.visibility = if (state is LoginViewModel.LoginState.Loading) View.VISIBLE else View.GONE

                    when (state) {
                        is LoginViewModel.LoginState.Idle, is LoginViewModel.LoginState.Loading -> {}
                        is LoginViewModel.LoginState.Success -> {
                            // 🚀 FIXED: Enforced a strong condition barrier checking that inputs are populated before executing encryption keys payload serialization
                            val inputtedEmail = binding.etEmail.text.toString().trim()
                            val inputtedPassword = binding.etPassword.text.toString().trim()

                            if (inputtedEmail.isNotEmpty() && inputtedPassword.isNotEmpty() && binding.layoutDefaultForm.isVisible) {
                                bioSettingsManager.saveCredentials(inputtedEmail, inputtedPassword)
                            }

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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
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

        binding.tvDifferentUser.setOnClickListener {
            isAdaptiveUiOverridden = true
            binding.layoutAdaptiveMpinForm.visibility = View.GONE
            binding.layoutDefaultForm.visibility = View.VISIBLE
        }

        binding.tvForgotMpinAction.setOnClickListener {
            val currentInputtedPass = binding.etPassword.text.toString().trim()
            val forgotMpinSheet = ForgotMpinBottomSheet(
                runtimePasswordFallback = currentInputtedPass,
                onResetVerified = {
                    // Synchronous execution post handling
                    showLocalMpinSetupDialog()
                }
            )
            forgotMpinSheet.show(childFragmentManager, "ForgotMpinBottomSheet")
        }

        binding.btnFingerprintTrigger.setOnClickListener { showBiometricPrompt() }
        binding.btnFaceUnlockTrigger.setOnClickListener { showBiometricPrompt() }
        binding.layoutActiveDots.setOnClickListener { showMpinBottomSheet() }
    }

    private fun showLocalMpinSetupDialog() {
        // 🚀 2026 UI Architecture: Programmatically structured clean layout encapsulation frame using device density metrics
        val containerFrame = android.widget.FrameLayout(requireContext()).apply {
            val paddingHorizontal = (24 * resources.displayMetrics.density).toInt()
            val paddingTop = (12 * resources.displayMetrics.density).toInt()
            setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        // 🚀 FIXED: Routed ContextThemeWrapper straight into your Centralized Custom Global XML Style Profile
        // This eliminates all manual cursor tint hacks, text colors, error boundaries, and selection pointer issues completely
        val styledContext = android.view.ContextThemeWrapper(
            requireContext(),
            R.style.Widget_Material3_TextInputLayout_OutlinedBox_CustomGlobal
        )

        val textInputLayout = com.google.android.material.textfield.TextInputLayout(
            styledContext,
            null,
            com.google.android.material.R.attr.textInputStyle
        ).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            // Enforce password visibility icons natively backed by your central system parameters
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }

        val etMpin = com.google.android.material.textfield.TextInputEditText(textInputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = "Enter New 4-Digit MPIN"
            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        // 🚀 FIXED: Dynamic reactive observer clearing error frames smoothly the moment user clicks or updates data streams
        etMpin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textInputLayout.error = null
                textInputLayout.isErrorEnabled = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        textInputLayout.addView(etMpin)
        containerFrame.addView(textInputLayout)

        // 🚀 2026 Human-Centric Master Alert Dialog Configuration System
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Reset Secure MPIN")
            .setMessage(getString(R.string.mpin_reset_dialog_msg))
            .setView(containerFrame)
            .setCancelable(false)
            .setPositiveButton("Save MPIN", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // 🚀 FIXED: Dynamic fluid boundaries scaling dialog component horizontally to exactly 92% layout metrics parameters
        val metricsWidth = (resources.displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(metricsWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val mpin = etMpin.text.toString().trim()

            // 🚀 FIXED: Resolved references by binding accurately to your class initialized variable token layer 'bioSettingsManager'
            val previouslySavedMpin = bioSettingsManager.getMPIN()

            when {
                mpin.length != 4 -> {
                    textInputLayout.isErrorEnabled = true
                    textInputLayout.error = "MPIN requires exactly 4 digits"
                    // Globally verified enterprise security feedback trigger
                    AppSettings.triggerErrorEffect(requireContext(), textInputLayout)
                }
                previouslySavedMpin != null && mpin == previouslySavedMpin -> {
                    textInputLayout.isErrorEnabled = true
                    textInputLayout.error = getString(R.string.error_duplicate_mpin)
                    AppSettings.triggerErrorEffect(requireContext(), textInputLayout)
                }
                else -> {
                    textInputLayout.error = null
                    viewLifecycleOwner.lifecycleScope.launch {
                        bioSettingsManager.saveMPIN(mpin)
                        dataStoreManager.saveMpinStatus(true)
                        toast("Security MPIN updated successfully! 🔒")
                        dialog.dismiss()
                        checkAdaptiveSessionState()
                    }
                }
            }
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
            binding.tilPassword.error = "Account locked! Try after ${getRemainingLockTime(email)}"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        // 🚀 2026 Standard Form Sanitization: Clean separation of field emptiness vs format constraints
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
        // 🚀 2026 Standard Password Validation: Granular evaluation separating empty states from depth limits
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
    }

    private fun showForgotPasswordSheet() {
        val bottomSheet = ForgotPasswordBottomSheet()
        bottomSheet.show(childFragmentManager, "ForgotPasswordSheet")
    }

    private fun showBiometricPrompt() {
        biometricHelper.triggerAuthentication(
            title = "Secure Biometric Login",
            subtitle = "Scan Fingerprint or Face for Secure Login.",
            onSuccess = { _ ->
                val savedEmail = bioSettingsManager.getSavedEmail()
                val savedPassword = bioSettingsManager.getSavedPass()

                if (!savedEmail.isNullOrEmpty() && savedPassword.isNotEmpty()) {
                    viewModel.loginWithEmail(savedEmail, savedPassword)
                } else {
                    proceedToHome()
                }
            },
            onError = { _ ->
                showMpinBottomSheet()
            }
        )
    }

    private fun showMpinBottomSheet() {
        val mpinSheet = MpinBottomSheet(
            onMpinSuccess = { email, pass ->
                if (!email.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                    viewModel.loginWithEmail(email, pass)
                } else {
                    proceedToHome()
                }
            },
            onBiometricRequest = {
                showBiometricPrompt()
            },
            onForgotMpinRequested = {
                val currentInputtedPass = binding.etPassword.text.toString().trim()
                val forgotSheet = ForgotMpinBottomSheet(
                    runtimePasswordFallback = currentInputtedPass,
                    onResetVerified = {
                        // 🚀 FIXED: Routed the verification stream back to the global material dialog pipeline
                        showLocalMpinSetupDialog()
                    }
                )
                forgotSheet.show(childFragmentManager, "ForgotMpinBottomSheet")
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
        // 🚀 FIXED: Explicitly stop background security ticker coroutines to completely eradicate structural leaks
        securityLockTickerJob?.cancel()
        viewModel.resetState()
        _binding = null
        super.onDestroyView()
    }
}