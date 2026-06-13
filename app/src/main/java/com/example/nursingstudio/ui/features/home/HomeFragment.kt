package com.example.nursingstudio.ui.features.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.databinding.FragmentHomeBinding
import com.example.nursingstudio.utils.ProgressManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    // 🚀 FIXED: Fully standardized tracking parameters to insulate window layouts during rotation updates
    private var biometricDialog: AlertDialog? = null

    // 🚀 FIXED: Migrated from legacy slow findViewById system down to clean pre-compiled ViewBinding structures
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataStoreManager = DataStoreManager(requireContext())

        setupCardsClickListeners()
        setupReactiveWelcomeHeader()
        checkAndShowBiometricPrompt()
        setupDailyMotivation()
    }

    private fun setupCardsClickListeners() {
        // 🚀 FIXED: Cleaner compilation references mapping inside safe pre-compiled binding layers
        binding.cardTest.setOnClickListener {
            ProgressManager.increment(requireContext(), "test_attempted")
            navigateToFragment(R.id.nav_quiz)
        }

        binding.cardPdf.setOnClickListener {
            ProgressManager.increment(requireContext(), "pdf_opened")
            navigateToFragment(R.id.nav_pdf)
        }

        binding.cardVideo.setOnClickListener {
            ProgressManager.increment(requireContext(), "video_watched")
            navigateToFragment(R.id.nav_video)
        }

        binding.cardProgress.setOnClickListener {
            navigateToFragment(R.id.nav_profile)
        }
    }

    private fun setupReactiveWelcomeHeader() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                dataStoreManager.userName.collect { name ->
                    // 🚀 FIXED: Guarded against null bindings variables transitions gracefully
                    _binding?.tvWelcome?.text = getString(R.string.welcome_user, name ?: "Scholar")
                }
            }
        }
    }

    private fun setupDailyMotivation() {
        // 🚀 Architectural Reactive Gate: Validate if user enabled the motivation component from settings first
        val settingsPref = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        val isMotivationEnabled = settingsPref.getBoolean("enable_motivation", true)

        if (!isMotivationEnabled) {
            // Safe fallback structure to smoothly hide components container from user layout view context
            binding.tvMotivation.visibility = View.GONE
            // If there's an outer card wrapper layout like cardMotivation, hide that component explicitly.
            return
        } else {
            binding.tvMotivation.visibility = View.VISIBLE
        }

        val quotes = listOf(
            "Consistency beats intensity.",
            "Small steps daily create big success.",
            "Today’s effort is tomorrow’s result.",
            "Study smart, not just hard.",
            "Discipline today, success tomorrow."
        )

        val sp = requireContext().getSharedPreferences("daily_motivation", Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = sp.getString("date", "")
        val savedQuote = sp.getString("quote", "")

        if (today == savedDate && !savedQuote.isNullOrEmpty()) {
            binding.tvMotivation.text = savedQuote
        } else {
            val newQuote = quotes.random()
            binding.tvMotivation.text = newQuote

            sp.edit(commit = false) {
                putString("date", today)
                putString("quote", newQuote)
            }
        }
    }

    private fun checkAndShowBiometricPrompt() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sp = requireContext().getSharedPreferences("nursing_studio_preferences", Context.MODE_PRIVATE)
            val shouldBlockSecureCascade = sp.getBoolean("cascade_mpin_onboarding_never_ask", false)

            if (!shouldBlockSecureCascade) {
                val isMpinActive = dataStoreManager.isMpinSet.firstOrNull() ?: false
                if (!isMpinActive) {
                    showProfessionalBiometricDialog()
                }
            }
        }
    }

    private fun showProfessionalBiometricDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_biometric_prompt, null)

        biometricDialog = MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        biometricDialog?.setOnShowListener {
            val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
            biometricDialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        dialogView.findViewById<View>(R.id.btnSetupNow).setOnClickListener {
            biometricDialog?.dismiss()
            if (isAdded) {
                navigateToFragment(R.id.nav_settings)
            }
        }

        dialogView.findViewById<View>(R.id.btnLater).setOnClickListener {
            biometricDialog?.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnNever).setOnClickListener {
            biometricDialog?.dismiss()
            requireContext().getSharedPreferences("nursing_studio_preferences", Context.MODE_PRIVATE).edit {
                putBoolean("cascade_mpin_onboarding_never_ask", true)
            }
        }

        biometricDialog?.show()
    }

    private fun navigateToFragment(destinationId: Int) {
        if (!isAdded) return
        try {
            findNavController().navigate(destinationId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        // 🚀 FIXED: Zeroed-out window objects mapping leaks explicitly during view detachment vectors
        biometricDialog?.dismiss()
        biometricDialog = null
        super.onDestroyView()
        _binding = null
    }
}