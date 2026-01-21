package com.example.nursingstudio.auth.login

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
        observeViewModel()

        // Button Effects
        AppSettings.setPushEffect(binding.btnLoginAction)
    }

    private fun setupTabSelection() {
        binding.loginTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // World-Class: Switch hote hi purane errors aur text clear karo
                clearAllErrors()
                clearAllFields()

                if (tab?.position == 0) {
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                    binding.btnLoginAction.text = "Login"
                } else {
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
                    // Agar OTP layout khula reh gaya tha switch ke waqt toh use reset karo
                    unlockMobileField()
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
            binding.layoutOtpLogin.visibility = View.GONE // Reset view to send mode
            performMobileLogin()
        }
    }

    private fun performEmailLogin() {
        val email = binding.tilEmail.text.toString().trim()
        val pass = binding.tilPassword.text.toString().trim()

        clearAllErrors() // Naya check shuru karne se pehle purane errors hatao

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid Email format"
            return
        }
        if (pass.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }

        viewModel.loginWithEmail(email, pass)
    }

    private fun performMobileLogin() {
        val mobile = binding.tilMobile.text.toString().trim()
        val otp = binding.tilOtp.text.toString().trim()

        if (binding.layoutOtpLogin.visibility == View.GONE) {
            // ... (Puraana mobile send logic same rahega)
            if (mobile.length != 10) {
                binding.tilMobile.error = "Enter 10 digit number" // TIL use karo border ke liye
                return
            }
            // ... (Aapka existing sendOtp code)
            binding.loadingOverlay.visibility = View.VISIBLE
            (activity as? AuthActivity)?.sendOtp(mobile) { id ->
                binding.loadingOverlay.visibility = View.GONE
                verificationId = id
                binding.layoutOtpLogin.visibility = View.VISIBLE
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
                    // World-Class Way: Pehle overlay hatao, fir navigate karo
                    binding.loadingOverlay.visibility = View.GONE
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish() // Ab crash nahi hoga
                }
                is LoginResult.NoProfile -> {
                    binding.loadingOverlay.visibility = View.GONE
                    toast("No profile found. Redirecting to Register...")
                    (activity as? AuthActivity)?.showRegister()
                }
                is LoginResult.Error -> {
                    binding.loadingOverlay.visibility = View.GONE
                    // Professional Red Border for Wrong OTP
                    if (binding.layoutOtpLogin.visibility == View.VISIBLE) {
                        binding.tilOtp.error = "Invalid OTP. Please try again."
                    }
                    toast(result.message)
                    binding.btnLoginAction.text = if (binding.layoutOtpLogin.visibility == View.VISIBLE) "Verify & Login" else "Login"
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
        binding.layoutOtpLogin.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.btnLoginAction.text = "Login"
        binding.tilOtp.setText("")
        binding.tilMobile.requestFocus()
    }

    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilMobile.error = null
        binding.tilOtp.error = null
    }

    private fun clearAllFields() {
        binding.tilEmail.setText("")
        binding.tilPassword.setText("")
        binding.tilMobile.setText("")
        binding.tilOtp.setText("")
    }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}