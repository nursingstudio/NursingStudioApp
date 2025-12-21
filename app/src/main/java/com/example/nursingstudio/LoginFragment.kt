package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // --- SESSION MANAGEMENT (AUTO-LOGIN) ---
        if (auth.currentUser != null) {
            navigateToDashboard()
            return
        }

        val etEmail = view.findViewById<EditText>(R.id.etEmail) // Make sure ID is etEmail in XML
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
        val tvGoRegister = view.findViewById<TextView>(R.id.tvGoRegister)

        // Keyboard "Done" button logic
        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else false
        }

        // LOGIN BUTTON LOGIC
        btnLogin.setOnClickListener {
            // Button Animation
            btnLogin.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction {
                btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Professional Validations
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Please enter a valid email address.")
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                toast("Please enter your password.")
                return@setOnClickListener
            }

            // --- FIREBASE LOGIN WITH DATA CHECK ---
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener { authResult ->
                    val userId = authResult.user?.uid
                    if (userId != null) {
                        // Check if user exists in Firestore
                        FirebaseFirestore.getInstance().collection("Users").document(userId).get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    toast("Login successful! Welcome back.")
                                    navigateToDashboard()
                                } else {
                                    toast("User profile not found. Please register again.")
                                    auth.signOut()
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    toast("Authentication failed. Please check your credentials.")
                }
        }

        // FORGOT PASSWORD
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                toast("Please enter your registered email to reset password.")
            } else {
                auth.sendPasswordResetEmail(email).addOnSuccessListener {
                    toast("Password reset link has been sent to your email.")
                }.addOnFailureListener {
                    toast("Error: Could not send reset email.")
                }
            }
        }

        // NAVIGATION TO REGISTER
        tvGoRegister.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.auth_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}