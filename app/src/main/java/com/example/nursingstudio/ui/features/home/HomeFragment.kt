package com.example.nursingstudio.ui.features.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.nursingstudio.ui.features.media.PdfFragment
import com.example.nursingstudio.utils.ProgressManager
import com.example.nursingstudio.ui.features.quiz.QuizFragment
import com.example.nursingstudio.R
import com.example.nursingstudio.ui.features.settings.SettingsFragment
import com.example.nursingstudio.ui.features.media.VideoFragment
import com.example.nursingstudio.ui.profile.ProfileFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    private var biometricDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Cards
        val cardTest = view.findViewById<MaterialCardView>(R.id.cardTest)
        val cardPdf = view.findViewById<MaterialCardView>(R.id.cardPdf)
        val cardVideo = view.findViewById<MaterialCardView>(R.id.cardVideo)
        val cardProgress = view.findViewById<MaterialCardView>(R.id.cardProgress)

        cardTest.setOnClickListener {
            ProgressManager.increment(requireContext(), "test_attempted")
            openFragment(QuizFragment())
        }

        cardPdf.setOnClickListener {
            ProgressManager.increment(requireContext(), "pdf_opened")
            openFragment(PdfFragment())
        }

        cardVideo.setOnClickListener {
            ProgressManager.increment(requireContext(), "video_watched")
            openFragment(VideoFragment())
        }

        cardProgress.setOnClickListener {
            openFragment(ProfileFragment())
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)

        val session = requireActivity().getSharedPreferences("session", Context.MODE_PRIVATE)
        val name = session.getString("reg_name", "User")
        tvWelcome.text = getString(R.string.welcome_user, name)

        checkAndShowBiometricPrompt()
        setupDailyMotivation(view)
    }

    private fun setupDailyMotivation(view: View) {
        val tvMotivation = view.findViewById<TextView>(R.id.tvMotivation)

        val quotes = listOf(
            "Consistency beats intensity.",
            "Small steps daily create big success.",
            "Today’s effort is tomorrow’s result.",
            "Study smart, not just hard.",
            "Discipline today, success tomorrow."
        )

        val sp = requireContext()
            .getSharedPreferences("daily_motivation", Context.MODE_PRIVATE)

        val today = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val savedDate = sp.getString("date", "")
        val savedQuote = sp.getString("quote", "")

        if (today == savedDate && !savedQuote.isNullOrEmpty()) {
            tvMotivation.text = savedQuote
        } else {
            val newQuote = quotes.random()
            tvMotivation.text = newQuote

            sp.edit(commit = false) {
                putString("date", today)
                putString("quote", newQuote)
            }
        }
    }

    private fun checkAndShowBiometricPrompt() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val isBiometricEnabled = prefs.getBoolean("biometric_enabled", false)
        val shouldShowPrompt = prefs.getBoolean("show_biometric_prompt", true)

        // Agar enabled nahi hai aur user ne 'Never' nahi bola, tabhi dikhao
        if (!isBiometricEnabled && shouldShowPrompt) {
            showProfessionalBiometricDialog()
        }
    }

    private fun showProfessionalBiometricDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_biometric_prompt, null)

        biometricDialog = MaterialAlertDialogBuilder(
            requireContext(),
            R.style.MaterialAlertDialog_Rounded
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btnSetupNow).setOnClickListener {
            biometricDialog?.dismiss()
            if (isAdded && activity != null) {
                openFragment(SettingsFragment())
            }
        }

        dialogView.findViewById<View>(R.id.btnLater).setOnClickListener {
            biometricDialog?.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnNever).setOnClickListener {
            biometricDialog?.dismiss()
            requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit {
                putBoolean("show_biometric_prompt", false)
            }
        }

        biometricDialog?.show()
    }
    private fun openFragment(fragment: Fragment) {
        if (!isAdded) return
        activity?.supportFragmentManager?.beginTransaction()
            ?.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        biometricDialog?.dismiss()
    }
}