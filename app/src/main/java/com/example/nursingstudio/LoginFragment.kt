package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etMobile = view.findViewById<EditText>(R.id.etMobile)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = view.findViewById<TextView>(R.id.tvForgotPassword)
        val tvGoRegister = view.findViewById<TextView>(R.id.tvGoRegister)

        val sp = requireContext().getSharedPreferences("session", 0)


        etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnLogin.performClick()
                true
            } else {
                false
            }
        }

        // LOGIN BUTTON
        btnLogin.setOnClickListener {
            btnLogin.animate()
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(80)
                .withEndAction {
                    btnLogin.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                }
                .start()

            val mob = etMobile.text.toString().trim()
            val pass = etPassword.text.toString()

            val savedMobile = sp.getString("reg_mobile", null)
            val savedPassword = sp.getString("reg_password", null)

            if (mob.isEmpty() || pass.isEmpty()) {
                Toast.makeText(requireContext(), "Enter mobile & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (mob == savedMobile && pass == savedPassword) {
                // login success
                sp.edit()
                    .putBoolean("logged_in", true)
                    .apply()

                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            } else {
                Toast.makeText(requireContext(), "Invalid mobile or password", Toast.LENGTH_SHORT).show()
            }
        }

        // FORGOT PASSWORD CLICK
        tvForgotPassword.setOnClickListener {
            // Abhi backend nahi, to simple info de rahe hain
            Toast.makeText(
                requireContext(),
                "Forgot password feature will use OTP / email later.",
                Toast.LENGTH_LONG
            ).show()
        }

        // NEW USER? REGISTER FIRST
        tvGoRegister.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
