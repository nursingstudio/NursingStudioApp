package com.example.nursingstudio

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val lottie = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val logo = findViewById<ImageView>(R.id.logoImageView)

        lottie.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                lottie.visibility = View.GONE
                logo.visibility = View.VISIBLE

                // Fade-in animation for logo
                logo.alpha = 0f
                logo.animate().alpha(1f).setDuration(1000).start()

                Handler(Looper.getMainLooper()).postDelayed({
                    checkSessionAndNavigate()
                }, 1500)
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
        finish()
    }
}