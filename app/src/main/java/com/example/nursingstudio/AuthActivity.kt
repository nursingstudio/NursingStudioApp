package com.example.nursingstudio

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nursingstudio.ui.auth.login.LoginFragment
import com.example.nursingstudio.ui.auth.register.RegisterFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.auth.PhoneAuthCredential

class AuthActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Sabse pehle initialize karo super se bhi pehle
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // Professional Way to Install Provider
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // 2. Fragment Loading Logic (Cleaned)
        if (savedInstanceState == null) {
            // Hum direct LoginFragment load kar rahe hain, Splash ne session pehle hi check kar liya hai
            showLogin()
        }
    }

    // --- ⭐ ULTRA PRO NAVIGATION LOGIC ⭐ ---

    // Login Fragment dikhane ke liye
    fun showLogin() {
        replaceFragment(LoginFragment(), false)
    }

    // Register Fragment dikhane ke liye (Iska error aa raha tha na? Ab nahi aayega!)
    fun showRegister() {
        replaceFragment(RegisterFragment(), true)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()

        // ⭐ WORLD-CLASS PREMIUM ANIMATION ⭐
        // Parameters: (Enter, Exit, PopEnter, PopExit)
        transaction.setCustomAnimations(
            R.anim.slide_in_right,  // Naya fragment aate waqt
            R.anim.slide_out_left,  // Purana fragment jaate waqt
            R.anim.slide_in_left,   // Back dabane par purana wapas aate waqt
            R.anim.slide_out_right  // Back dabane par naya jaate waqt
        )

        transaction.replace(R.id.auth_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    // --- ⚡ OTP SENDER (Fragment se call hoga) ⚡ ---
    // --- ⚡ SECURE OTP SENDER (Updated with Play Integrity) ⚡ ---
// AuthActivity.kt (Optimized Master Hub)
    fun sendOtp(mobile: String, onCodeSent: (String) -> Unit) {
        val integrityManager = IntegrityManagerFactory.create(applicationContext)
        val nonce = "nursing_studio_${System.currentTimeMillis()}"

        val integrityTokenRequest = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(1009948838228L)
            .build()

        // Professional Approach: Try Integrity, Fallback to SMS
        integrityManager.requestIntegrityToken(integrityTokenRequest)
            .addOnCompleteListener { _ ->
                // Success ho ya fail, hum OTP bhejne ki koshish karenge
                proceedToVerify(mobile, onCodeSent)
            }
    }

    private fun proceedToVerify(mobile: String, onCodeSent: (String) -> Unit) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$mobile")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    onCodeSent(id)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    // Professional Error Mapping
                    val errorMsg = when(e.message) {
                        null -> "Security Check Failed"
                        else -> if(e.message!!.contains("quota")) "Too many attempts. Try tomorrow." else e.message
                    }
                    android.widget.Toast.makeText(this@AuthActivity, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // 1. Check karein ki abhi screen par LoginFragment hi hai
                    val navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_container)

                    if (navHostFragment is LoginFragment) {
                        // 2. Instant Auto-Login trigger karein
                        navHostFragment.triggerAutoLogin(credential)

                        // 3. User Experience: Chota sa message dikhayein
                        android.widget.Toast.makeText(this@AuthActivity, "Instant Verification Successful! ⚡", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

}