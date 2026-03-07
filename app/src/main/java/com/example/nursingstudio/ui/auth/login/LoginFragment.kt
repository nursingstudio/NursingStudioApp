package com.example.nursingstudio.ui.auth.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.nursingstudio.MainActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentLoginBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.PhoneAuthProvider
import com.example.nursingstudio.AuthActivity
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.textfield.TextInputLayout
import java.util.Locale
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.lifecycle.withResumed

class LoginFragment : Fragment() {
    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_TIME_HOURS = 6
    }
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var verificationId: String? = null
    private var countDownTimer: CountDownTimer? = null

    // ⭐ 2026 Modern Activity Result Launcher (Replace old way)
    private val smsConsentLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(com.google.android.gms.auth.api.phone.SmsRetriever.EXTRA_SMS_MESSAGE)
            val otpCode = "\\d{6}".toRegex().find(message ?: "")?.value
            otpCode?.let {
                binding.etOtpLogin.setText(it)
                performMobileLogin()
            }
        }
    }
    private val smsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (com.google.android.gms.auth.api.phone.SmsRetriever.SMS_RETRIEVED_ACTION == intent?.action) {
                val extras = intent.extras ?: return

                // ⭐ 2026 Type-Safe Retrieval (Fixes Deprecation)
                val status = androidx.core.os.BundleCompat.getParcelable(
                    extras,
                    com.google.android.gms.auth.api.phone.SmsRetriever.EXTRA_STATUS,
                    com.google.android.gms.common.api.Status::class.java
                )

                when (status?.statusCode) {
                    com.google.android.gms.common.api.CommonStatusCodes.SUCCESS -> {
                        val message = extras.getString(com.google.android.gms.auth.api.phone.SmsRetriever.EXTRA_SMS_MESSAGE)
                        if (message != null) {
                            val otpCode = "\\d{6}".toRegex().find(message)?.value
                            otpCode?.let {
                                binding.etOtpLogin.setText(it)
                                performMobileLogin()
                            }
                        } else {
                            // ⭐ 2026 Standard: Trigger Consent without Deprecation
                            val consentIntent = androidx.core.os.BundleCompat.getParcelable(extras, com.google.android.gms.auth.api.phone.SmsRetriever.EXTRA_CONSENT_INTENT, Intent::class.java)
                            consentIntent?.let { smsConsentLauncher.launch(it) }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val intentFilter = android.content.IntentFilter(com.google.android.gms.auth.api.phone.SmsRetriever.SMS_RETRIEVED_ACTION)

        // ⭐ HIGH PRIORITY FOR AUTO-FILL
        intentFilter.priority = 1000

        ContextCompat.registerReceiver(
            requireActivity(),
            smsReceiver,
            intentFilter,
            com.google.android.gms.auth.api.phone.SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )

        // IMPORTANT: Clear previous instances
        com.google.android.gms.auth.api.phone.SmsRetriever.getClient(requireContext()).startSmsRetriever()
    }

    override fun onStop() {
        super.onStop()
        try {
            requireActivity().unregisterReceiver(smsReceiver)
        } catch (_: Exception) { /* Receiver not registered */ }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialUI()
        setupTabSelection()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
        startSmsListener()

        val bioManager = BiometricSettingsManager(requireContext())
        if (bioManager.isBiometricEnabled()) {
            binding.root.postDelayed({ showBiometricPrompt() }, 500)
        }
    }
    private fun setupInitialUI() {
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(0.8f).setDuration(1000).start()
        AppSettings.setPushEffect(binding.btnLoginAction)
    }
// --- 🛡️ LOCK LOGIC (Restored & Refined) ---
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

            // ⭐ GOLD STANDARD: SharedPreferences KTX Extension
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
    // ⭐ PROFESSIONAL LOCK HANDLER
    private fun checkAndHandleLock(email: String): Boolean {
        val isLocked = isUserLocked(email)

        if (isLocked) {
            // 1. Pehle UI ko turant update karein
            val timeLeft = getRemainingLockTime(email)
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Account locked! Try after $timeLeft"
            updateLoginButtonState()

            // 2. ⭐ MODERN COROUTINE: Har second UI update karne ke liye
            // repeatOnLifecycle ensure karta hai ki app background mein jate hi timer ruk jaye (Battery bachegi!)
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isUserLocked(email)) {
                        val currentTimeLeft = getRemainingLockTime(email)
                        binding.tilEmail.error = "Account locked! Try after $currentTimeLeft"

                        kotlinx.coroutines.delay(1000) // 1 second wait karein
                    }

                    // 3. Jaise hi lock khule, UI reset karein
                    binding.tilEmail.isErrorEnabled = false
                    binding.tilEmail.error = null
                    updateLoginButtonState()
                }
            }
        } else {
            // Agar user locked nahi hai
            binding.tilEmail.isErrorEnabled = false
            binding.tilEmail.error = null
        }

        return isLocked
    }
    // --- 📊 VIEWMODEL OBSERVATION (Fixed Progress Bar) ---
    private fun observeViewModel() {
        viewModel.loginStatus.observe(viewLifecycleOwner) { result ->
            binding.loadingOverlay.visibility = if (result is LoginViewModel.LoginResult.Loading) View.VISIBLE else View.GONE

            when (result) {
                is LoginViewModel.LoginResult.Loading -> {
                }

                is LoginViewModel.LoginResult.Success -> proceedToHome()

                is LoginViewModel.LoginResult.NoProfile -> {
                    countDownTimer?.cancel()
                    toast("No profile found. Redirecting to Register...")
                    binding.root.postDelayed({
                        if (isAdded) (activity as? AuthActivity)?.showRegister()
                    }, 1000)
                }

                is LoginViewModel.LoginResult.Error -> {
                    val email = binding.etEmail.text.toString().trim()
                    val msg = result.message

                    // ⭐ GOLD STANDARD: User-Friendly Message Mapping
                    val userFriendlyMsg = when {
                        // Network Errors
                        msg.contains("network", true) || msg.contains("timeout", true) || msg.contains("unreachable", true) ->
                            getString(R.string.no_internet)

                        // OTP Errors
                        msg.contains("OTP", true) || msg.contains("verification", true) || msg.contains("code", true) ->
                            "Invalid OTP! Please check and try again. 🔢"

                        // Credential Errors
                        msg.contains("password", true) || msg.contains("credential", true) ->
                            "Incorrect Password! Please try again. 🔑"

                        // Too many requests
                        msg.contains("too-many-requests", true) ->
                            "Too many attempts! Please try after some time. ⏳"

                        else -> msg // Fallback for unknown errors
                    }

                    when {
                        // 1. Agar OTP wala field dikh raha hai aur error OTP ka hai
                        !binding.tilOtp.isGone && (msg.contains("OTP", true) || msg.contains("verification", true)) -> {
                            binding.tilOtp.isErrorEnabled = true
                            binding.tilOtp.error = userFriendlyMsg
                            AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                        }

                        // 2. Agar Password wala field dikh raha hai aur error password ka hai
                        binding.layoutEmailLogin.isVisible && (msg.contains("password", true) || msg.contains("credential", true)) -> {
                            if (email.isNotEmpty()) recordFailedAttempt(email)

                            val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
                            val currentAttempts = prefs.getInt("attempts_$email", 0)
                            val remaining = MAX_ATTEMPTS - currentAttempts

                            binding.tilPassword.isErrorEnabled = true
                            binding.tilPassword.error = "$userFriendlyMsg ($remaining attempts left)"
                            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                        }

                        // 3. Network Error ya baki errors ke liye professional SnackBar ya Toast
                        else -> {
                            toast(userFriendlyMsg)
                            // Button ko shake karein taaki user ko feedback mile
                            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
                        }
                    }
                }
            }
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

        binding.etMobileLogin.addTextChangedListener(createTextWatcher { binding.tilMobile })
        binding.etOtpLogin.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilOtp.error = null
                binding.tilOtp.isErrorEnabled = false

                // ⭐ Professional Touch: Auto-perform action when 6 digits are reached
                if (s?.length == 6) {
                    hideKeyboard()
                    // Optional: You can even trigger the login button automatically here
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    private fun setupTabSelection() {
        binding.loginTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                clearAllErrors()
                clearAllFields()
                binding.btnLoginAction.isEnabled = true
                updateLoginButtonState()
                countDownTimer?.cancel()

                if (tab?.position == 0) {
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = getString(R.string.login)
                } else {
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
                    unlockMobileField()
                }
           binding.root.requestLayout()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    private fun setupClickListeners() {
        binding.btnLoginAction.setOnClickListener {
            hideKeyboard()
            val currentTab = binding.loginTabLayout.selectedTabPosition
            if (currentTab == 0) performEmailLogin() else performMobileLogin()
        }

        binding.tvGoRegister.setOnClickListener {
            (activity as? AuthActivity)?.showRegister()
        }
        binding.tvChangeNumber.setOnClickListener { unlockMobileField() }
        binding.btnResendOtp.setOnClickListener {
            verificationId = null
            binding.tilOtp.visibility = View.GONE
            performMobileLogin()
        }
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordSheet()
        }
    }
    private fun performEmailLogin() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        // 1. Connectivity Check (World-Class Security)
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
    private fun performMobileLogin() {
        val mobile = binding.etMobileLogin.text.toString().trim()
        val otp = binding.etOtpLogin.text.toString().trim()

        if (!isNetworkAvailable()) {
            toast(getString(R.string.no_internet))
            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
            return
        }
        if (binding.tilOtp.isGone) {
            when {
                mobile.isEmpty() -> {
                    binding.tilMobile.error = getString(R.string.error_mobile_empty)
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilMobile)
                    return
                }
                mobile.length != 10 -> {
                    binding.tilMobile.error = getString(R.string.error_mobile_invalid)
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilMobile)
                    return
                }
            }
            binding.loadingOverlay.visibility = View.VISIBLE
            (activity as? AuthActivity)?.sendOtp(mobile) { id ->
                binding.loadingOverlay.visibility = View.GONE
                verificationId = id
                binding.tilOtp.visibility = View.VISIBLE
                binding.tvChangeNumber.visibility = View.VISIBLE
                binding.tilMobile.isEnabled = false
                binding.tilMobile.alpha = 0.6f
                startLoginTimer()
                binding.btnLoginAction.text = getString(R.string.verify_login)
                AppSettings.triggerVibration(requireContext(), 100)
                toast("OTP Sent Successfully! ✨")
            }
        } else {
            when {
                otp.isEmpty() -> {
                    binding.tilOtp.error = getString(R.string.error_otp_empty)
                    binding.tilOtp.requestFocus()
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                    return
                }
                otp.length != 6 -> {
                    binding.tilOtp.error = getString(R.string.error_otp_invalid)
                    binding.tilOtp.requestFocus()
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                    return
                }
            }

            binding.tilOtp.error = null
            binding.tilOtp.isErrorEnabled = false
            binding.loadingOverlay.visibility = View.VISIBLE
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            viewModel.loginWithPhone(credential)
        }
    }
    private fun proceedToHome() {
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
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
    private fun startLoginTimer() {
        binding.tvTimer.visibility = View.VISIBLE
        binding.btnResendOtp.visibility = View.GONE

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) {
                if (_binding != null && isAdded) {
                    binding.tvTimer.text = getString(R.string.resend_in_s, m / 1000)
                } else {
                    countDownTimer?.cancel()
                }
            }

            override fun onFinish() {
                binding.tvTimer.visibility = View.GONE
                binding.btnResendOtp.visibility = View.VISIBLE
            }
        }.start()
    }
    private fun unlockMobileField() {
        countDownTimer?.cancel()
        binding.tilMobile.isEnabled = true
        binding.ccpLogin.isEnabled = true
        binding.tilMobile.alpha = 1.0f
        binding.tilOtp.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.btnLoginAction.text = getString(R.string.login)
        binding.etOtpLogin.setText("")
        binding.tilMobile.requestFocus()
    }
    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false
        binding.tilPassword.error = null
        binding.tilPassword.isErrorEnabled = false
        binding.tilMobile.error = null
        binding.tilMobile.isErrorEnabled = false
        binding.tilOtp.error = null
        binding.tilOtp.isErrorEnabled = false

        binding.btnLoginAction.post {
            val params = binding.btnLoginAction.layoutParams as ConstraintLayout.LayoutParams
            params.topMargin = (12 * resources.displayMetrics.density).toInt()
            binding.btnLoginAction.layoutParams = params
        }

        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE

        binding.tilOtp.visibility = View.GONE
    }
    private fun clearAllFields() {
        binding.etEmail.setText("")
        binding.etPassword.setText("")
        binding.etMobileLogin.setText("")
        binding.etOtpLogin.setText("")
    }

    private fun updateLoginButtonState() {
        binding.btnLoginAction.isEnabled = true
        binding.btnLoginAction.alpha = 1.0f
    }
    private fun createTextWatcher(getLayout: () -> TextInputLayout) =
        object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                getLayout().error = null
                getLayout().isErrorEnabled = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
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

                    when (manager.getLoginType()) {
                        0 -> {
                            val email = manager.getSavedEmail()
                            val password = manager.getSavedPass()

                            if (email != null) {
                                viewModel.loginWithEmail(email, password)
                            } else {
                                proceedToHome()
                            }
                        }
                        else -> {
                            proceedToHome()
                        }
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
                // Jab MPIN sahi ho jaye
                if (email != null && pass != null && pass != "OTP_USER") {
                    viewModel.loginWithEmail(email, pass)
                } else {
                    proceedToHome()
                }
            },
            onBiometricRequest = {
                // Jab user fingerprint button dabaye
                showBiometricPrompt()
            }
        )
        mpinSheet.show(childFragmentManager, "MpinSheet")
    }
    private fun startSmsListener() {
        // 2026 Industry Standard: Use User Consent API as fallback
        com.google.android.gms.auth.api.phone.SmsRetriever.getClient(requireContext())
            .startSmsUserConsent(null) // null means it will listen to any sender
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    // ⭐ 2026 GOLD STANDARD: Public Bridge for Instant Verification
    fun triggerAutoLogin(credential: com.google.firebase.auth.AuthCredential) {
        if (isAdded) {
            // Direct ViewModel ko call karein, user ko OTP screen bhi nahi dikhegi!
            viewModel.loginWithPhone(credential)
        }
    }
    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
        super.onDestroyView()
    }
}