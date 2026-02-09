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

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()
    private var verificationId: String? = null
    private var countDownTimer: android.os.CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
    }

    private fun setupTabSelection() {
        binding.loginTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                clearAllErrors()
                clearAllFields()
                countDownTimer?.cancel()

                val params = binding.btnLoginAction.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

                if (tab?.position == 0) {
                    // Email Mode
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = "Login"

                    // Button ko specifically Email layout ke niche anchor karo
                    params.topToBottom = binding.layoutEmailLogin.id
                    params.topMargin = 32 // Pixels mein (aap isse adjust kar sakte ho 32 ya 48)
                } else {
                    // Mobile Mode
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
                    unlockMobileField()

                    // Button ko specifically Mobile layout ke niche anchor karo
                    params.topToBottom = binding.layoutMobileLogin.id
                    params.topMargin = 32
                }

                // Force apply params
                binding.btnLoginAction.layoutParams = params

                // WORLD-CLASS TRICK: Layout ko force refresh karne ke liye
                binding.root.post {
                    binding.root.requestLayout()
                }
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
            if (result !is LoginResult.Loading) {
                binding.loadingOverlay.visibility = View.GONE
            }

            when (result) {
                is LoginResult.Loading -> {
                    binding.loadingOverlay.visibility = View.VISIBLE
                }
                is LoginResult.Success -> {
                    binding.loadingOverlay.visibility = View.GONE
                    val context = context ?: return@observe // Safe check
                    val intent = Intent(context, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                }
                is LoginResult.NoProfile -> {
                    binding.loadingOverlay.visibility = View.GONE
                    toast("No profile found. Redirecting to Register...")
                    (activity as? AuthActivity)?.showRegister()
                }
                is LoginResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    // Professional Red Border for Wrong OTP
                    if (binding.tilOtp.visibility == View.VISIBLE) {
                        binding.tilOtp.error = "Invalid OTP. Please try again."
                    }
                    toast(result.message)
                    binding.btnLoginAction.text = if (binding.tilOtp.visibility == View.VISIBLE) "Verify & Login" else "Login"
                }
            }
        }
    }
    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun startLoginTimer() {
        binding.tvTimer.visibility = View.VISIBLE
        binding.btnResendOtp.visibility = View.GONE

        countDownTimer?.cancel()
        countDownTimer = object : android.os.CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) {
                binding.tvTimer.text = "Resend in ${m / 1000}s"
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
        // Sabhi errors aur space reserve band karo
        binding.tilEmail.isErrorEnabled = false
        binding.tilPassword.isErrorEnabled = false
        binding.tilMobile.isErrorEnabled = false
        binding.tilOtp.isErrorEnabled = false

        binding.tilEmail.error = null
        binding.tilPassword.error = null

        // Sabhi mobile views ko GONE karo taaki wo space na khayein
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE

        // Refresh layout instantly
        binding.root.requestLayout()
        // Inhe GONE karne se extra space khatam hogi
        binding.layoutEmailLogin.requestLayout()
        binding.layoutMobileLogin.requestLayout()
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
                binding.tilEmail.error = null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                // Password eye icon wapas dikhane ke liye password visibility toggle reset
                binding.tilPassword.isErrorEnabled = false
                binding.tilPassword.isErrorEnabled = true
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.etMobileLogin.addTextChangedListener(object : android.text.TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilMobile.error = null
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
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.layout_forgot_password, null)

        val btnReset = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnResetPassword)
        val etForgotEmail = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etForgotEmail)
        val tilForgotEmail = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilForgotEmail)

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

    private fun sendPasswordReset(email: String, dialog: com.google.android.material.bottomsheet.BottomSheetDialog) {
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
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}