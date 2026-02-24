package com.example.nursingstudio.auth.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.MainActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentLoginBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.PhoneAuthProvider
import com.example.nursingstudio.AuthActivity
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.google.android.material.button.MaterialButton

class LoginFragment : Fragment() {

    private var loginAttempts = 0

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_TIME_HOURS = 6
    }
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var verificationId: String? = null
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabSelection()
        setupClickListeners()
        setupTextWatchers() // Naya function
        observeViewModel()

        // Tagline ko thoda animation dena (Optional but Professional)
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(0.8f).setDuration(1000).start()

        // Button Effects
        AppSettings.setPushEffect(binding.btnLoginAction)
        // Check for returning biometric user
        val bioManager = BiometricSettingsManager(requireContext())
        if (bioManager.isBiometricEnabled()) {
            // Chhota delay taaki UI load ho jaye phir prompt aaye
            binding.root.postDelayed({
                showBiometricPrompt()
            }, 500)
        }
    }

    private fun setupTabSelection() {
        binding.loginTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                clearAllErrors()
                clearAllFields()
                // Tab change hone par button reset karein
                binding.btnLoginAction.isEnabled = true
                binding.btnLoginAction.alpha = 1.0f
                countDownTimer?.cancel()

                // Barrier ki wajah se humein params change karne ki zarurat nahi hai
                // Sirf visibility toggle karni hai
                if (tab?.position == 0) {
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = "Login"
                } else {
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
                    unlockMobileField()
                }

                // Force layout update taaki barrier recalculate ho jaye
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
            // Fragment transaction logic to Register
            (activity as? AuthActivity)?.showRegister()
        }
        binding.tvChangeNumber.setOnClickListener { unlockMobileField() }
        binding.btnResendOtp.setOnClickListener {
            // Resend ke liye pehle verificationId null karo taki fresh call ho
            verificationId = null
            binding.tilOtp.visibility = View.GONE // Reset view to send mode
            performMobileLogin()
        }
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordSheet()
        }
    }

    private fun performEmailLogin() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        // 1. Sabse pehle Lock Check (Agar locked hai toh aage badhne ka sawal hi nahi)
        if (isUserLocked(email)) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "This account is locked. Try after ${getRemainingLockTime(email)} hrs"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        // 2. Email Validations
        if (email.isEmpty()) {
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Email is required"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Invalid Email format"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
            return
        }

        // 3. Password Validations (World-Class Update)
        if (pass.isEmpty()) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "Password is required"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        // --- ⭐ NEW: Anti-Brute Force Filter ⭐ ---
        // Kyunki ab humne Register mein password min 8 ka kar diya hai,
        // toh 8 se chota password hamesha galat hi hoga.
        // Ise yahan rokne se 'loginAttempts' count nahi badhega.
        if (pass.length < 8) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "Incorrect password" // Error generic rakhein security ke liye
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return // Yahan se exit! Attempt count safe rahega.
        }

        // 4. Sab sahi hai tabhi server hit karein
        viewModel.loginWithEmail(email, pass)
    }

    private fun performMobileLogin() {
        val mobile = binding.etMobileLogin.text.toString().trim()
        val otp = binding.etOtpLogin.text.toString().trim()

        if (binding.tilOtp.visibility == View.GONE) {
            // ... (Puraana mobile send logic same rahega)
            if (mobile.length != 10) {
                binding.tilMobile.isErrorEnabled = true
                binding.tilMobile.error = "Enter 10 digit number" // TIL use karo border ke liye
                // Shake Mobile Field
                AppSettings.triggerErrorEffect(requireContext(), binding.tilMobile)
                return
            }
            // ... (Aapka existing sendOtp code)
            binding.loadingOverlay.visibility = View.VISIBLE
            (activity as? AuthActivity)?.sendOtp(mobile) { id ->
                binding.loadingOverlay.visibility = View.GONE
                verificationId = id
                binding.tilOtp.visibility = View.VISIBLE
                binding.tvChangeNumber.visibility = View.VISIBLE
                binding.tilMobile.isEnabled = false
                binding.tilMobile.alpha = 0.6f
                startLoginTimer()
                binding.btnLoginAction.text = "Verify & Login"
                AppSettings.triggerVibration(requireContext(), 100)
                toast("OTP Sent Successfully! ✨")
            }
        } else {
            // OTP Verification Logic
            if (otp.length != 6) {
                // World-Class Red Border Error
                binding.tilOtp.isErrorEnabled = true
                binding.tilOtp.error = "Enter 6 digit OTP"
                binding.tilOtp.requestFocus()
                // Shake OTP Field
                AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                return
            }

            binding.tilOtp.error = null // Error clear karo
            binding.loadingOverlay.visibility = View.VISIBLE
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
            viewModel.loginWithPhone(credential)
        }
    }

    private fun observeViewModel() {
        viewModel.loginStatus.observe(viewLifecycleOwner) { result ->
            if (result is LoginViewModel.LoginResult.Loading) {
                binding.loadingOverlay.visibility = View.VISIBLE
            } else {
                binding.loadingOverlay.visibility = View.GONE
            }

            when (result) {
                is LoginViewModel.LoginResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                }
                is LoginViewModel.LoginResult.Success -> {
                    loginAttempts = 0
                    val email = binding.etEmail.text.toString().trim()
                    requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
                        .edit().remove("lock_timestamp_$email").apply()

                    countDownTimer?.cancel()
                    val bioManager = BiometricSettingsManager(requireContext())
                    val currentTab = binding.loginTabLayout.selectedTabPosition
                    // YAHAN FIX HAI: Login type save karo pehle
                    bioManager.saveLoginType(currentTab)

                    if (!bioManager.isBiometricEnabled()) {

                        if (currentTab == 0) {
                            val email = binding.etEmail.text.toString().trim()
                            val pass = binding.etPassword.text.toString().trim()
                            showBiometricSetupDialog(email, pass, true)
                        } else {
                            val mobile = binding.etMobileLogin.text.toString().trim()
                            showBiometricSetupDialog(mobile, "OTP_USER", false)
                        }
                    } else {
                        proceedToHome()
                    }
                }
                is LoginViewModel.LoginResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE

                    // Fix: Email ko block se pehle define karein taaki niche 'isUserLocked' mein mil sake
                    val email = binding.etEmail.text.toString().trim()
                    val errorMessage = result.message ?: ""

                    if (binding.layoutEmailLogin.visibility == View.VISIBLE) {

                        // World-Class Smart Converter Logic
                        val friendlyMessage = when {
                            errorMessage.contains("invalid-credential", true) ||
                                    errorMessage.contains("wrong-password", true) -> "Incorrect Email or Password"

                            errorMessage.contains("user-not-found", true) -> "Account not found. Please Register first."

                            errorMessage.contains("network-request-failed", true) -> "No internet connection! Check your network."

                            errorMessage.contains("too-many-requests", true) -> "Too many attempts! Please try again later."

                            // Lock case handle karne ke liye (Agar Firebase se lock error aaye)
                            isUserLocked(email) -> "Account locked! Try after ${getRemainingLockTime(email)} hours."

                            else -> "Login Failed. Please check your details."
                        }

                        binding.tilPassword.isErrorEnabled = true
                        binding.tilPassword.error = friendlyMessage
                        AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                    }


                    if (binding.layoutEmailLogin.visibility == View.VISIBLE) {
                        loginAttempts++
                        val remaining = MAX_ATTEMPTS - loginAttempts

                        binding.tilPassword.isErrorEnabled = true

                        if (remaining > 0) {
                            // Toast ki jagah TIL par error set karein
                            binding.tilPassword.error = "Incorrect password! $remaining attempts left."
                        } else {
                            lockUser(email)
                            binding.tilPassword.error = "Attempts over! Account locked for 6 hours."
                        }

                        binding.tilPassword.requestFocus()
                        AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)

                    } else if (binding.tilOtp.visibility == View.VISIBLE) {
                        binding.tilOtp.isErrorEnabled = true
                        binding.tilOtp.error = "Invalid OTP! Please check again."
                        AppSettings.triggerVibration(requireContext(), 200)
                    }
                    binding.btnLoginAction.text = if (binding.tilOtp.visibility == View.VISIBLE) "Verify & Login" else "Login"
                }

                is LoginViewModel.LoginResult.NoProfile -> {
                    countDownTimer?.cancel()
                    binding.loadingOverlay.visibility = View.GONE
                    toast("No profile found. Redirecting to Register...")
                    binding.root.postDelayed({
                        if (isAdded) (activity as? AuthActivity)?.showRegister()
                    }, 1000)
                }
                else -> {
                    binding.loadingOverlay.visibility = View.GONE
                }
            }
        }
    }

    private fun verifyPasswordBeforeBiometric(identifier: String, pass: String, onVerified: () -> Unit) {
        binding.loadingOverlay.visibility = View.VISIBLE

        // World-class security: Firebase se check karo ki password sahi hai ya nahi
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(identifier, pass)

        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task ->
                binding.loadingOverlay.visibility = View.GONE
                if (task.isSuccessful) {
                    onVerified() // Password sahi hai, aage badho
                } else {
                    AppSettings.triggerVibration(requireContext(), 200)
                    toast("Invalid Password! Verification failed.")
                }
            }
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun proceedToHome() {
        if (isAdded && !requireActivity().isFinishing) {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun startLoginTimer() {
        binding.tvTimer.visibility = View.VISIBLE
        binding.btnResendOtp.visibility = View.GONE

        countDownTimer?.cancel()
        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) {
                // WORLD-CLASS SAFETY CHECK: Agar binding null hai ya fragment detach ho gaya hai toh kuch mat karo
                if (_binding != null && isAdded) {
                    binding.tvTimer.text = "Resend in ${m / 1000}s"
                } else {
                    // Agar fragment khatam ho gaya hai toh timer ko yahin stop kar do
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
        binding.ccpLogin.setCcpClickable(true)
        binding.tilMobile.alpha = 1.0f
        binding.tilOtp.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.btnLoginAction.text = "Login"
        binding.etOtpLogin.setText("")
        binding.tilMobile.requestFocus()
    }

    private fun clearAllErrors() {
        // IsErrorEnabled = false karne se hi extra space khatam hoti hai
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false

        binding.tilPassword.error = null
        binding.tilPassword.isErrorEnabled = false

        binding.tilMobile.error = null
        binding.tilMobile.isErrorEnabled = false

        binding.tilOtp.error = null
        binding.tilOtp.isErrorEnabled = false

        // World-class refinement: Layout ko force request karein gaps fix karne ke liye
        binding.btnLoginAction.post {
            val params = binding.btnLoginAction.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            params.topMargin = (12 * resources.displayMetrics.density).toInt()
            binding.btnLoginAction.layoutParams = params
        }

        // Mobile specific views ko GONE karein
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE

        // Agar mobile layout me OTP field visible thi to use hide karein
        binding.tilOtp.visibility = View.GONE
    }

    private fun clearAllFields() {
        binding.etEmail.setText("")
        binding.etPassword.setText("")
        binding.etMobileLogin.setText("")
        binding.etOtpLogin.setText("")
    }

    private fun setupTextWatchers() {
        // 1. Email Watcher (With Lock Check & Button State)
        binding.etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = s.toString().trim()
                val pass = binding.etPassword.text.toString().trim()

                binding.tilEmail.isErrorEnabled = false
                binding.tilEmail.error = null

                if (isUserLocked(email)) {
                    val timeLeft = getRemainingLockTime(email)
                    binding.tilEmail.isErrorEnabled = true
                    binding.tilEmail.error = "Account locked! Try after $timeLeft hours."
                    updateLoginButtonState(false) // Lock hai toh button OFF
                } else {
                    // Agar password bhi 8+ hai, tabhi button ON hoga
                    updateLoginButtonState(pass.length >= 8)
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // 2. Password Watcher (With Length Check)
        binding.etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s.toString().trim()
                val email = binding.etEmail.text.toString().trim()

                binding.tilPassword.error = null
                binding.tilPassword.isErrorEnabled = false

                // Button tabhi ON hoga jab lock na ho AUR password 8+ chars ho
                val isNotLocked = !isUserLocked(email)
                updateLoginButtonState(isNotLocked && pass.length >= 8)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Mobile & OTP (Purana logic sahi hai, bas consistency ke liye helper use karein)
        binding.etMobileLogin.addTextChangedListener(createTextWatcher { binding.tilMobile })
        binding.etOtpLogin.addTextChangedListener(createTextWatcher { binding.tilOtp })
    }

    // --- ⭐ Helper Function: Button State Manage Karne Ke Liye ⭐ ---
    private fun updateLoginButtonState(isEnabled: Boolean) {
        binding.btnLoginAction.isEnabled = isEnabled
        binding.btnLoginAction.alpha = if (isEnabled) 1.0f else 0.5f
    }

    // --- ⭐ Helper Function: Code Repeat Kam Karne Ke Liye ⭐ ---
    private fun createTextWatcher(getLayout: () -> com.google.android.material.textfield.TextInputLayout) =
        object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                getLayout().error = null
                getLayout().isErrorEnabled = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
    private fun showForgotPasswordSheet() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(
            requireContext(),
            R.style.BottomSheetDialogTheme
        )
        val view = layoutInflater.inflate(R.layout.layout_forgot_password, null)

        val btnReset =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnResetPassword)
        val etForgotEmail =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etForgotEmail)
        val tilForgotEmail =
            view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilForgotEmail)

        // Type karte hi error hatane ke liye
        etForgotEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilForgotEmail.error = null
                tilForgotEmail.isErrorEnabled = false
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: android.text.Editable?) {}
        })

        btnReset.setOnClickListener {
            val email = etForgotEmail.text.toString().trim()
            if (email.isEmpty()) {
                tilForgotEmail.isErrorEnabled = true
                tilForgotEmail.error = "Please enter your registered email"
                AppSettings.triggerErrorEffect(requireContext(), tilForgotEmail) // Shake it!
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilForgotEmail.isErrorEnabled = true
                tilForgotEmail.error = "Invalid email format"
                AppSettings.triggerErrorEffect(requireContext(), tilForgotEmail) // Shake it!
            } else {
                // Success Logic: Reset Link Send
                sendPasswordReset(email, dialog)
            }
        }
        dialog.setContentView(view)
        dialog.show()
    }

    private fun sendPasswordReset(
        email: String,
        dialog: com.google.android.material.bottomsheet.BottomSheetDialog
    ) {
        binding.loadingOverlay.visibility = View.VISIBLE // Main screen par loading dikhao

        com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(email)
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
        val executor = androidx.core.content.ContextCompat.getMainExecutor(requireContext())
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val manager = BiometricSettingsManager(requireContext())
                    val type = manager.getLoginType()
                    val identifier = manager.getSavedEmail() // This stores email or mobile
                    val pass = manager.getSavedPass()

                    if (type == 0) { // Email User
                        if (identifier != null && pass != null) {
                            viewModel.loginWithEmail(identifier, pass)
                        }
                    } else { // Mobile User
                        // Mobile user verified via Biometric/MPIN can go straight to home
                        // because they already authenticated via OTP once
                        proceedToHome()
                    }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Agar user ne "Negative Button" (MPIN) dabaya ya biometric fail hua
                    if (errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                        showMpinBottomSheet()
                    }
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Login")
            .setSubtitle("Scan fingerprint or use MPIN")
            .setNegativeButtonText("Use MPIN") // "Use Password" ki jagah "Use MPIN"
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showBiometricSetupDialog(identifier: String, pass: String, isEmail: Boolean) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Security Setup")
            .setMessage("Enable Fingerprint & MPIN for faster login?")
            .setPositiveButton("Setup Now") { _, _ ->
                if (isEmail) {
                    // Email user ke pas pass already hai aur verified hai
                    showMPINSetupDialogDuringLogin(identifier, pass, true)
                } else {
                    // Mobile user se pehle password mangenge verification ke liye
                    showPasswordVerificationForMobileUser(identifier)
                }
            }
            .setNegativeButton("Maybe Later") { _, _ -> proceedToHome() }
            .setCancelable(false)
            .show()
    }

    // Ye naya function hai LoginFragment ke liye
    private fun showMPINSetupDialogDuringLogin(identifier: String, pass: String, isEmail: Boolean) {
        val etMpin = android.widget.EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            hint = "Create 4-Digit MPIN"
            textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle("Create Login MPIN")
            .setView(etMpin)
            .setPositiveButton("Save & Enable") { _, _ ->
                val mpin = etMpin.text.toString()
                if (mpin.length == 4) {
                    val manager = BiometricSettingsManager(requireContext())
                    manager.setBiometricEnabled(true)
                    manager.saveMPIN(mpin)

                    if (isEmail || pass != "OTP_USER") {
                        manager.saveCredentials(identifier, pass)
                    } else {
                        // Ye tab hoga agar koi mobile user bina password verify kiye yahan tak aaya
                        toast("Verification Required")
                        return@setPositiveButton
                    }
                    Toast.makeText(requireContext(), "Biometric & MPIN Enabled! 🔒", Toast.LENGTH_SHORT).show()
                    proceedToHome()
                } else {
                    Toast.makeText(requireContext(), "Invalid MPIN", Toast.LENGTH_SHORT).show()
                    showMPINSetupDialogDuringLogin(identifier, pass, isEmail) // Retry
                }
            }
            .show()
    }

    private fun showMpinBottomSheet() {
        // 1. Dialog initialization with Glass Theme
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.GlassBottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_mpin_keypad, null)

        var enteredMpin = ""
        val bioManager = BiometricSettingsManager(requireContext())
        val correctMpin = bioManager.getMPIN()

        // 2. APPLY GLASS EFFECT (World-Class Step)
        dialog.window?.let { window ->
            // Android 12 (API 31) aur upar ke liye background blur trigger karein
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                window.attributes.blurBehindRadius = 30
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }

        // --- Baki logic same rahega (Keypad functionality) ---
        val handleKeyClick: (String) -> Unit = { key ->
            if (enteredMpin.length < 4) {
                enteredMpin += key
                updateMpinDots(view, enteredMpin.length)

                if (enteredMpin.length == 4) {
                    if (enteredMpin == correctMpin) {
                        dialog.dismiss()
                        val type = bioManager.getLoginType()
                        val savedEmailOrMobile = bioManager.getSavedEmail()
                        val savedPass = bioManager.getSavedPass()

                        // Professional check: Agar password 'OTP_USER' nahi hai, toh credentials se login karo
                        if (savedPass != "OTP_USER" && savedEmailOrMobile != null) {
                            // Hum mobile user ke case mein bhi Email se login karwa sakte hain
                            // kyunki humne password verify karwa ke save kiya hai.
                            val currentUserEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: savedEmailOrMobile
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
            dialog.dismiss() // Important: Biometric khulne se pehle sheet hide karein
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

    private fun showPasswordVerificationForMobileUser(mobile: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetView = layoutInflater.inflate(R.layout.layout_verify_for_biometric, null)

        // Glass Effect logic
        dialog.window?.let { window ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                window.attributes.blurBehindRadius = 30
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            }
        }

        val etPass = sheetView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etVerifyPassword)
        val btnVerify = sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVerifyAndSetMPIN)

        btnVerify.setOnClickListener {
            val inputPass = etPass.text.toString().trim()

            // 1. Basic Check
            if (inputPass.isEmpty()) {
                etPass.error = "Password is required for security"
                AppSettings.triggerErrorEffect(requireContext(), etPass) // Shake Effect
                return@setOnClickListener
            }

            // 2. Firebase Verification (The "Bestever" Security)
            // Hum current user ki email nikaal kar re-authenticate karenge
            val userEmail = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email

            if (userEmail != null) {
                verifyPasswordBeforeBiometric(userEmail, inputPass) {
                    // Ye block tabhi chalega jab password 100% sahi hoga
                    dialog.dismiss()
                    toast("Identity Verified! ✨")
                    showMPINSetupDialogDuringLogin(mobile, inputPass, false)
                }
            } else {
                // Fallback: Agar email nahi mil rahi (Rare case)
                toast("Security Error: Please login again with Email.")
            }
        }
        dialog.setContentView(sheetView)
        dialog.show()
    }

    // Updated functions with 'identifier' parameter
    private fun isUserLocked(identifier: String): Boolean {
        if (identifier.isEmpty()) return false

        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val lockTime = prefs.getLong("lock_timestamp_$identifier", 0)
        if (lockTime == 0L) return false

        val diff = System.currentTimeMillis() - lockTime
        val hoursPassed = diff / (1000 * 60 * 60)

        return if (hoursPassed >= LOCK_TIME_HOURS) {
            // Time over! Lock hata do
            prefs.edit().remove("lock_timestamp_$identifier").apply()
            false
        } else {
            true
        }
    }

    private fun lockUser(identifier: String) {
        requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
            .edit().putLong("lock_timestamp_$identifier", System.currentTimeMillis()).apply()
    }

    private fun getRemainingLockTime(identifier: String): String {
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val lockTime = prefs.getLong("lock_timestamp_$identifier", 0)
        val diff = System.currentTimeMillis() - lockTime
        val remainingMillis = (LOCK_TIME_HOURS * 60 * 60 * 1000) - diff

        val hours = (remainingMillis / (1000 * 60 * 60)) % 24
        val minutes = (remainingMillis / (1000 * 60)) % 60
        val seconds = (remainingMillis / 1000) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        // Fragment band hote hi timer ko turant cancel karein
        countDownTimer?.cancel()
        countDownTimer = null

        super.onDestroyView()
        _binding = null
    }
}