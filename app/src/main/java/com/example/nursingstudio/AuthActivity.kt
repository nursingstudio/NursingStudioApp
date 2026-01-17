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
        // Firebase initialization (Wahi rakhein)
        com.google.firebase.FirebaseApp.initializeApp(this)
        val firebaseAppCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // --- ⚡ WORLD-CLASS OPTIMIZATION ⚡ ---
        // Yahan se Handler aur Logo animation hata diya hai
        // Taki Splash ke baad direct Login Fragment load ho bina kisi gap ke

        val sp = getSharedPreferences("session", MODE_PRIVATE)

        if (savedInstanceState == null) { // Taki screen rotate hone par baar-baar load na ho
            if (sp.getBoolean("logged_in", false)) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Bina kisi delay ke LoginFragment load karo
                supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, LoginFragment())
                    .commit()
            }
        }
    }
}