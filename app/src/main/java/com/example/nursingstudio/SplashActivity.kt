package com.example.nursingstudio

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView // Ye add karein
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Ye add karein
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.delay // Ye add karein
import kotlinx.coroutines.launch // Ye add karein
import android.os.Build

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- YE LINE SABSE PEHLE ADD KAREIN ---
        // Isse Android 12 ka default white splash hat jayega
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashProvider ->
                splashProvider.remove()
            }
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val lottie = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val logo = findViewById<ImageView>(R.id.logoImageView)
        val footerText = findViewById<TextView>(R.id.poweredByText)

        // --- PRO ANIMATION START ---
        val fullText = "Powered by Nursing Studio"
        footerText.text = "" // Pehle khali rakhenge

        lifecycleScope.launch {
            delay(500) // Heartbeat shuru hone ke thodi der baad
            for (letter in fullText) {
                footerText.append(letter.toString())
                delay(40) // Letters assemble speed (pro-level smooth)
            }
        }
        // --- PRO ANIMATION END ---

        lottie.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                lottie.visibility = View.GONE
                logo.visibility = View.VISIBLE

                logo.alpha = 0f
                logo.animate().alpha(1f).setDuration(800).withEndAction {
                    // Jab logo fade-in ho jaye, tab next screen par jao
                    Handler(Looper.getMainLooper()).postDelayed({
                        checkSessionAndNavigate()
                    }, 1000)
                }.start()
            }
            override fun onAnimationStart(p0: Animator) {}
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationRepeat(p0: Animator) {}
        })
    }

    private fun checkSessionAndNavigate() {
        val sp = getSharedPreferences("session", MODE_PRIVATE)
        val intent = if (sp.getBoolean("logged_in", false)) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) // Smooth Transition
        finish()
    }
}