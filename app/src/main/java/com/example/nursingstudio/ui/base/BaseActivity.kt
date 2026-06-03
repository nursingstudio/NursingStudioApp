package com.example.nursingstudio.ui.base

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
import androidx.core.view.doOnAttach
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.LayoutSecurityAlertBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File

abstract class BaseActivity : AppCompatActivity() {

    private var securitySheet: BottomSheetDialog? = null
    private var currentThreatType: SecurityType? = null // ⭐ 2026 STATE TRACKER: Keeps account of active foreground threat type
    private var usbReceiver: BroadcastReceiver? = null
    private var devOptionsObserver: ContentObserver? = null
    private var adbObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupSystemSettingsObservers()
        setupUsbReceiver()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        window.decorView.doOnAttach {
            checkEnvironmentIntegrity()
        }
    }

    override fun onResume() {
        super.onResume()
        if (window.decorView.isAttachedToWindow) {
            checkEnvironmentIntegrity()
        }
    }

    private fun setupUsbReceiver() {
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // ⭐ SANITIZED: We perform deep scanning instead of relying on unreliable transient flags
                checkEnvironmentIntegrity()
            }
        }
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
    }

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

    private fun checkEnvironmentIntegrity() {
        if (isFinishing || isDestroyed) return

        val isDevOptions = try {
            Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0
        } catch (_: Exception) { false }

        val isUsbDebugging = try {
            Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
        } catch (_: Exception) { false }

        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val isUsbConnected = (batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1) == android.os.BatteryManager.BATTERY_PLUGGED_USB
        val isRooted = checkRootMethod()

        // ⭐ 2026 CRITICAL PRIORITY MATRIX: Evaluates exact ranking order of security threats
        var activeThreat: SecurityType? = null
        when {
            isRooted -> activeThreat = SecurityType.ROOTED_DEVICE
            isUsbDebugging -> activeThreat = SecurityType.USB_DEBUGGING
            isDevOptions -> activeThreat = SecurityType.DEVELOPER_OPTIONS
            isUsbConnected -> activeThreat = SecurityType.ACTIVE_USB
        }

        if (activeThreat != null) {
            showSecurityAlert(activeThreat)
        } else {
            // Clears any sheet cleanly if all threats are completely resolved
            securitySheet?.takeIf { it.isShowing }?.dismiss()
            securitySheet = null
            currentThreatType = null
        }
    }

    enum class SecurityType {
        DEVELOPER_OPTIONS, USB_DEBUGGING, ACTIVE_USB, ROOTED_DEVICE
    }

    private fun showSecurityAlert(type: SecurityType) {
        if (isFinishing || isDestroyed) return

        // ⭐ 2026 DYNAMIC REDRAW ENGINE: If sheet is already displaying the SAME threat, skip rendering
        if (securitySheet?.isShowing == true && currentThreatType == type) {
            return
        }

        // If threat state has changed, dismiss the existing stale sheet instantly to redraw the higher priority alert
        securitySheet?.dismiss()

        securitySheet = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val binding = LayoutSecurityAlertBinding.inflate(layoutInflater)
        securitySheet?.setContentView(binding.root)
        securitySheet?.setCancelable(false)
        currentThreatType = type // Syncing data variable with state matrix

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
        for (path in paths) { if (File(path).exists()) return true }
        return false
    }

    override fun onDestroy() {
        try {
            devOptionsObserver?.let { contentResolver.unregisterContentObserver(it) }
            adbObserver?.let { contentResolver.unregisterContentObserver(it) }
            usbReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) {}
        super.onDestroy()
    }
}