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
import com.example.nursingstudio.R
import com.example.nursingstudio.utils.ProgressManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.fragment.findNavController

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
            navigateToFragment(R.id.nav_quiz)
        }

        cardPdf.setOnClickListener {
            ProgressManager.increment(requireContext(), "pdf_opened")
            navigateToFragment(R.id.nav_pdf)
        }

        cardVideo.setOnClickListener {
            ProgressManager.increment(requireContext(), "video_watched")
            navigateToFragment(R.id.nav_video)
        }

        cardProgress.setOnClickListener {
            navigateToFragment(R.id.nav_profile)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)

        // 1. Initial State: Professional Placeholder (Jab tak data load ho raha hai)
        tvWelcome.text = getString(R.string.hello_future_nursing_offiecr)

        // 2. Immediate Real-time Fetch from Firebase
        fetchUserRealtime(tvWelcome)

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
    private fun fetchUserRealtime(tvWelcome: TextView) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        db.collection("Users").document(userId).addSnapshotListener { snapshot, e ->
            // ⭐ GOLD STANDARD 2026: Safety check first
            if (!isAdded || context == null) return@addSnapshotListener

            if (e != null) {
                val session = requireActivity().getSharedPreferences("session", Context.MODE_PRIVATE)
                val localName = session.getString("reg_name", "Scholar")
                tvWelcome.text = getString(R.string.welcome_user, localName)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("fullName") ?: "Scholar"
                tvWelcome.text = getString(R.string.welcome_user, name)

                // Context use karne se pehle safety check
                activity?.getSharedPreferences("session", Context.MODE_PRIVATE)?.edit {
                    putString("reg_name", name)
                }
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
                navigateToFragment(R.id.nav_settings)
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
    private fun navigateToFragment(destinationId: Int) {
        if (!isAdded) return
        try {
            // Direct call without full package name
            findNavController().navigate(destinationId)
        } catch (e: Exception) {
            e.printStackTrace() // Debugging ke liye zaruri hai
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        biometricDialog?.dismiss()
    }
}