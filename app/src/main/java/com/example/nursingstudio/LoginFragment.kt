package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
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

        val etEmail = view.findViewById<EditText>(R.id.etMobile) // XML mein etMobile hai, par hum email use karenge login ke liye
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

        // LOGIN BUTTON
        btnLogin.setOnClickListener {
            // Tumhara animation logic
            btnLogin.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80).withEndAction {
                btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
            }.start()

            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            // Validations
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                toast("Please enter a valid email")
                return@setOnClickListener
            }
            if (pass.isEmpty()) {
                toast("Please enter password")
                return@setOnClickListener
            }

            // 🔥 FIREBASE LOGIN 🔥
            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    toast("Welcome back to Nursing Studio! ❤️")
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .addOnFailureListener { e ->
                    toast("Login Failed: ${e.message}")
                }
        }

        // FORGOT PASSWORD
        tvForgotPassword.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                toast("Enter your email above to reset password")
            } else {
                auth.sendPasswordResetEmail(email).addOnSuccessListener {
                    toast("Reset link sent to your email! 📧")
                }
            }
        }

        // NEW USER? REGISTER FIRST
        tvGoRegister.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}