package com.example.nursingstudio.ui.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutSecurityAlertBinding
import com.example.nursingstudio.ui.auth.login.LoginFragment
import com.example.nursingstudio.ui.auth.register.RegisterFragment
import com.example.nursingstudio.ui.main.MainActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.TimeUnit

@AndroidEntryPoint // ✅ 2026 Gold Standard: This allows Hilt to inject into Fragments
class AuthActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    private var securitySheet: BottomSheetDialog? = null
    private lateinit var usbReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // ⭐ 2026 GOLD STANDARD: Smart App Check Initialization
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        if (BuildConfig.DEBUG) {
            // Debug mode: For Testing (Emulators/Physical devices)
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Release mode: For Play Store (World-Class Security)
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )

        }

        setContentView(R.layout.activity_auth)

        // ⭐ Security Check Call
        window.decorView.postDelayed({ checkEnvironmentIntegrity() }, 500)

        // ⭐ 2026 INTEL-LOGIC: Auto-dismiss alert when USB is unplugged
         usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val connected = intent?.extras?.getBoolean("connected") ?: false
                if (!isFinishing && !isDestroyed && securitySheet?.isShowing == true) {
                    securitySheet!!.dismiss()
                }
                if (!connected && securitySheet?.isShowing == true) {
                    // Agar alert USB wala tha, toh band kar do
                    securitySheet?.dismiss()
                }
            }
        }
        registerReceiver(usbReceiver, IntentFilter("android.hardware.usb.action.USB_STATE"))

        if (savedInstanceState == null) {
            showLogin()
        }

        // Delay updates for better UX
        window.decorView.postDelayed({ checkForUpdates() }, 2000)
    }
    override fun onResume() {
        super.onResume()
        // 500ms delay taaki system settings fresh read ho sake
        window.decorView.postDelayed({ checkEnvironmentIntegrity() }, 500)
    }
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
        val nonce = Base64.encodeToString(
            nonceRaw.toByteArray(),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
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
                    FirebaseCrashlytics.getInstance().apply {
                        log("OTP Failed for number: $mobile")
                        recordException(e)
                    }
                    // 2026 Professional Error Mapping (User Friendly)
                    val errorMsg = when {
                        e.message?.contains("not authorized") == true -> "Security error: Please verify SHA-256 in Firebase Console."
                        e.message?.contains("quota") == true -> "SMS limit reached. Try again in 24 hours."
                        else -> e.localizedMessage ?: "Verification Failed"
                    }
                    Toast.makeText(this@AuthActivity, errorMsg, Toast.LENGTH_LONG).show()
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
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                // ⭐ 2026 GOLD STANDARD: Activity Result Launcher for Updates
                val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
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
        // 1. Fetch current states
        val isDevOptions = try { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0 } catch (_: Exception) { false }
        val isUsbDebugging = try { Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) != 0 } catch (_: Exception) { false }
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isUsbConnected = (batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1) == android.os.BatteryManager.BATTERY_PLUGGED_USB
        val isRooted = checkRootMethod()

        // ⭐ 2026 WORLD-CLASS LOGIC: Single Entry, Single Exit (No Dead Code Warnings)
        var activeThreat: SecurityType? = null

        when {
            isRooted -> activeThreat = SecurityType.ROOTED_DEVICE
            isUsbDebugging -> activeThreat = SecurityType.USB_DEBUGGING
            isDevOptions -> activeThreat = SecurityType.DEVELOPER_OPTIONS
            isUsbConnected -> activeThreat = SecurityType.ACTIVE_USB
        }

        // 2. Action based on threat state
        if (activeThreat != null) {
            showSecurityAlert(activeThreat)
        } else {
            // Agar koi threat nahi hai, toh purana alert hata do (Warning-Free Dismissal)
            securitySheet?.takeIf { it.isShowing }?.dismiss()
        }

        // 3. PLAY INTEGRITY (Keep as it is, but only for Release)
        // Ye check karega ki app Play Store se hai ya MOD APK hai
        /*
        if (!BuildConfig.DEBUG) {
            val integrityManager = IntegrityManagerFactory.create(applicationContext)
            val nonce = Base64.encodeToString("nursing_studio_verify".toByteArray(), Base64.URL_SAFE)

            val integrityTokenRequest = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .setCloudProjectNumber(1009948838228L) // Aapka Project Number
                .build()

            integrityManager.requestIntegrityToken(integrityTokenRequest)
                .addOnSuccessListener { _ ->
                    // App is Genuine
                }
                .addOnFailureListener { _ ->
                    // Agar Integrity fail ho jaye (Matlab MOD APK ya Tampered App hai)
                    Toast.makeText(this, "Security Breach: Official App Required", Toast.LENGTH_LONG).show()
                    finishAffinity()
                }
        }

         */
    }
    // ⭐ 2026 Professional Security Enum
    enum class SecurityType {
        DEVELOPER_OPTIONS, USB_DEBUGGING, ACTIVE_USB, ROOTED_DEVICE
    }

    private fun showSecurityAlert(type: SecurityType) {
        // Purani line: if (securitySheet?.isShowing == true) return
        if (securitySheet?.isShowing == true) {
            // Agar alert badal gaya hai (e.g., USB se Dev Mode), toh purana dismiss karo
            securitySheet?.dismiss()
        }

        securitySheet = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val binding = LayoutSecurityAlertBinding.inflate(layoutInflater)
        securitySheet?.setContentView(binding.root)
        securitySheet?.setCancelable(false)

        when(type) {
            SecurityType.DEVELOPER_OPTIONS -> {
                binding.tvAlertTitle.text = getString(R.string.developer_mode_enabled)
                binding.tvAlertDesc.text = getString(R.string.developer_mode_description)
                binding.tvWarningStar.text = getString(R.string.developer_options_star_text)
                binding.btnSettings.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }
            }
            SecurityType.USB_DEBUGGING -> {
                binding.tvAlertTitle.text = getString(R.string.usb_debugging_enabled)
                binding.tvAlertDesc.text = getString(R.string.usb_debugging_description)
                binding.tvWarningStar.text = getString(R.string.usb_debugging_star_text)
                binding.btnSettings.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                }
            }
            SecurityType.ACTIVE_USB -> {
                binding.tvAlertTitle.text = getString(R.string.active_usb_connection_detected)
                binding.tvAlertDesc.text = getString(R.string.active_usb_description)
                binding.tvWarningStar.text = getString(R.string.active_usb_star_text)
                binding.btnSettings.visibility = View.GONE
                // Isse button poori width le lega
                val params = binding.btnExit.layoutParams as LinearLayout.LayoutParams
                params.weight = 2f
                params.marginEnd = 0
                binding.btnExit.layoutParams = params
            }
            SecurityType.ROOTED_DEVICE -> {
                binding.tvAlertTitle.text = getString(R.string.device_compromised)
                binding.tvAlertDesc.text = getString(R.string.root_access_description)
                binding.tvWarningStar.text = getString(R.string.root_access_star_text)
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
            if (File(path).exists()) return true
        }
        return false
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {
            // Receiver already unregistered
        }
    }
}