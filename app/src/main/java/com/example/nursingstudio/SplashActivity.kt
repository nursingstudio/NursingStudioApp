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


        // --- WORLD-CLASS LOGO POP-IN START ---
        lottie.addAnimatorUpdateListener { animation ->
            // Jab heartbeat 60% (0.6f) complete ho jaye, tab logo ko pop karwao
            if (animation.animatedFraction >= 0.7f && logo.alpha == 0f) {

                logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800) // Smooth duration
                    // OvershootInterpolator(2.0f) hi wo "Spring/Jhatka" effect deta hai
                    .setInterpolator(android.view.animation.OvershootInterpolator(3.0f))
                    .withEndAction {
                        // Jab logo settle ho jaye, tab 1.2/2 sec baad next screen
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkSessionAndNavigate()
                        }, 600)
                    }
                    .start()

                // Saath mein Lottie ko dheere se fade-out kar do
                lottie.animate().alpha(0f).setDuration(400).start()
            }
        }
        // --- WORLD-CLASS LOGO POP-IN END ---
    }


    private fun checkSessionAndNavigate() {
        // ViewModel ka use karke session check
        val viewModel = androidx.lifecycle.ViewModelProvider(this)[SplashViewModel::class.java]

        val intent = if (viewModel.isUserLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthActivity::class.java)
        }

        // Agli screen ko background mein pehle hi ready karne ke liye flag
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)

        // Modern Professional Transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        finish()
    }
}