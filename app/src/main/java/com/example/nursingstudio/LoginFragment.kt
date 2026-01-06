package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.auth.login.LoginResult
import com.example.nursingstudio.auth.login.LoginViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.hbb20.CountryCodePicker
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val viewModel: LoginViewModel by viewModels()
    private var isEmailMode = true
    private var verificationId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        val tabLayout = view.findViewById<TabLayout>(R.id.loginTabLayout)
        val layoutEmail = view.findViewById<LinearLayout>(R.id.layoutEmailLogin)
        val layoutMobile = view.findViewById<LinearLayout>(R.id.layoutMobileLogin)
        val btnLoginAction = view.findViewById<Button>(R.id.btnLoginAction)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etMobile = view.findViewById<EditText>(R.id.etMobileLogin)
        val etOtp = view.findViewById<EditText>(R.id.etOtpLogin)
        val layoutOtp = view.findViewById<View>(R.id.layoutOtpLogin)
        val ccp = view.findViewById<CountryCodePicker>(R.id.ccpLogin)
        val progressBar = view.findViewById<ProgressBar>(R.id.loginProgress)

        ccp.registerCarrierNumberEditText(etMobile)

        // --- Tab Change ---
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isEmailMode = tab?.position == 0
                verificationId = null
                layoutOtp.visibility = View.GONE
                if (isEmailMode) {
                    layoutEmail.visibility = View.VISIBLE
                    layoutMobile.visibility = View.GONE
                    btnLoginAction.text = "Login"
                } else {
                    layoutEmail.visibility = View.GONE
                    layoutMobile.visibility = View.VISIBLE
                    btnLoginAction.text = "Send OTP"
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // --- Observe ViewModel ---
        viewModel.loginStatus.observe(viewLifecycleOwner) { result ->
            when(result) {
                is LoginResult.Loading -> toggleLoading(true, btnLoginAction, progressBar)
                is LoginResult.Success -> {
                    toggleLoading(false, btnLoginAction, progressBar)
                    toast("Welcome back! ✨")
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
                is LoginResult.NoProfile -> {
                    toggleLoading(false, btnLoginAction, progressBar)
                    showNoProfileDialog()
                }
                is LoginResult.Error -> {
                    toggleLoading(false, btnLoginAction, progressBar)
                    toast(result.message)
                }
            }
        }

        // --- Main Button Action ---
        btnLoginAction.setOnClickListener {
            if (isEmailMode) {
                val email = etEmail.text.toString().trim()
                val pass = etPassword.text.toString().trim()
                if (email.isEmpty() || pass.isEmpty()) toast("Enter credentials")
                else viewModel.loginWithEmail(email, pass)
            } else {
                if (verificationId == null) {
                    val mobile = etMobile.text.toString().trim()
                    if (mobile.length < 10) toast("Enter valid number")
                    else sendOtp(ccp.fullNumberWithPlus, layoutOtp, btnLoginAction, progressBar)
                } else {
                    val otp = etOtp.text.toString().trim()
                    if (otp.length < 6) toast("Enter 6-digit OTP")
                    else {
                        val credential = PhoneAuthProvider.getCredential(verificationId!!, otp)
                        viewModel.loginWithPhone(credential)
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            val email = etEmail.text.toString().trim()
            showForgotPasswordDialog(email)
        }

        view.findViewById<TextView>(R.id.tvGoRegister).setOnClickListener { navigateToRegister() }
    }

    private fun sendOtp(fullPhone: String, otpLayout: View, btn: Button, pb: ProgressBar) {
        toggleLoading(true, btn, pb)
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(c: PhoneAuthCredential) { viewModel.loginWithPhone(c) }
                override fun onVerificationFailed(e: FirebaseException) {
                    toggleLoading(false, btn, pb)
                    toast("Failed: ${e.localizedMessage}")
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    toggleLoading(false, btn, pb)
                    verificationId = id
                    otpLayout.visibility = View.VISIBLE
                    btn.text = "Verify & Login"
                    toast("OTP Sent")
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun showNoProfileDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profile Not Found")
            .setMessage("Please register first.")
            .setCancelable(false)
            .setPositiveButton("Register Now") { _, _ -> auth.signOut(); navigateToRegister() }
            .setNegativeButton("Cancel") { _, _ -> auth.signOut() }.show()
    }

    private fun toggleLoading(isLoading: Boolean, btn: Button, pb: ProgressBar) {
        btn.isEnabled = !isLoading
        pb.visibility = if (isLoading) View.VISIBLE else View.GONE
        btn.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun navigateToRegister() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.auth_container, RegisterFragment()).commit()
    }

    private fun showForgotPasswordDialog(email: String) {
        val input = EditText(requireContext()).apply { hint = "Registered Email"; setText(email) }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password").setView(input)
            .setPositiveButton("Send Link") { _, _ ->
                val mail = input.text.toString().trim()
                if (mail.isNotEmpty()) auth.sendPasswordResetEmail(mail).addOnSuccessListener { toast("Link Sent!") }
            }.setNegativeButton("Cancel", null).show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()
}