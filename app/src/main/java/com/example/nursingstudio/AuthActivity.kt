package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val sp = getSharedPreferences("session", MODE_PRIVATE)

        window.decorView.postDelayed({
            if (sp.getBoolean("logged_in", false)) {
                startActivity(Intent(this, MainActivity::class.java))
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, LoginFragment())
                    .commit()
            }
        }, 300)
    }
}
