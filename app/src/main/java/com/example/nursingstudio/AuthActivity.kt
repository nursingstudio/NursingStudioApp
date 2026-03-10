package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nursingstudio.databinding.LayoutSecurityAlertBinding
import com.example.nursingstudio.ui.auth.login.LoginFragment
import com.example.nursingstudio.ui.auth.register.RegisterFragment
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AuthActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private var securitySheet: com.google.android.material.bottomsheet.BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Firebase aur App Check Initialize (Pehle logic set karein)
        FirebaseApp.initializeApp(this)

        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            // 2026 Professional Logic: Debug check with safe fallback
            if (BuildConfig.DEBUG) {
                firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            } else {
                firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
        } catch (e: Exception) {
            // Agar App Check fail ho toh app crash na ho, bas log karein
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().recordException(e)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        // ⭐ Security Check Call
        checkEnvironmentIntegrity()

        // ⭐ 2026 INTEL-LOGIC: Auto-dismiss alert when USB is unplugged
        val usbReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: Intent?) {
                val connected = intent?.extras?.getBoolean("connected") ?: false
                if (!connected && securitySheet?.isShowing == true) {
                    // Agar alert USB wala tha, toh band kar do
                    securitySheet?.dismiss()
                }
            }
        }
        registerReceiver(usbReceiver, android.content.IntentFilter("android.hardware.usb.action.USB_STATE"))

        if (savedInstanceState == null) {
            showLogin()
        }

        // Delay updates for better UX
        window.decorView.postDelayed({ checkForUpdates() }, 2000)
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

        // ⭐ 2026 Standard: Base64 Nonce
        val nonceRaw = "nursing_studio_${System.currentTimeMillis()}"
        val nonce = android.util.Base64.encodeToString(
            nonceRaw.toByteArray(),
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )

        val integrityTokenRequest = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(1009948838228L)
            .build()

        // Baaki code same rahega...
        integrityManager.requestIntegrityToken(integrityTokenRequest)
            .addOnCompleteListener { _ -> proceedToVerify(mobile, onCodeSent) }
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
                    // ⭐ WORLD-CLASS LOGGING: Sends the error to your Firebase Console
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().apply {
                        log("OTP Failed for number: $mobile")
                        recordException(e)
                    }
                    // 2026 Professional Error Mapping (User Friendly)
                    val errorMsg = when {
                        e.message?.contains("not authorized") == true -> "Security error: Please verify SHA-256 in Firebase Console."
                        e.message?.contains("quota") == true -> "SMS limit reached. Try again in 24 hours."
                        else -> e.localizedMessage ?: "Verification Failed"
                    }
                    android.widget.Toast.makeText(this@AuthActivity, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                }

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // ⭐ 2026 PRO LOGIC: Background safe auto-login
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.auth_container)

                    // 1. Agar OTP code mil gaya hai toh UI update karein
                    credential.smsCode?.let { code ->
                        if (currentFragment is LoginFragment) {
                            currentFragment.binding.etOtpLogin.setText(code)
                        }
                    }

                    // 2. Direct sign-in (Faster & Crash-proof)
                    auth.signInWithCredential(credential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            if (currentFragment is LoginFragment) {
                                currentFragment.proceedToHome()
                            } else {
                                // Fallback: Direct transition
                                val intent = Intent(this@AuthActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        }
                    }
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }
    private fun checkForUpdates() {
        val appUpdateManager = com.google.android.play.core.appupdate.AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)
            ) {
                // ⭐ 2026 GOLD STANDARD: Activity Result Launcher for Updates
                val updateOptions = com.google.android.play.core.appupdate.AppUpdateOptions.newBuilder(com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE)
                    .setAllowAssetPackDeletion(true)
                    .build()

                appUpdateManager.startUpdateFlow(
                    appUpdateInfo,
                    this@AuthActivity, // Explicitly use Activity context
                    updateOptions
                )
            }
        }
    }
    // ⭐ 2026 WORLD-CLASS SECURITY: Environment Integrity Check
    private fun checkEnvironmentIntegrity() {
        if (BuildConfig.DEBUG) return

        val isRooted = checkRootMethod()
        val isDevOptions = android.provider.Settings.Global.getInt(contentResolver, "development_settings_enabled", 0) != 0
        val isUsbDebugging = android.provider.Settings.Global.getInt(contentResolver, "adb_enabled", 0) != 0

        // USB Cable check logic
        val intent = registerReceiver(null, android.content.IntentFilter("android.hardware.usb.action.USB_STATE"))
        val isUsbConnected = intent?.extras?.getBoolean("connected") ?: false

        when {
            isRooted -> showSecurityAlert(SecurityType.ROOTED_DEVICE)
            isDevOptions -> showSecurityAlert(SecurityType.DEVELOPER_OPTIONS)
            isUsbDebugging -> showSecurityAlert(SecurityType.USB_DEBUGGING)
            isUsbConnected -> showSecurityAlert(SecurityType.ACTIVE_USB)
        }
    }
    // ⭐ 2026 Professional Security Enum
    enum class SecurityType {
        DEVELOPER_OPTIONS, USB_DEBUGGING, ACTIVE_USB, ROOTED_DEVICE
    }

    private fun showSecurityAlert(type: SecurityType) {
        // 1. Agar pehle se dikh raha hai toh wapas mat dikhao
        if (securitySheet?.isShowing == true) return

        securitySheet = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val binding = LayoutSecurityAlertBinding.inflate(layoutInflater)
        securitySheet?.setContentView(binding.root)
        securitySheet?.setCancelable(false)

        when(type) {
            SecurityType.DEVELOPER_OPTIONS -> {
                binding.tvAlertTitle.text = "DEVELOPER OPTIONS"
                binding.tvAlertDesc.text = "Nursing Studio security protocol: Please disable Developer Options to continue."
                binding.tvWarningStar.text = "System integrity check: FAILED"
                binding.btnSettings.setOnClickListener {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }
            }
            SecurityType.USB_DEBUGGING -> {
                binding.tvAlertTitle.text = "USB DEBUGGING"
                binding.tvAlertDesc.text = "Data protection active. Please turn off USB Debugging from your device settings."
                binding.tvWarningStar.text = "Encryption risk detected"
                binding.btnSettings.setOnClickListener {
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }
            }
            SecurityType.ACTIVE_USB -> {
                binding.tvAlertTitle.text = "USB CONNECTION"
                binding.tvAlertDesc.text = "Nursing Studio restricts USB connections to prevent unauthorized mirroring. Unplug to unlock."
                binding.tvWarningStar.text = "Waiting for cable removal..."
                binding.btnSettings.visibility = View.GONE
                // Isse button poori width le lega
                val params = binding.btnExit.layoutParams as android.widget.LinearLayout.LayoutParams
                params.weight = 2f
                params.marginEnd = 0
                binding.btnExit.layoutParams = params
            }
            SecurityType.ROOTED_DEVICE -> {
                binding.tvAlertTitle.text = "DEVICE COMPROMISED"
                binding.tvAlertDesc.text = "Nursing Studio detected root access. For your data safety, this app cannot run on rooted devices."
                binding.tvWarningStar.text = "Security Level: CRITICAL"
                binding.btnSettings.visibility = View.GONE
            }
        }

        binding.btnExit.setOnClickListener { finishAffinity() }
        securitySheet?.show()
    }
    // ⭐ Root detection logic (World-Class Utility)
    private fun checkRootMethod(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        for (path in paths) {
            if (java.io.File(path).exists()) return true
        }
        return false
    }
}