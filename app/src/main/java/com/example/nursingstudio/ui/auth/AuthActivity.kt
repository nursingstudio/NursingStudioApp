package com.example.nursingstudio.ui.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutSecurityAlertBinding
import com.example.nursingstudio.ui.auth.login.LoginFragment
import com.example.nursingstudio.ui.auth.register.RegisterFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint // ✅ Hilt Multi-Thread Injection Engine Allowed
class AuthActivity : AppCompatActivity() {

    private var securitySheet: BottomSheetDialog? = null
    private lateinit var usbReceiver: BroadcastReceiver

    // ⭐ 2026 GOLD STANDARD: Real-time Database Engine Observers for System Settings
    private var devOptionsObserver: ContentObserver? = null
    private var adbObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // ⭐ 2026 GOLD STANDARD: Double-Layer Guard to Silences Kotlin Linter Warning on Build Variants
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        @Suppress("KotlinConstantConditions", "ConstantConditions")
        val isDebugEnvironment = com.example.nursingstudio.BuildConfig.DEBUG

        @Suppress("KotlinConstantConditions", "ConstantConditions")
        if (isDebugEnvironment) {
            // Debug mode: Loaded safely when variants switched to debug config
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // Release mode: Signed dynamic security check for Google Play Store
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        setContentView(R.layout.activity_auth)

        // Architectural isolation of standard verification engine
        window.decorView.postDelayed({ checkEnvironmentIntegrity() }, 500)

        // ⭐ 2026 INTEL-LOGIC: Auto-dismiss system hardware structural listener
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val connected = intent?.extras?.getBoolean("connected") ?: false
                if (!isFinishing && !isDestroyed) {
                    // Real-time immediate validation when state changes
                    checkEnvironmentIntegrity()
                    if (!connected && securitySheet?.isShowing == true) {
                        securitySheet?.dismiss()
                    }
                }
            }
        }

        // Android 14+ Explicit Context Broadcast Verification Standard Compliance
        val usbFilter = IntentFilter().apply {
            addAction("android.hardware.usb.action.USB_STATE")
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, usbFilter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(usbReceiver, usbFilter)
        }

        // ⭐ SETUP REAL-TIME SYSTEM REGISTRY OBSERVERS
        setupSystemSettingsObservers()

        if (savedInstanceState == null) {
            showLogin()
        }

        window.decorView.postDelayed({ checkForUpdates() }, 2000)
    }

    override fun onResume() {
        super.onResume()
        checkEnvironmentIntegrity()
    }

    // ⭐ 2026 SECURE RUNTIME PIPELINE: Continuous Tracking Architecture
    private fun setupSystemSettingsObservers() {
        val mainHandler = Handler(Looper.getMainLooper())

        devOptionsObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkEnvironmentIntegrity()
            }
        }

        adbObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkEnvironmentIntegrity()
            }
        }

        // Register secure listener nodes deep inside Android ContentResolver
        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
            false,
            devOptionsObserver!!
        )
        contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ADB_ENABLED),
            false,
            adbObserver!!
        )
    }

    fun showLogin() {
        replaceFragment(LoginFragment(), false)
    }

    fun showRegister() {
        replaceFragment(RegisterFragment(), true)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager.beginTransaction()

        // ⭐ PREMIUM ACCELERATED SEAMLESS INTERPOLATION CORE ANIMATIONS
        transaction.setCustomAnimations(
            R.anim.slide_in_right,
            R.anim.slide_out_left,
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )

        transaction.replace(R.id.auth_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()
    }

    private fun checkForUpdates() {
        val appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                val updateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE)
                    .setAllowAssetPackDeletion(true)
                    .build()

                appUpdateManager.startUpdateFlow(
                    appUpdateInfo,
                    this@AuthActivity,
                    updateOptions
                )
            }
        }
    }

    // ⭐ 2026 SYSTEM LAYER PROTECTION: Deep Structural Validation Checks
    private fun checkEnvironmentIntegrity() {
        val isDevOptions = try {
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        } catch (_: Exception) {
            false
        }
        val isUsbDebugging = try {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
        } catch (_: Exception) {
            false
        }

        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isUsbConnected = (batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1)
            ?: -1) == android.os.BatteryManager.BATTERY_PLUGGED_USB
        val isRooted = checkRootMethod()

        var activeThreat: SecurityType? = null
        when {
            isRooted -> activeThreat = SecurityType.ROOTED_DEVICE
            isUsbDebugging -> activeThreat = SecurityType.USB_DEBUGGING
            isDevOptions -> activeThreat = SecurityType.DEVELOPER_OPTIONS
            isUsbConnected -> activeThreat = SecurityType.ACTIVE_USB
        }

        // Synchronous execution path safety mapping
        if (activeThreat != null) {
            showSecurityAlert(activeThreat)
        } else {
            securitySheet?.takeIf { it.isShowing }?.dismiss()
        }
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

    enum class SecurityType {
        DEVELOPER_OPTIONS, USB_DEBUGGING, ACTIVE_USB, ROOTED_DEVICE
    }

    private fun showSecurityAlert(type: SecurityType) {
        if (securitySheet?.isShowing == true) {
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

    private fun checkRootMethod(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        for (binary in arrayOf("su")) {
            val targets = arrayOf("/system/xbin/", "/system/bin/", "/vendor/bin/", "/sbin/")
            for (target in targets) {
                if (File(target + binary).exists()) return true
            }
        }
        return false
    }

    override fun onDestroy() {
        try {
            devOptionsObserver?.let { contentResolver.unregisterContentObserver(it) }
            adbObserver?.let { contentResolver.unregisterContentObserver(it) }
            unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}