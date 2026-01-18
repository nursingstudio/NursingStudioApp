package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nursingstudio.auth.login.LoginFragment
import com.example.nursingstudio.auth.register.RegisterFragment
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AuthActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Firebase App Check (Wahi rakha hai jo tune diya tha)
        FirebaseApp.initializeApp(this)
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            firebaseAppCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            firebaseAppCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
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

        // World-class smooth transition (Fade in/out)
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        transaction.replace(R.id.auth_container, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null) // Taki register se back dabane par login pe aaye
        }

        transaction.commit()
    }

    // --- ⚡ OTP SENDER (Fragment se call hoga) ⚡ ---
    fun sendOtp(mobile: String, onCodeSent: (String) -> Unit) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber("+91$mobile")
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    onCodeSent(id)
                }
                override fun onVerificationFailed(e: FirebaseException) {
                    // Yahan aap toast dikha sakte hain (Error handling)
                }

                // --- YE WALA MISSING HAI, ISE ADD KARO ---
                override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                    // Auto-verification ke liye hota hai, abhi khali chhod do
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}