package com.example.nursingstudio.auth.login

import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.example.nursingstudio.AppSettings
import com.example.nursingstudio.AuthActivity
import com.example.nursingstudio.utils.BiometricSettingsManager

class LoginFragment : Fragment() {

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
                countDownTimer?.cancel()

                val params =
                    binding.btnLoginAction.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

                // Standard Margin calculation (World-class practice)
                val marginInPx = (12 * resources.displayMetrics.density).toInt()
                params.topMargin = marginInPx
                params.bottomMargin = 0 // Extra bottom space remove karne ke liye

                if (tab?.position == 0) {
                    // Email Mode
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = "Login"
                    params.topToBottom = binding.layoutEmailLogin.id
                } else {
                    // Mobile Mode
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
                    unlockMobileField()
                    params.topToBottom = binding.layoutMobileLogin.id
                }

                binding.btnLoginAction.layoutParams = params
                binding.root.post { binding.root.requestLayout() }
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

        clearAllErrors() // Naya check shuru karne se pehle purane errors hatao

        if (email.isEmpty()) {
            binding.tilEmail.isErrorEnabled = true // Space on karein
            binding.tilEmail.error = "Email is required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid Email format"
            return
        }
        if (pass.isEmpty()) {
            binding.tilPassword.isErrorEnabled = true // Space on karein
            binding.tilPassword.error = "Password is required"
            return
        }

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
                is LoginViewModel.LoginResult.Success -> {
                    countDownTimer?.cancel()
                    val bioManager = BiometricSettingsManager(requireContext())

                    // Agar biometric enabled nahi hai, toh SETUP dikhao
                    if (!bioManager.isBiometricEnabled()) {
                        val currentTab = binding.loginTabLayout.selectedTabPosition
                        if (currentTab == 0) {
                            // Email Login Case
                            val email = binding.etEmail.text.toString().trim()
                            val pass = binding.etPassword.text.toString().trim()
                            showBiometricSetupDialog(email, pass, true)
                        } else {
                            // Mobile Login Case
                            val mobile = binding.etMobileLogin.text.toString().trim()
                            showBiometricSetupDialog(mobile, "OTP_USER", false)
                        }
                    } else {
                        proceedToHome()
                    }
                }
                is LoginViewModel.LoginResult.NoProfile -> {
                    countDownTimer?.cancel()
                    binding.loadingOverlay.visibility = View.GONE
                    toast("No profile found. Redirecting to Register...")
                    binding.root.postDelayed({
                        if (isAdded) (activity as? AuthActivity)?.showRegister()
                    }, 1000)
                }
                is LoginViewModel.LoginResult.Error -> {
                    toast(result.message)
                    binding.btnLoginAction.text = if (binding.tilOtp.visibility == View.VISIBLE) "Verify & Login" else "Login"
                }
                else -> {}
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
        binding.etEmail.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Typing shuru hote hi error aur uski reserved space dono hatao
                binding.tilEmail.error = null
                binding.tilEmail.isErrorEnabled = false
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                binding.tilPassword.isErrorEnabled = false
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Mobile aur OTP watchers ke liye bhi same logic:
        binding.etMobileLogin.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilMobile.error = null
                binding.tilMobile.isErrorEnabled = false
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: android.text.Editable?) {}
        })

        binding.etOtpLogin.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilOtp.error = null
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: android.text.Editable?) {}
        })
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
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilForgotEmail.isErrorEnabled = true
                tilForgotEmail.error = "Invalid email format"
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
                    // World-class Auto Login: Saved credentials se login karein
                    val manager = BiometricSettingsManager(requireContext())
                    val email = manager.getSavedEmail()
                    val pass = manager.getSavedPass()
                    if (email != null && pass != null) {
                        viewModel.loginWithEmail(email, pass)
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
                // Step 1: MPIN set karwao pehle (Professional way)
                showMPINSetupDialogDuringLogin(identifier, pass, isEmail)
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
                    if (isEmail) manager.saveCredentials(identifier, pass)
                    else manager.saveCredentials(identifier, "OTP_USER")

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
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_mpin_keypad, null)
        var enteredMpin = ""
        val bioManager = BiometricSettingsManager(requireContext())
        val correctMpin = bioManager.getMPIN()

        // Helper to handle key clicks
        val handleKeyClick: (String) -> Unit = { key ->
            if (enteredMpin.length < 4) {
                enteredMpin += key
                updateMpinDots(view, enteredMpin.length)

                if (enteredMpin.length == 4) {
                    if (enteredMpin == correctMpin) {
                        dialog.dismiss()
                        val email = bioManager.getSavedEmail()
                        val pass = bioManager.getSavedPass()

                        if (email != null && pass != null) {
                            if (pass == "OTP_USER") {
                                // Agar mobile user hai toh seedha home (kyunki OTP session managed hota hai)
                                proceedToHome()
                            } else {
                                // Email user hai toh re-login for security
                                viewModel.loginWithEmail(email, pass)
                            }
                        }
                    } else {
                        AppSettings.triggerVibration(requireContext(), 200) // Wrong MPIN feedback
                        toast("Incorrect MPIN! Please try again.")
                        enteredMpin = ""
                        updateMpinDots(view, 0)
                    }
                }
            }
        }

        // 1 to 9 Buttons setup
        val numberButtons = listOf(
            R.id.btnKey1 to "1", R.id.btnKey2 to "2", R.id.btnKey3 to "3",
            R.id.btnKey4 to "4", R.id.btnKey5 to "5", R.id.btnKey6 to "6",
            R.id.btnKey7 to "7", R.id.btnKey8 to "8", R.id.btnKey9 to "9",
            R.id.btnKey0 to "0"
        )

        numberButtons.forEach { (id, value) ->
            view.findViewById<com.google.android.material.button.MaterialButton>(id).setOnClickListener {
                handleKeyClick(value)
            }
        }

        // Delete Button logic
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnKeyDel).setOnClickListener {
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
        // Fragment band hote hi timer ko turant cancel karein
        countDownTimer?.cancel()
        countDownTimer = null

        super.onDestroyView()
        _binding = null
    }
}