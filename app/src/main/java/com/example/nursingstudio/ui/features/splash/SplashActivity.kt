package com.example.nursingstudio.ui.features.splash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.LottieAnimationView
import com.example.nursingstudio.R
import com.example.nursingstudio.ui.features.auth.AuthActivity
import com.example.nursingstudio.ui.features.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint // ✅ 1. Mandatory for Hilt to work
class SplashActivity : AppCompatActivity() {

    // Modern way to initialize ViewModel
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Android 12+ SplashScreen Fix
        splashScreen.setOnExitAnimationListener { it.remove() }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val lottie = findViewById<LottieAnimationView>(R.id.lottieAnimationView)
        val logo = findViewById<ImageView>(R.id.logoImageView)
        val footerText = findViewById<TextView>(R.id.poweredByText)

        // 2. Typing Animation (Improved)
        lifecycleScope.launch {
            val fullText = "Powered by Nursing Studio"
            footerText.text = ""
            delay(400)
            fullText.forEach { char ->
                footerText.append(char.toString())
                delay(50)
            }
        }

        // 3. Logo Pop & Session Logic
        lottie.addAnimatorUpdateListener { animation ->
            if (animation.animatedFraction >= 0.7f && logo.alpha == 0f) {
                lottie.animate().alpha(0f).setDuration(300).start()

                logo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(600)
                    .setInterpolator(OvershootInterpolator(2.5f))
                    .withEndAction {
                        // Handler hata kar lifecycleScope use kiya hai (Professional approach)
                        lifecycleScope.launch {
                            delay(200)
                            viewModel.checkUserSession()
                        }
                    }
                    .start()
            }
        }

        // 4. Observer: MVVM ka asli magic
        // ⬇️ Replace your old observer (Point 4) with this:
        observeViewModel()
    }
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ✅ 2026 Gold Standard: Collecting StateFlow
                viewModel.isLoggedIn.collect { status ->
                    status?.let { navigateToNextScreen(it) }
                }
            }
        }
    }

    private fun navigateToNextScreen(isLoggedIn: Boolean) {
        val targetClass = if (isLoggedIn) MainActivity::class.java else AuthActivity::class.java

        startActivity(Intent(this, targetClass))

        // Professional fade transition
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        finish()
    }
}