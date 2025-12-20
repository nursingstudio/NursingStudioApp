package com.example.nursingstudio

import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView


class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var isOtpVerified = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Views
        val etName = view.findViewById<EditText>(R.id.etName)
        val spGender = view.findViewById<Spinner>(R.id.spGender)
        val spDay = view.findViewById<Spinner>(R.id.spDay)
        val spMonth = view.findViewById<Spinner>(R.id.spMonth)
        val spYear = view.findViewById<Spinner>(R.id.spYear)
        val spReligion = view.findViewById<Spinner>(R.id.spReligion)
        val spMarital = view.findViewById<Spinner>(R.id.spMarital)
        val etMobile = view.findViewById<EditText>(R.id.etMobile)
        val etOtp = view.findViewById<EditText>(R.id.etOtp)
        val btnSendOtp = view.findViewById<Button>(R.id.btnSendOtp)
        val btnVerifyOtp = view.findViewById<Button>(R.id.btnVerifyOtp)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val spEducation = view.findViewById<Spinner>(R.id.spEducation)
        val etEducationOther = view.findViewById<EditText>(R.id.etEducationOther)
        val spOccupation = view.findViewById<Spinner>(R.id.spOccupation)
        val etOccupationOther = view.findViewById<EditText>(R.id.etOccupationOther)
        val spCountry = view.findViewById<Spinner>(R.id.spCountry)
        val etCountryOther = view.findViewById<EditText>(R.id.etCountryOther)
        val spStateIndia = view.findViewById<Spinner>(R.id.spStateIndia)
        val etStateOther = view.findViewById<EditText>(R.id.etStateOther)
        val etDistrict = view.findViewById<EditText>(R.id.etDistrict)
        val etTehsil = view.findViewById<EditText>(R.id.etTehsil)
        val etAddress = view.findViewById<EditText>(R.id.etAddress)
        val etPincode = view.findViewById<EditText>(R.id.etPincode)
        val rgNursingReg = view.findViewById<RadioGroup>(R.id.rgNursingReg)
        val rbRegYes = view.findViewById<RadioButton>(R.id.rbRegYes)
        val rbRegNo = view.findViewById<RadioButton>(R.id.rbRegNo)
        val layoutRegDetails = view.findViewById<LinearLayout>(R.id.layoutRegDetails)
        val etRegState = view.findViewById<EditText>(R.id.etRegState)
        val etRegNumber = view.findViewById<EditText>(R.id.etRegNumber)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)

        // -------- Spinners (WORKING) --------
        val genderList = listOf("Select gender","Male","Female","Transgender")
        val maritalList = listOf("Select marital status","Unmarried","Married","Divorced","Widow/Widower")
        val religionList = listOf("Select religion","Hindu","Muslim","Christian","Sikh","Buddhist","Jain","Other")
        val educationList = listOf("Select education","ANM","GNM","B.Sc. Nursing","Post Basic B.Sc. Nursing","M.Sc. Nursing","PhD Nursing","Other")
        val occupationList = listOf("Select occupation","Student","Preparation (Competitive Exam)","Private Job (Hospital/Clinic)","Contractual Job (Govt. Hospital)","Private Job + Preparation","Nursing Officer","CHO","Nursing Tutor","SNO","Ward Incharge","Metron","ANS","DNS","Nursing Superintendent","Other")
        val countryList = listOf("Select country","Bharat (India)","Other")
        val states = listOf("Select state/UT","Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Andaman and Nicobar Islands","Chandigarh","Dadra & Nagar Haveli and Daman & Diu","Delhi","Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry")
        val dayList = mutableListOf("DD").apply { for (i in 1..31) add("%02d".format(i)) }
        val monthList = mutableListOf("MM").apply { for (i in 1..12) add("%02d".format(i)) }
        val yearList = mutableListOf("YYYY").apply {
            val y = Calendar.getInstance().get(Calendar.YEAR)
            for (i in y downTo 1900) add(i.toString())
        }

        fun <T> Spinner.set(list: List<T>) {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
        }

        spGender.set(genderList)
        spMarital.set(maritalList)
        spReligion.set(religionList)
        spEducation.set(educationList)
        spOccupation.set(occupationList)
        spCountry.set(countryList)
        spStateIndia.set(states)
        spDay.set(dayList); spMonth.set(monthList); spYear.set(yearList)

        spEducation.onItemSelectedListener = simpleListener {
            etEducationOther.visibility = if (educationList[it] == "Other") View.VISIBLE else View.GONE
        }
        spOccupation.onItemSelectedListener = simpleListener {
            etOccupationOther.visibility = if (occupationList[it] == "Other") View.VISIBLE else View.GONE
        }
        spCountry.onItemSelectedListener = simpleListener {
            when (countryList[it]) {
                "Bharat (India)" -> { spStateIndia.visibility = View.VISIBLE; etStateOther.visibility = View.GONE; etCountryOther.visibility = View.GONE }
                "Other" -> { spStateIndia.visibility = View.GONE; etStateOther.visibility = View.VISIBLE; etCountryOther.visibility = View.VISIBLE }
                else -> { spStateIndia.visibility = View.GONE; etStateOther.visibility = View.GONE; etCountryOther.visibility = View.GONE }
            }
        }
        rgNursingReg.setOnCheckedChangeListener { _, id ->
            layoutRegDetails.visibility = if (id == R.id.rbRegYes) View.VISIBLE else View.GONE
        }

        // -------- Firebase OTP --------
        btnSendOtp.setOnClickListener {
            val mob = etMobile.text.toString().trim()
            if (mob.length != 10) { toast("Enter 10-digit mobile"); return@setOnClickListener }
            val phone = "+91$mob"

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (verificationId.isNullOrEmpty() || code.isEmpty()) {
                toast("Send OTP first"); return@setOnClickListener
            }
            val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            auth.signInWithCredential(credential)
                .addOnSuccessListener { isOtpVerified = true; toast("OTP verified") }
                .addOnFailureListener { toast(it.message ?: "OTP failed") }
        }

        // -------- Register Save --------
        btnRegister.setOnClickListener {
            if (!isOtpVerified) { toast("Verify mobile OTP first"); return@setOnClickListener }
            if (etName.text.isNullOrBlank()) { toast("Enter name"); return@setOnClickListener }
            if (etEmail.text.isNullOrBlank() || !Patterns.EMAIL_ADDRESS.matcher(etEmail.text).matches()) {
                toast("Enter valid email"); return@setOnClickListener
            }
            if (etPassword.text.length < 4) { toast("Password too short"); return@setOnClickListener }

            // Local save (profile)
            val sp = requireContext().getSharedPreferences("session", 0)
            sp.edit()
                .putString("reg_name", etName.text.toString())
                .putString("reg_mobile", etMobile.text.toString())
                .putString("reg_email", etEmail.text.toString())
                .putString("reg_password", etPassword.text.toString())
                .putBoolean("logged_in", true)
                .apply()

            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(cred: PhoneAuthCredential) {
            FirebaseAuth.getInstance().signInWithCredential(cred)
                .addOnSuccessListener { isOtpVerified = true }
        }
        override fun onVerificationFailed(e: FirebaseException) {
            toast(e.message ?: "OTP error")
        }
        override fun onCodeSent(
            id: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            verificationId = id
            resendToken = token
            toast("OTP sent")
        }
    }

    private fun simpleListener(block: (Int) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = block(pos)
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
