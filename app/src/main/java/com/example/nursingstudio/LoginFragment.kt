package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
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

        // View Mappings
        val tabLayout = view.findViewById<TabLayout>(R.id.loginTabLayout)
        val layoutEmail = view.findViewById<LinearLayout>(R.id.layoutEmailLogin)
        val layoutMobile = view.findViewById<LinearLayout>(R.id.layoutMobileLogin)
        val btnLoginAction = view.findViewById<Button>(R.id.btnLoginAction)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)

        // Mobile Fields
        val etMobile = view.findViewById<EditText>(R.id.etMobileLogin)
        val etOtp = view.findViewById<EditText>(R.id.etOtpLogin)
        val layoutOtp = view.findViewById<View>(R.id.layoutOtpLogin)
        val ccp = view.findViewById<CountryCodePicker>(R.id.ccpLogin)

        // 🔥 VITAL STEP: CCP ko mobile field se connect karo
        ccp.registerCarrierNumberEditText(etMobile)


        // Switch Logic
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    isEmailMode = true
                    layoutEmail.visibility = View.VISIBLE
                    layoutMobile.visibility = View.GONE
                    btnLoginAction.text = "Login"
                } else {
                    isEmailMode = false
                    layoutEmail.visibility = View.GONE
                    layoutMobile.visibility = View.VISIBLE
                    btnLoginAction.text = "Send OTP"
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnLoginAction.setOnClickListener {
            if (isEmailMode) {
                performEmailLogin(etEmail.text.toString(), etPassword.text.toString())
            } else {
                if (verificationId == null) {
                    sendOtp(ccp.fullNumberWithPlus, etMobile.text.toString(), layoutOtp, btnLoginAction)
                } else {
                    verifyOtp(etOtp.text.toString())
                }
            }
        }

        view.findViewById<TextView>(R.id.tvForgotPassword).setOnClickListener {
            showForgotPasswordDialog(etEmail.text.toString())
        }

        view.findViewById<TextView>(R.id.tvGoRegister).setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, RegisterFragment()).addToBackStack(null).commit()
        }
    }

    private fun performEmailLogin(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) { toast("Please fill credentials"); return }

        auth.signInWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
            checkUserInFirestore(result.user?.uid)
        }.addOnFailureListener { toast("Auth Failed: ${it.message}") }
    }

    private fun sendOtp(fullPhone: String, mobile: String, otpLayout: View, btn: Button) {
        if (mobile.length < 10) { toast("Invalid Number"); return }

        PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(fullPhone).setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(requireActivity())
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(c: PhoneAuthCredential) { loginWithCredential(c) }
                override fun onVerificationFailed(e: FirebaseException) { toast(e.message ?: "Error") }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    otpLayout.visibility = View.VISIBLE
                    btn.text = "Verify & Login"
                    toast("OTP Sent!")
                }
            }).build())
    }

    private fun verifyOtp(code: String) {
        if (code.length < 6) { toast("Enter full OTP"); return }
        loginWithCredential(PhoneAuthProvider.getCredential(verificationId!!, code))
    }

    private fun loginWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnSuccessListener { result ->
            checkUserInFirestore(result.user?.uid)
        }.addOnFailureListener { toast("Login Failed") }
    }

    private fun checkUserInFirestore(uid: String?) {
        if (uid == null) return
        db.collection("Users").document(uid).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                toast("Profile not found. Please register."); auth.signOut()
            }
        }
    }

    private fun showForgotPasswordDialog(email: String) {
        val input = EditText(requireContext()).apply { hint = "Registered Email"; setText(email) }
        MaterialAlertDialogBuilder(requireContext()).setTitle("Reset Password").setView(input)
            .setPositiveButton("Send Link") { _, _ ->
                auth.sendPasswordResetEmail(input.text.toString()).addOnSuccessListener { toast("Link Sent!") }
            }.show()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()
}