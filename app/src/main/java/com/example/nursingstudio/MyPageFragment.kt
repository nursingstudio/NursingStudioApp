package com.example.nursingstudio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyPageFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imgProfile = view.findViewById<ImageView>(R.id.imgProfile)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvGender = view.findViewById<TextView>(R.id.tvGender)
        val tvDobWithAge = view.findViewById<TextView>(R.id.tvDobWithAge)
        val tvMarital = view.findViewById<TextView>(R.id.tvMarital)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvMobile = view.findViewById<TextView>(R.id.tvMobile)
        val tvEducation = view.findViewById<TextView>(R.id.tvEducation)
        val tvOccupation = view.findViewById<TextView>(R.id.tvOccupation)
        val tvAddressFull = view.findViewById<TextView>(R.id.tvAddressFull)
        val tvNursingRegStatus = view.findViewById<TextView>(R.id.tvNursingRegStatus)
        val tvNursingRegDetails = view.findViewById<TextView>(R.id.tvNursingRegDetails)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val sp = requireContext().getSharedPreferences("session", 0)

        val name = sp.getString("reg_name", "User")
        val gender = sp.getString("reg_gender", "-")
        val dob = sp.getString("reg_dob", "-")
        val marital = sp.getString("reg_marital", "-")
        val email = sp.getString("reg_email", "-")
        val mobile = sp.getString("reg_mobile", "-")
        val education = sp.getString("reg_education", "-")
        val occupation = sp.getString("reg_occupation", "-")
        val country = sp.getString("reg_country", "")
        val state = sp.getString("reg_state", "")
        val district = sp.getString("reg_district", "")
        val tehsil = sp.getString("reg_tehsil", "")
        val address = sp.getString("reg_address", "")
        val pincode = sp.getString("reg_pincode", "")
        val hasReg = sp.getString("reg_has_nursing_reg", "No")
        val regState = sp.getString("reg_nursing_reg_state", "")
        val regNumber = sp.getString("reg_nursing_reg_number", "")

        tvWelcome.text = "Welcome,"
        tvName.text = name
        tvGender.text = gender
        tvMarital.text = marital
        tvEmail.text = email
        tvMobile.text = mobile
        tvEducation.text = education
        tvOccupation.text = occupation

        val fullAddress = "$address, $tehsil, $district, $state, $country - $pincode"
        tvAddressFull.text = fullAddress

        // DOB + Age
        val ageText = calculateAgeText(dob ?: "")
        tvDobWithAge.text = if (ageText.isNotEmpty()) "$dob ($ageText)" else dob

        // Nursing registration
        tvNursingRegStatus.text = hasReg
        tvNursingRegDetails.text =
            if (hasReg == "Yes") "State: $regState | Reg. No: $regNumber" else ""

        // Logout
        btnLogout.setOnClickListener {
            sp.edit().putBoolean("logged_in", false).apply()
            val intent = android.content.Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun calculateAgeText(dobString: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return ""
            val dobCal = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()

            var years = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - dobCal.get(Calendar.MONTH)

            if (months < 0) {
                years--
                months += 12
            }

            if (years < 0) return ""

            "$years years, $months months"
        } catch (e: Exception) {
            ""
        }
    }
}
