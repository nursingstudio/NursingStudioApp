package com.example.nursingstudio.com.example.nursingstudio.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nursingstudio.MainActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.com.example.nursingstudio.register.RegisterFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.hbb20.CountryCodePicker
import java.util.concurrent.TimeUnit

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isEmailMode = true
    private var verificationId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // View Mapping
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
        val progressBar = view.findViewById<ProgressBar>(R.id.loginProgress) // Ensure this exists in XML

        ccp.registerCarrierNumberEditText(etMobile)

        // --- Switch Logic (Tab Selection) ---
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isEmailMode = tab?.position == 0
                // Reset state when switching
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

        // --- Main Button Action ---
        btnLoginAction.setOnClickListener {
            if (isEmailMode) {
                val email = etEmail.text.toString().trim()
                val pass = etPassword.text.toString().trim()
                if (email.isEmpty() || pass.isEmpty()) {
                    toast("Please enter email and password")
                } else {
                    toggleLoading(true, btnLoginAction, progressBar)
                    performEmailLogin(email, pass, btnLoginAction, progressBar)
                }
            } else {
                if (verificationId == null) {
                    val mobile = etMobile.text.toString().trim()
                    if (mobile.length < 10) {
                        toast("Enter a valid 10-digit number")
                    } else {
                        toggleLoading(true, btnLoginAction, progressBar)
                        sendOtp(ccp.fullNumberWithPlus, layoutOtp, btnLoginAction, progressBar)
                    }
                } else {
                    val otp = etOtp.text.toString().trim()
                    if (otp.length < 6) {
                        toast("Enter 6-digit OTP")
                    } else {
                        toggleLoading(true, btnLoginAction, progressBar)
                        verifyOtp(otp, btnLoginAction, progressBar)
                    }
                }
            }
        }

        view.findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            showForgotPasswordDialog(etEmail.text.toString())
        }

        view.findViewById<TextView>(R.id.tvGoRegister).setOnClickListener {
            navigateToRegister()
        }
    }

    private fun performEmailLogin(email: String, pass: String, btn: Button, pb: ProgressBar) {
        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
            checkUserInFirestore(result.user?.uid, btn, pb)
        }.addOnFailureListener {
            toggleLoading(false, btn, pb)
            toast("Login Failed: ${it.localizedMessage}")
        }
    }

    private fun sendOtp(fullPhone: String, otpLayout: View, btn: Button, pb: ProgressBar) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(c: PhoneAuthCredential) {
                    loginWithCredential(c, btn, pb)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    toggleLoading(false, btn, pb)
                    toast("Failed: ${e.localizedMessage}")
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    toggleLoading(false, btn, pb)
                    verificationId = id
                    otpLayout.visibility = View.VISIBLE
                    btn.text = "Verify & Login"
                    toast("OTP Sent to $fullPhone")
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyOtp(code: String, btn: Button, pb: ProgressBar) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        loginWithCredential(credential, btn, pb)
    }

    private fun loginWithCredential(credential: PhoneAuthCredential, btn: Button, pb: ProgressBar) {
        auth.signInWithCredential(credential).addOnSuccessListener { result ->
            checkUserInFirestore(result.user?.uid, btn, pb)
        }.addOnFailureListener {
            toggleLoading(false, btn, pb)
            toast("OTP Verification Failed")
        }
    }

    private fun checkUserInFirestore(uid: String?, btn: Button, pb: ProgressBar) {
        if (uid == null) {
            toggleLoading(false, btn, pb)
            return
        }
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            toggleLoading(false, btn, pb)
            if (doc.exists()) {
                toast("Welcome back! ✨")
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                showNoProfileDialog()
            }
        }.addOnFailureListener {
            toggleLoading(false, btn, pb)
            toast("Database Error: ${it.localizedMessage}")
        }
    }

    private fun showNoProfileDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Profile Not Found")
            .setMessage("No account is linked with this credential. Please register first.")
            .setCancelable(false)
            .setPositiveButton("Register Now") { _, _ ->
                auth.signOut()
                navigateToRegister()
            }
            .setNegativeButton("Cancel") { _, _ -> auth.signOut() }
            .show()
    }

    private fun toggleLoading(isLoading: Boolean, btn: Button, pb: ProgressBar) {
        btn.isEnabled = !isLoading
        pb.visibility = if (isLoading) View.VISIBLE else View.GONE
        btn.alpha = if (isLoading) 0.5f else 1.0f
    }

    private fun navigateToRegister() {
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.auth_container, RegisterFragment())
            .commit()
    }

    private fun showForgotPasswordDialog(email: String) {
        val input = EditText(requireContext()).apply {
            hint = "Registered Email"
            setText(email)
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset Password")
            .setMessage("Enter your email to receive a reset link.")
            .setView(input)
            .setPositiveButton("Send Link") { _, _ ->
                val mail = input.text.toString().trim()
                if (mail.isNotEmpty()) {
                    auth.sendPasswordResetEmail(mail).addOnSuccessListener { toast("Link Sent!") }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()
}