package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.view.View


class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val logo = findViewById<ImageView>(R.id.imgSplash)
        logo.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )

        val sp = getSharedPreferences("session", MODE_PRIVATE)

        Handler(Looper.getMainLooper()).postDelayed({

            if (sp.getBoolean("logged_in", false)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, LoginFragment())
                    .commit()
            }

        }, 1200)
    }
}
