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

        val rootView = findViewById<View>(android.R.id.content)
        rootView.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )


        val logo = findViewById<ImageView>(R.id.imgSplash)
        logo.startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )


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

        Handler(Looper.getMainLooper()).postDelayed(
            {

                val sp = getSharedPreferences("session", MODE_PRIVATE)
                if (sp.getBoolean("logged_in", false)) {
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    // Login screen
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.auth_container, LoginFragment())
                        .commit()
                }

            }, 1200
        ) // 1200 ms = 1.2 seconds
    }
}

