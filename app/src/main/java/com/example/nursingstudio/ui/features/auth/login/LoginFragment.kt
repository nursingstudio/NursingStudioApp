package com.example.nursingstudio.ui.features.auth.login

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.load
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.databinding.FragmentLoginBinding
import com.example.nursingstudio.ui.features.auth.AuthActivity
import com.example.nursingstudio.ui.features.main.MainActivity
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.utils.BiometricAuthHelper
import com.example.nursingstudio.utils.BiometricSettingsManager
import com.example.nursingstudio.workers.DataSyncWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

@AndroidEntryPoint
class LoginFragment : Fragment() {

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_TIME_HOURS = 6
        private const val SYNC_WORK_NAME = "NursingStudioPeriodicSync"
    }

    private var _binding: FragmentLoginBinding? = null
    internal val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    private lateinit var dataStoreManager: DataStoreManager
    private lateinit var bioSettingsManager: BiometricSettingsManager
    private lateinit var biometricHelper: BiometricAuthHelper

    private var isAdaptiveUiOverridden = false
    private var securityLockTickerJob: Job? = null

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            toast(getString(R.string.enable_notifications_for_alerts))
        }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            delay(200.milliseconds)
            if (_binding != null) {
                binding.etEmail.clearFocus()
                binding.etPassword.clearFocus()
                hideKeyboard()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager(requireContext())
        bioSettingsManager = BiometricSettingsManager(requireContext())
        biometricHelper = BiometricAuthHelper(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialUI()
        checkAdaptiveSessionState()
        setupClickListeners()
        setupTextWatchers()
        observeViewModel()
        startPromotionalAdsRotationEngine()
        schedulePeriodicBackgroundSync()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun setupInitialUI() {
        binding.tvAppTagline.alpha = 0f
        binding.tvAppTagline.animate().alpha(0.85f).setDuration(1000).start()
        AppSettings.setPushEffect(binding.btnLoginAction)

        AppSettings.setPushEffect(binding.btnMpinBoxTrigger)
        AppSettings.setPushEffect(binding.btnBiometricBoxTrigger)
    }

    private fun checkAdaptiveSessionState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (isAdaptiveUiOverridden) return@repeatOnLifecycle
                if (isAnyBottomSheetVisible()) return@repeatOnLifecycle

                val isMpinConfigured = dataStoreManager.isMpinSet.firstOrNull() ?: false
                val cachedName = dataStoreManager.userName.firstOrNull() ?: "User"

                if (isMpinConfigured) {
                    binding.layoutDefaultForm.visibility = View.GONE
                    binding.layoutAdaptiveMpinForm.visibility = View.VISIBLE
                    binding.tvWelcomeUser.text = getString(R.string.welcome_back, cachedName)

                    val isBiometricActive = bioSettingsManager.isBiometricAuthActive() &&
                            biometricHelper.checkBiometricAvailability() == BiometricManager.BIOMETRIC_SUCCESS

                    binding.btnBiometricBoxTrigger.isVisible = isBiometricActive

                    // 🚀 FIXED: Safe ConstraintLayout handling preventing 'layoutSecureMethodsContainer' unresolved crash
                    val constraintLayout = binding.btnMpinBoxTrigger.parent as? ConstraintLayout
                    constraintLayout?.let { container ->
                        val constraintSet = ConstraintSet()
                        constraintSet.clone(container)

                        if (isBiometricActive) {
                            constraintSet.connect(
                                R.id.btnMpinBoxTrigger,
                                ConstraintSet.END,
                                R.id.btnBiometricBoxTrigger,
                                ConstraintSet.START
                            )
                        } else {
                            constraintSet.connect(
                                R.id.btnMpinBoxTrigger,
                                ConstraintSet.END,
                                ConstraintSet.PARENT_ID,
                                ConstraintSet.END
                            )
                        }
                        constraintSet.applyTo(container)
                    }

                    binding.tvMpinSubtitlePrompt.text = if (isBiometricActive) {
                        getString(R.string.choose_your_secure_method_below_to_proceed)
                    } else {
                        getString(R.string.use_secure_4_digit_mpin_to_proceed)
                    }

                } else {
                    binding.layoutDefaultForm.visibility = View.VISIBLE
                    binding.layoutAdaptiveMpinForm.visibility = View.GONE
                }
            }
        }
    }

    private fun isAnyBottomSheetVisible(): Boolean {
        val forgotSheet = childFragmentManager.findFragmentByTag("ForgotPasswordSheet")
        val mpinSheet = childFragmentManager.findFragmentByTag("MpinSheet")
        val forgotMpin = childFragmentManager.findFragmentByTag("ForgotMpinBottomSheet")

        return (forgotSheet != null && forgotSheet.isAdded) ||
                (mpinSheet != null && mpinSheet.isAdded) ||
                (forgotMpin != null && forgotMpin.isAdded)
    }

    private fun isUserLocked(email: String): Boolean {
        if (email.isEmpty()) return false
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val lockUntil = prefs.getLong("lock_timestamp_$email", 0)
        return System.currentTimeMillis() < lockUntil
    }

    private fun getRemainingLockTime(email: String): String {
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val diff = prefs.getLong("lock_timestamp_$email", 0) - System.currentTimeMillis()
        if (diff <= 0) return "00:00:00"
        val hours = (diff / (1000 * 60 * 60)) % 24
        val minutes = (diff / (1000 * 60)) % 60
        val seconds = (diff / 1000) % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun recordFailedAttempt(email: String) {
        val prefs = requireContext().getSharedPreferences("login_lock", Context.MODE_PRIVATE)
        val attempts = prefs.getInt("attempts_$email", 0) + 1

        if (attempts >= MAX_ATTEMPTS) {
            val lockUntil = System.currentTimeMillis() + (LOCK_TIME_HOURS * 60 * 60 * 1000)
            prefs.edit {
                putLong("lock_timestamp_$email", lockUntil)
                putInt("attempts_$email", 0)
            }
        } else {
            prefs.edit { putInt("attempts_$email", attempts) }
        }
    }

    private fun checkAndHandleLock(email: String): Boolean {
        val isLocked = isUserLocked(email)
        if (isLocked) {
            val timeLeft = getRemainingLockTime(email)
            binding.tilEmail.isErrorEnabled = true
            binding.tilEmail.error = "Account locked! Try after $timeLeft"

            securityLockTickerJob?.cancel()
            securityLockTickerJob = viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isUserLocked(email)) {
                        val currentTimeLeft = getRemainingLockTime(email)
                        binding.tilEmail.error = "Account locked! Try after $currentTimeLeft"
                        delay(1000.milliseconds)
                    }
                    binding.tilEmail.isErrorEnabled = false
                    binding.tilEmail.error = null
                }
            }
        } else {
            binding.tilEmail.isErrorEnabled = false
            binding.tilEmail.error = null
        }
        return isLocked
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    binding.loadingOverlay.visibility = if (state is LoginViewModel.LoginState.Loading) View.VISIBLE else View.GONE

                    when (state) {
                        is LoginViewModel.LoginState.Idle, is LoginViewModel.LoginState.Loading -> {}
                        is LoginViewModel.LoginState.Success -> {
                            val inputtedEmail = binding.etEmail.text.toString().trim()
                            val inputtedPassword = binding.etPassword.text.toString().trim()

                            if (inputtedEmail.isNotEmpty() && inputtedPassword.isNotEmpty() && binding.layoutDefaultForm.isVisible) {
                                bioSettingsManager.saveCredentials(inputtedEmail, inputtedPassword)
                            }

                            AppSettings.startNewUserSession(requireContext())
                            proceedToHome()
                        }
                        is LoginViewModel.LoginState.NoProfile -> {
                            toast(getString(R.string.no_profile_found))
                            binding.root.postDelayed({
                                if (isAdded) (activity as? AuthActivity)?.showRegister()
                            }, 1000)
                        }
                        is LoginViewModel.LoginState.Error -> handleLoginError(state.message)
                    }
                }
            }
        }
    }

    private fun handleLoginError(msg: String) {
        val email = binding.etEmail.text.toString().trim()
        val userFriendlyMsg = when {
            msg.contains("network", true) || msg.contains("timeout", true) -> getString(R.string.no_internet)
            msg.contains("password", true) || msg.contains("credential", true) -> "Incorrect Credentials!🔑"
            else -> msg
        }

        if (msg.contains("password", true) || msg.contains("credential", true)) {
            if (email.isNotEmpty()) recordFailedAttempt(email)
            binding.tilPassword.error = userFriendlyMsg
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
        } else {
            toast(userFriendlyMsg)
            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
        }
    }

    private fun setupTextWatchers() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilEmail.isErrorEnabled = false
                binding.tilEmail.error = null
                checkAndHandleLock(s.toString().trim())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tilPassword.error = null
                binding.tilPassword.isErrorEnabled = false
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnLoginAction.setOnClickListener {
            hideKeyboard()
            performEmailLogin()
        }

        binding.tvGoRegister.setOnClickListener {
            (activity as? AuthActivity)?.showRegister()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordSheet()
        }

        binding.tvDifferentUser.setOnClickListener {
            isAdaptiveUiOverridden = true
            binding.layoutAdaptiveMpinForm.visibility = View.GONE
            binding.layoutDefaultForm.visibility = View.VISIBLE
        }

        binding.tvForgotMpinAction.setOnClickListener {
            val currentInputtedPass = binding.etPassword.text.toString().trim()
            val forgotMpinSheet = ForgotMpinBottomSheet(
                runtimePasswordFallback = currentInputtedPass,
                onResetVerified = {
                    showLocalMpinSetupDialog()
                }
            )
            forgotMpinSheet.show(childFragmentManager, "ForgotMpinBottomSheet")
        }

        binding.btnMpinBoxTrigger.setOnClickListener {
            AppSettings.triggerVibration(requireContext(), 25)
            showMpinBottomSheet()
        }

        binding.btnBiometricBoxTrigger.setOnClickListener {
            AppSettings.triggerVibration(requireContext(), 25)
            showBiometricPrompt()
        }
    }

    /**
     * 🚀 2026 INDUSTRY GOLD STANDARD: Safe Lifecycle-Aware Coil Image Loading & Banner Rotation.
     */
    private fun startPromotionalAdsRotationEngine() {
        binding.adViewFlipper.setInAnimation(requireContext(), android.R.anim.slide_in_left)
        binding.adViewFlipper.setOutAnimation(requireContext(), android.R.anim.slide_out_right)

        // Safe Direct Access via ViewBinding & Efficient Coil Image Loading
        binding.imgYtBanner.load(R.drawable.yt_snap) {
            crossfade(true)
        }
        binding.imgWaBanner.load(R.drawable.wa_snap) {
            crossfade(true)
        }

        binding.layoutYoutubeAdClick.setOnClickListener {
            AppSettings.triggerVibration(requireContext(), 20)
            val intent = Intent(
                Intent.ACTION_VIEW,
                getString(R.string.yt_channel_handle).toUri()
            )
            startActivity(intent)
        }

        binding.layoutWhatsappAdClick.setOnClickListener {
            AppSettings.triggerVibration(requireContext(), 20)
            val intent = Intent(
                Intent.ACTION_VIEW,
                getString(R.string.wa_channel_handle).toUri()
            )
            startActivity(intent)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    delay(3000.milliseconds)
                    if (_binding != null) {
                        binding.adViewFlipper.showNext()
                    }
                }
            }
        }
    }

    /**
     * 🚀 2026 GOLD STANDARD: Schedule WorkManager for background battery-efficient tasks.
     */
    private fun schedulePeriodicBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun showLocalMpinSetupDialog() {
        val containerFrame = FrameLayout(requireContext()).apply {
            val paddingHorizontal = (24 * resources.displayMetrics.density).toInt()
            val paddingTop = (12 * resources.displayMetrics.density).toInt()
            setPadding(paddingHorizontal, paddingTop, paddingHorizontal, 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val styledContext = ContextThemeWrapper(
            requireContext(),
            R.style.Widget_Material3_TextInputLayout_OutlinedBox_CustomGlobal
        )

        val textInputLayout = TextInputLayout(
            styledContext,
            null,
            com.google.android.material.R.attr.textInputStyle
        ).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }

        val etMpin = TextInputEditText(textInputLayout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            transformationMethod = PasswordTransformationMethod.getInstance()
            filters = arrayOf(InputFilter.LengthFilter(4))
            hint = getString(R.string.enter_new_mpin)
            textSize = 16f
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        etMpin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textInputLayout.error = null
                textInputLayout.isErrorEnabled = false
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        textInputLayout.addView(etMpin)
        containerFrame.addView(textInputLayout)

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setTitle(getString(R.string.reset_secure_mpin))
            .setMessage(getString(R.string.mpin_reset_dialog_msg))
            .setView(containerFrame)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.save_mpin), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()

        val metricsWidth = (resources.displayMetrics.widthPixels * 0.92).toInt()
        dialog.window?.setLayout(metricsWidth, ViewGroup.LayoutParams.WRAP_CONTENT)

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val mpin = etMpin.text.toString().trim()
            val previouslySavedMpin = bioSettingsManager.getMPIN()

            when {
                mpin.length != 4 -> {
                    textInputLayout.isErrorEnabled = true
                    textInputLayout.error = getString(R.string.mpin_requires_exactly_4_digits)
                    AppSettings.triggerErrorEffect(requireContext(), textInputLayout)
                }
                previouslySavedMpin != null && mpin == previouslySavedMpin -> {
                    textInputLayout.isErrorEnabled = true
                    textInputLayout.error = getString(R.string.error_duplicate_mpin)
                    AppSettings.triggerErrorEffect(requireContext(), textInputLayout)
                }
                else -> {
                    textInputLayout.error = null
                    viewLifecycleOwner.lifecycleScope.launch {
                        bioSettingsManager.saveMPIN(mpin)
                        dataStoreManager.saveMpinStatus(true)
                        toast(getString(R.string.secure_mpin_updated_successfully))
                        dialog.dismiss()
                        checkAdaptiveSessionState()
                    }
                }
            }
        }
    }

    private fun performEmailLogin() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (!isNetworkAvailable()) {
            toast(getString(R.string.no_internet))
            AppSettings.triggerErrorEffect(requireContext(), binding.btnLoginAction)
            return
        }

        if (isUserLocked(email)) {
            binding.tilPassword.isErrorEnabled = true
            binding.tilPassword.error = "Account locked! Try after ${getRemainingLockTime(email)}"
            AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
            return
        }

        clearAllErrors()

        when {
            email.isEmpty() -> {
                binding.tilEmail.error = getString(R.string.error_email_empty)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
                return
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = getString(R.string.error_email_invalid)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
                return
            }
        }

        when {
            pass.isEmpty() -> {
                binding.tilPassword.error = getString(R.string.error_password_empty)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                return
            }
            pass.length < 8 -> {
                binding.tilPassword.error = getString(R.string.error_password_length)
                AppSettings.triggerErrorEffect(requireContext(), binding.tilPassword)
                return
            }
        }
        viewModel.loginWithEmail(email, pass)
    }

    internal fun proceedToHome() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.withResumed {
                if (isAdded && activity != null) {
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    activity?.finish()
                }
            }
        }
    }

    private fun toast(m: String) = Toast.makeText(context, m, Toast.LENGTH_SHORT).show()

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view?.windowToken?.let { imm.hideSoftInputFromWindow(it, 0) }
    }

    private fun clearAllErrors() {
        binding.tilEmail.error = null
        binding.tilEmail.isErrorEnabled = false
        binding.tilPassword.error = null
        binding.tilPassword.isErrorEnabled = false
    }

    private fun showForgotPasswordSheet() {
        if (isAnyBottomSheetVisible()) return
        val bottomSheet = ForgotPasswordBottomSheet()
        bottomSheet.show(childFragmentManager, "ForgotPasswordSheet")
    }

    private fun showBiometricPrompt() {
        biometricHelper.triggerAuthentication(
            title = getString(R.string.secure_biometrics_login),
            subtitle = getString(R.string.scan_fingerprint_for_secure_login),
            onSuccess = { _ ->
                val savedEmail = bioSettingsManager.getSavedEmail()
                val savedPassword = bioSettingsManager.getSavedPass()

                if (!savedEmail.isNullOrEmpty() && savedPassword.isNotEmpty()) {
                    viewModel.loginWithEmail(savedEmail, savedPassword)
                } else {
                    proceedToHome()
                }
            },
            onError = { _ ->
                showMpinBottomSheet()
            }
        )
    }

    private fun showMpinBottomSheet() {
        if (isAnyBottomSheetVisible()) return
        val mpinSheet = MpinBottomSheet(
            onMpinSuccess = { email, pass ->
                if (!email.isNullOrEmpty() && !pass.isNullOrEmpty()) {
                    viewModel.loginWithEmail(email, pass)
                } else {
                    proceedToHome()
                }
            },
            onBiometricRequest = {
                showBiometricPrompt()
            },
            onForgotMpinRequested = {
                val currentInputtedPass = binding.etPassword.text.toString().trim()
                val forgotSheet = ForgotMpinBottomSheet(
                    runtimePasswordFallback = currentInputtedPass,
                    onResetVerified = {
                        showLocalMpinSetupDialog()
                    }
                )
                forgotSheet.show(childFragmentManager, "ForgotMpinBottomSheet")
            }
        )
        mpinSheet.show(childFragmentManager, "MpinSheet")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        securityLockTickerJob?.cancel()
        viewModel.resetState()
        _binding = null
        super.onDestroyView()
    }
}