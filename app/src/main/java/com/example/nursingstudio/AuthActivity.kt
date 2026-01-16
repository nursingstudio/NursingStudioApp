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
import com.example.nursingstudio.auth.login.LoginFragment


class AuthActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🔥 1. Sabse pehle Firebase aur App Check (Yahan hona chahiye!)
        com.google.firebase.FirebaseApp.initializeApp(this)
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
            // Ye line token ko logcat mein print karegi
            android.util.Log.d("AppCheckDebug", "AuthActivity: Debug Provider Installed")
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        super.onCreate(savedInstanceState) // Iske baad super call
        setContentView(R.layout.activity_auth)

        // ... tumhara baki ka logo animation aur handler wala code

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
