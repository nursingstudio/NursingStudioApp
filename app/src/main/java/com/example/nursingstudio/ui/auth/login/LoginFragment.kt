package com.example.nursingstudio.ui.auth.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

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

        // ⭐ World-Class: Check if Biometric is already enabled by user
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
        var attempts = prefs.getInt("attempts_$email", 0) + 1

        if (attempts >= MAX_ATTEMPTS) {
            val lockUntil = System.currentTimeMillis() + (LOCK_TIME_HOURS * 60 * 60 * 1000)
            prefs.edit().putLong("lock_timestamp_$email", lockUntil).putInt("attempts_$email", 0).apply()
        } else {
            prefs.edit().putInt("attempts_$email", attempts).apply()
        }
    }
    // ⭐ PROFESSIONAL LOCK HANDLER
    private fun checkAndHandleLock(email: String): Boolean {
        if (isUserLocked(email)) {
            val timeLeft = getRemainingLockTime(email)
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Account locked! Try after $timeLeft"
            updateLoginButtonState(false)

            // Auto-refresh timer logic (Har second update hoga)
            viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                kotlinx.coroutines.delay(1000)
                if (isAdded) checkAndHandleLock(email)
            }
            return true
        } else {
            binding.tilEmail.isErrorEnabled = false
            binding.tilEmail.error = null
            return false
        }
    }
    // --- 📊 VIEWMODEL OBSERVATION (Fixed Progress Bar) ---
    private fun observeViewModel() {
        viewModel.loginStatus.observe(viewLifecycleOwner) { result ->
            binding.loadingOverlay.visibility = if (result is LoginViewModel.LoginResult.Loading) View.VISIBLE else View.GONE

            when (result) {
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
                    val msg = result.message ?: ""

                    if (msg.contains("password", true) || msg.contains("credential", true)) {
                        recordFailedAttempt(email)
                        val remaining = MAX_ATTEMPTS - requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE).getInt("attempts_$email", 0)
                        binding.tilPassword.error = "Incorrect Password! $remaining attempts left."
                    } else {
                        toast(msg)
                    }
                    AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
                }
                else -> {}
            }
        }
    }
// --- ⌨️ TEXT WATCHERS (Restored Exactly) ---
    private fun setupTextWatchers() {
    binding.etEmail.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val email = s.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            binding.tilEmail.isErrorEnabled = false
            binding.tilEmail.error = null

            // ⭐ EXACT INTEGRATION: Check lock status as user types
            val isLocked = checkAndHandleLock(email)

            if (!isLocked) {
                // Agar user locked nahi hai tabhi button enable/disable check karo
                updateLoginButtonState(pass.length >= 8 && Patterns.EMAIL_ADDRESS.matcher(email).matches())
            }
        }
        override fun afterTextChanged(s: Editable?) {}
    })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = binding.etEmail.text.toString().trim()
                binding.tilPassword.error = null
                updateLoginButtonState(!isUserLocked(email) && s?.length ?: 0 >= 8)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMobileLogin.addTextChangedListener(createTextWatcher { binding.tilMobile })
        binding.etOtpLogin.addTextChangedListener(createTextWatcher { binding.tilOtp })
    }
    private fun setupTabSelection() {
        binding.loginTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                clearAllErrors()
                clearAllFields()
                binding.btnLoginAction.isEnabled = true
                binding.btnLoginAction.alpha = 1.0f
                countDownTimer?.cancel()

                if (tab?.position == 0) {
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = "Login"
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

        if (isUserLocked(email)) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "This account is locked. Try after ${getRemainingLockTime(email)} hrs"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        if (email.isEmpty()) {
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Email is required"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Invalid Email format"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
            return
        }
        if (pass.isEmpty()) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "Password is required"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }
        if (pass.length < 8) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "Incorrect password"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }
        viewModel.loginWithEmail(email, pass)
    }
    private fun performMobileLogin() {
        val mobile = binding.etMobileLogin.text.toString().trim()
        val otp = binding.etOtpLogin.text.toString().trim()

        if (binding.tilOtp.visibility == View.GONE) {
            if (mobile.length != 10) {
                binding.tilMobile.isErrorEnabled = true
                binding.tilMobile.error = "Enter 10 digit number"
                AppSettings.triggerErrorEffect(requireContext(), binding.tilMobile)
                return
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
            if (otp.length != 6) {
                binding.tilOtp.isErrorEnabled = true
                binding.tilOtp.error = "Enter 6 digit OTP"
                binding.tilOtp.requestFocus()
                AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                return
            }

            binding.tilOtp.error = null
            binding.loadingOverlay.visibility = View.VISIBLE
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            viewModel.loginWithPhone(credential)
        }
    }
    private fun proceedToHome() {
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            if (isAdded && activity != null) {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                activity?.finish()
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

    private fun updateLoginButtonState(isEnabled: Boolean) {
        binding.btnLoginAction.isEnabled = isEnabled
        binding.btnLoginAction.alpha = if (isEnabled) 1.0f else 0.5f
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
        val dialog = BottomSheetDialog(
            requireContext(),
            R.style.BottomSheetDialogTheme
        )
        val view = layoutInflater.inflate(R.layout.layout_forgot_password, binding.root as? ViewGroup, false)
        val btnReset =
            view.findViewById<MaterialButton>(R.id.btnResetPassword)
        val etForgotEmail =
            view.findViewById<TextInputEditText>(R.id.etForgotEmail)
        val tilForgotEmail =
            view.findViewById<TextInputLayout>(R.id.tilForgotEmail)

        etForgotEmail.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilForgotEmail.error = null
                tilForgotEmail.isErrorEnabled = false
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {}
        })

        btnReset.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilForgotEmail.isErrorEnabled = true
                tilForgotEmail.error = "Please enter your registered email"
                AppSettings.triggerErrorEffect(requireContext(), tilForgotEmail) // Shake it!
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilForgotEmail.isErrorEnabled = true
                tilForgotEmail.error = "Invalid email format"
                AppSettings.triggerErrorEffect(requireContext(), tilForgotEmail) // Shake it!
            } else {
                sendPasswordReset(email, dialog)
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }
    private fun sendPasswordReset(
        email: String,
        dialog: BottomSheetDialog
    ) {
        binding.loadingOverlay.visibility = View.VISIBLE

        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.loadingOverlay.visibility = View.GONE
                if (task.isSuccessful) {
                    dialog.dismiss()
                    toast("Reset link sent! Please check your email inbox. 📧")
                } else {
                    toast("Error: ${task.exception?.message}")
                }
            }
    }
    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val manager = BiometricSettingsManager(requireContext())
                    val type = manager.getLoginType()
                    val identifier = manager.getSavedEmail()
                    val pass = manager.getSavedPass()

                    if (type == 0) {
                        if (identifier != null && pass != null) {
                            viewModel.loginWithEmail(identifier, pass)
                        }
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
        val dialog = BottomSheetDialog(requireContext(), R.style.GlassBottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_mpin_keypad, binding.root as? ViewGroup, false)
        var enteredMpin = ""
        val bioManager = BiometricSettingsManager(requireContext())
        val correctMpin = bioManager.getMPIN()
        dialog.window?.let { window ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.attributes.blurBehindRadius = 30
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }
        val handleKeyClick: (String) -> Unit = { key ->
            if (enteredMpin.length < 4) {
                enteredMpin += key
                updateMpinDots(view, enteredMpin.length)
                if (enteredMpin.length == 4) {
                    if (enteredMpin == correctMpin) {
                        dialog.dismiss()
                        bioManager.getLoginType()
                        val savedEmailOrMobile = bioManager.getSavedEmail()
                        val savedPass = bioManager.getSavedPass()

                        if (savedPass != "OTP_USER" && savedEmailOrMobile != null) {
                            val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: savedEmailOrMobile
                            viewModel.loginWithEmail(currentUserEmail, savedPass)
                        } else {
                            proceedToHome()
                        }
                    }
                    else {
                        AppSettings.triggerVibration(requireContext(), 200)
                        toast("Incorrect MPIN! Please try again.")
                        enteredMpin = ""
                        updateMpinDots(view, 0)
                    }
                }
            }
        }
        val numberButtons = listOf(
            R.id.btnKey1 to "1", R.id.btnKey2 to "2", R.id.btnKey3 to "3",
            R.id.btnKey4 to "4", R.id.btnKey5 to "5", R.id.btnKey6 to "6",
            R.id.btnKey7 to "7", R.id.btnKey8 to "8", R.id.btnKey9 to "9",
            R.id.btnKey0 to "0"
        )
        numberButtons.forEach { (id, value) ->
            view.findViewById<MaterialButton>(id).setOnClickListener { handleKeyClick(value) }
        }
        view.findViewById<MaterialButton>(R.id.btnKeyBio).setOnClickListener {
            dialog.dismiss()
            showBiometricPrompt()
        }
        view.findViewById<MaterialButton>(R.id.btnKeyDel).setOnClickListener {
            if (enteredMpin.isNotEmpty()) {
                enteredMpin = enteredMpin.dropLast(1)
                updateMpinDots(view, enteredMpin.length)
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }
    private fun updateMpinDots(view: View, length: Int) {
        val dotsLayout = view.findViewById<LinearLayout>(R.id.layoutMpinDots)
        for (i in 0 until dotsLayout.childCount) {
            val dot = dotsLayout.getChildAt(i)
            if (i < length) {
                dot.setBackgroundResource(R.drawable.mpin_dot_filled)
            } else {
                dot.setBackgroundResource(R.drawable.mpin_dot_empty)
            }
        }
    }

    override fun onDestroyView() {
        countDownTimer?.cancel()
        countDownTimer = null
        _binding = null
        super.onDestroyView()
    }
}