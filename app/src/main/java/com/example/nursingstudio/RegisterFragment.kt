package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etMobile = view.findViewById<EditText>(R.id.etMobile)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val mob = etMobile.text.toString().trim()
            val pass = etPassword.text.toString()

            if (name.isEmpty()) {
                toast("Enter name")
                return@setOnClickListener
            }
            if (mob.length != 10) {
                toast("Enter 10-digit mobile")
                return@setOnClickListener
            }
            if (pass.length < 4) {
                toast("Password too short")
                return@setOnClickListener
            }

            // 1) SharedPreferences me save
            val sp = requireContext().getSharedPreferences("session", 0)
            sp.edit()
                .putString("reg_name", name)
                .putString("reg_mobile", mob)
                .putString("reg_password", pass)
                .apply()

            // 2) Form fields ko clear karo
            etName.text.clear()
            etMobile.text.clear()
            etPassword.text.clear()

            // 3) Toast
            toast("Registered! Now login.")

            // 4) Turant LoginFragment open karo
            parentFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }
    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }