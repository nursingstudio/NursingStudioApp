package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Agar already logged in hai to seedha MainActivity me bhej do
        val sp = getSharedPreferences("session", MODE_PRIVATE)
        if (sp.getBoolean("logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_auth)

        // Default – Login fragment dikhayenge
        supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, LoginFragment())
            .commit()

        // Login button click
        findViewById<Button>(R.id.btnGoLogin).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }

        // Register button click
        findViewById<Button>(R.id.btnGoRegister).setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, RegisterFragment())
                .commit()
        }
    }
}
