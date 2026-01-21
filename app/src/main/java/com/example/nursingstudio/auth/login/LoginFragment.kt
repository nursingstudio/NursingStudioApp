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
                if (tab?.position == 0) {
                    binding.layoutEmailLogin.visibility = View.VISIBLE
                    binding.layoutMobileLogin.visibility = View.GONE
                } else {
                    binding.layoutEmailLogin.visibility = View.GONE
                    binding.layoutMobileLogin.visibility = View.VISIBLE
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
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (email.isEmpty()) { toast("Enter Email"); return }
        if (pass.isEmpty()) { toast("Enter Password"); return }

        viewModel.loginWithEmail(email, pass)
    }

    private fun performMobileLogin() {
        val mobile = binding.etMobileLogin.text.toString().trim()
        val otp = binding.etOtpLogin.text.toString().trim()

        if (binding.layoutOtpLogin.visibility == View.GONE) {
            // ... (Puraana mobile send logic same rahega)
            if (mobile.length != 10) {
                binding.etMobileLogin.error = "Enter 10 digit number" // Red Border
                return
            }
            // ... (Aapka existing sendOtp code)
            binding.loadingOverlay.visibility = View.VISIBLE
            (activity as? AuthActivity)?.sendOtp(mobile) { id ->
                binding.loadingOverlay.visibility = View.GONE
                verificationId = id
                binding.layoutOtpLogin.visibility = View.VISIBLE
                binding.tvChangeNumber.visibility = View.VISIBLE
                binding.etMobileLogin.isEnabled = false
                binding.etMobileLogin.alpha = 0.6f
                startLoginTimer()
                binding.btnLoginAction.text = "Verify & Login"
                AppSettings.triggerVibration(requireContext(), 100)
                toast("OTP Sent Successfully! ✨")
            }
        } else {
            // OTP Verification Logic
            if (otp.length != 6) {
                // World-Class Red Border Error
                binding.etOtpLogin.error = "Enter 6 digit OTP"
                binding.etOtpLogin.requestFocus()
                return
            }

            binding.etOtpLogin.error = null // Error clear karo
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
                        binding.etOtpLogin.error = "Invalid OTP. Please try again."
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
        binding.etMobileLogin.isEnabled = true
        binding.ccpLogin.setCcpClickable(true)
        binding.etMobileLogin.alpha = 1.0f
        binding.layoutOtpLogin.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        binding.btnLoginAction.text = "Login"
        binding.etOtpLogin.setText("")
        binding.etMobileLogin.requestFocus()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}