package com.example.nursingstudio.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.nursingstudio.R
import com.example.nursingstudio.data.local.DataStoreManager
import com.example.nursingstudio.databinding.FragmentProfileBinding
import com.example.nursingstudio.ui.auth.AuthActivity
import com.example.nursingstudio.utils.AppSettings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Zero-Latency Hot-Cached Profile Pipeline
 * Implements activity-scoped pre-fetching engine for instant view rendering state updates.
 */
class ProfileFragment : Fragment() {

    // 🚀 FIXED: Upgraded scope to activityViewModels for absolute instant zero-lag data display
    private val viewModel: ProfileViewModel by activityViewModels()
    private val auth = FirebaseAuth.getInstance()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var dataStoreManager: DataStoreManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        dataStoreManager = DataStoreManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInitialTactileUI()
        observeProfileDataStream()
        setupInteractiveClickListeners()

        // 🚀 CRITICAL REFACTOR: Removed raw trigger line because initialization occurs eagerly in shared background memory heap
    }

    private fun setupInitialTactileUI() {
        AppSettings.setPushEffect(binding.btnLogout)
        AppSettings.setPushEffect(binding.layoutProfileAvatarContainer)
    }

    private fun observeProfileDataStream() {
        viewModel.userData.observe(viewLifecycleOwner) { dataMap ->
            dataMap?.let { data ->

                // 1. Core Identity Component Updates via Type-Safe ViewBinding
                val fullName = data["fullName"]?.toString() ?: "Scholar Student"
                val emailStr = data["email"]?.toString() ?: auth.currentUser?.email ?: "-"

                binding.tvName.text = fullName
                binding.tvEmail.text = emailStr

                // 🚀 FIXED: Synchronized Multi-State Identifier Parser Engine
                val uniqueNsId = data["uniqueNsId"]?.toString() ?: "NS-2026-PENDING"
                binding.tvUniqueNsId.text = uniqueNsId

                // Real-time asynchronous dynamic push synchronization to cache
                viewLifecycleOwner.lifecycleScope.launch {
                    if (uniqueNsId != "NS-2026-PENDING") {
                        dataStoreManager.saveUniqueNsId(uniqueNsId)
                    }
                }

                // 🚀 LIVE EMAIL VERIFICATION BADGE CONTROLLER STATE
                evaluateEmailVerificationState()

                // 2. Personal Information Card Rows Mapping Strategy
                setupRow(binding.rowGender.root, getString(R.string.label_gender), data["gender"])

                val dobString = data["dob"]?.toString() ?: ""
                val computedAgeSuffix = if (dobString.isNotEmpty()) calculateAge(dobString) else ""
                setupRow(binding.rowDob.root, getString(R.string.label_dob), "$dobString $computedAgeSuffix".trim())

                setupRow(binding.rowMarital.root, getString(R.string.label_marital), data["maritalStatus"])
                setupRow(binding.rowReligion.root, getString(R.string.label_religion), data["religion"])
                setupRow(binding.rowMobile.root, getString(R.string.label_mobile), data["mobile"])
                setupRow(binding.rowEducation.root, getString(R.string.label_education), data["education"])
                setupRow(binding.rowOccupation.root, getString(R.string.label_occupation), data["occupation"])

                // Address Construction String Assembler Pipeline
                val fullAddressCompiled = buildString {
                    append(data["address"]?.toString() ?: "")
                    data["district"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(", $it") }
                    data["state"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(", $it") }
                    data["country"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(", $it") }
                    data["pincode"]?.toString()?.takeIf { it.isNotEmpty() }?.let { append(" - $it") }
                }.ifEmpty { "-" }

                setupRow(binding.rowAddress.root, getString(R.string.label_address), fullAddressCompiled)

                // 🚀 2026 GOLD STANDARD: Multi-Type Matrix Parsing Engine (Handles Boolean, String, and key fallbacks)
                val rawRegStatus = data["nursingRegStatus"] ?: data["nursingRegistered"] ?: data["isRegistered"]

                val isNursingRegistered = when (rawRegStatus) {
                    is Boolean -> rawRegStatus
                    is String -> rawRegStatus.equals("Yes", ignoreCase = true) || rawRegStatus.equals("true", ignoreCase = true)
                    else -> false
                }

                if (isNursingRegistered) {
                    // Force text and append structural green verification styling icon
                    binding.tvNursingStatus.text = getString(R.string.nursing_registered_yes)
                    binding.tvNursingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.medical_teal))

                    binding.rowNursingState.root.visibility = View.VISIBLE
                    binding.rowNursingNo.root.visibility = View.VISIBLE

                    // Safe fallback lookups for registration data node elements
                    val stateRegistered = data["nursingState"] ?: data["regState"] ?: "-"
                    val registrationNumber = data["nursingRegNo"] ?: data["regNo"] ?: "-"

                    setupRow(binding.rowNursingState.root, "Registered State", stateRegistered)
                    setupRow(binding.rowNursingNo.root, "Registration No", registrationNumber)
                } else {
                    binding.tvNursingStatus.text = getString(R.string.nursing_registered_no)
                    binding.tvNursingStatus.setTextColor(Color.RED)
                    binding.rowNursingState.root.visibility = View.GONE
                    binding.rowNursingNo.root.visibility = View.GONE
                }
            }
        }
    }

    private fun setupRow(rowView: View, label: String, value: Any?) {
        rowView.findViewById<TextView>(R.id.tvLabel).text = label
        rowView.findViewById<TextView>(R.id.tvValue).text = value?.toString() ?: "-"
    }

    private fun evaluateEmailVerificationState() {
        val currentUserToken = auth.currentUser
        if (currentUserToken != null && currentUserToken.isEmailVerified) {
            binding.tvEmailBadgeStatus.text = getString(R.string.verified)
            binding.tvEmailBadgeStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            binding.btnVerifyNowAction.visibility = View.GONE
        } else {
            binding.tvEmailBadgeStatus.text = getString(R.string.not_verified)
            binding.tvEmailBadgeStatus.setTextColor(Color.RED)
            binding.btnVerifyNowAction.visibility = View.VISIBLE
        }
    }

    private fun setupInteractiveClickListeners() {
        binding.layoutProfileAvatarContainer.setOnClickListener {
            Toast.makeText(context, "Opening Camera Hardware Profile Scanner Launcher...", Toast.LENGTH_SHORT).show()
        }

        binding.btnVerifyNowAction.setOnClickListener {
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Verification email dispatched successfully! ✉️ Check your inbox.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Mailer lookup error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLogout.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.MaterialAlertDialog_Rounded)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to terminate your current Nursing Studio learning session?")
                .setCancelable(true)
                .setPositiveButton("Logout") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        auth.signOut()
                        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        activity?.finish()
                    }
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun calculateAge(dobString: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return ""
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
            "($age Years)"
        } catch (_: Exception) { "" }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}