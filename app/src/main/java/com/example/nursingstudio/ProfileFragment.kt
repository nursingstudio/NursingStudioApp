package com.example.nursingstudio.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.R
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        viewModel.userData.observe(viewLifecycleOwner) { data ->
            data?.let {
                tvName.text = it["fullName"]?.toString() ?: "User"
                tvEmail.text = it["email"]?.toString() ?: "-"

                // Set Rows with strings.xml
                setupRow(view.findViewById(R.id.rowGender), getString(R.string.label_gender), it["gender"])
                setupRow(view.findViewById(R.id.rowDob), getString(R.string.label_dob), "${it["dob"]} ${calculateAge(it["dob"].toString())}")
                setupRow(view.findViewById(R.id.rowMarital), getString(R.string.label_marital), it["maritalStatus"])
                setupRow(view.findViewById(R.id.rowReligion), getString(R.string.label_religion), it["religion"])
                setupRow(view.findViewById(R.id.rowMobile), getString(R.string.label_mobile), it["mobile"])
                setupRow(view.findViewById(R.id.rowEducation), getString(R.string.label_education), it["education"])
                setupRow(view.findViewById(R.id.rowOccupation), getString(R.string.label_occupation), it["occupation"])

                val addr = "${it["address"]}, ${it["district"]}, ${it["state"]}, ${it["country"]} - ${it["pincode"]}"
                setupRow(view.findViewById(R.id.rowAddress), getString(R.string.label_address), addr)

                val isReg = it["nursingRegStatus"]?.toString() ?: ""
                view.findViewById<TextView>(R.id.tvNursingStatus).text = getString(R.string.label_nursing_reg, isReg)
            }
        }

        viewModel.fetchProfile()

        btnLogout.setOnClickListener {
            auth.signOut()
            activity?.finish()
        }
    }

    private fun setupRow(row: View, label: String, value: Any?) {
        row.findViewById<TextView>(R.id.tvLabel).text = label
        row.findViewById<TextView>(R.id.tvValue).text = value?.toString() ?: "-"
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
        } catch (e: Exception) { "" }
    }
}