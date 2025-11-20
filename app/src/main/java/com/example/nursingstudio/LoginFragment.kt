package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

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

        btnLogin.setOnClickListener {
            val mob = etMobile.text.toString().trim()
            val pass = etPassword.text.toString()

            if (mob.length != 10) {
                toast("Enter 10-digit mobile")
                return@setOnClickListener
            }
            if (pass.length < 4) {
                toast("Password too short")
                return@setOnClickListener
            }

            // saved credentials read karo
            val sp = requireContext().getSharedPreferences("session", 0)
            val savedMob = sp.getString("reg_mobile", null)
            val savedPass = sp.getString("reg_password", null)

            if (mob == savedMob && pass == savedPass) {
                sp.edit().putBoolean("logged_in", true).apply()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            } else {
                toast("Invalid mobile or password")
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
