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

    private fun validateOrStop(cond: Boolean, msg: String): Boolean {
        if (!cond) {
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }


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
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val tvTermsText = view.findViewById<TextView>(R.id.tvTermsText)

        val fullText =
            "I have read and agree to the Terms & Conditions and Privacy Policy."

        val spannable = android.text.SpannableString(fullText)

// Terms click
        val termsStart = fullText.indexOf("Terms")
        val termsEnd = termsStart + "Terms & Conditions".length

        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, TermsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }, termsStart, termsEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

// Privacy click
        val privacyStart = fullText.indexOf("Privacy")
        val privacyEnd = privacyStart + "Privacy Policy".length

        spannable.setSpan(object : android.text.style.ClickableSpan() {
            override fun onClick(widget: View) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, PrivacyFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }, privacyStart, privacyEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        tvTermsText.text = spannable
        tvTermsText.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvTermsText.highlightColor = android.graphics.Color.TRANSPARENT


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

        tvTermsText.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, TermsFragment())
                .addToBackStack(null)
                .commit()
        }

        // --------- HARD VALIDATION ---------

        btnRegister.setOnClickListener {

            if (!validateOrStop(etName.text.isNotBlank(), "Enter full name")) return@setOnClickListener
            if (!validateOrStop(spGender.selectedItemPosition != 0, "Select gender")) return@setOnClickListener

            if (!validateOrStop(
                    spDay.selectedItemPosition != 0 &&
                            spMonth.selectedItemPosition != 0 &&
                            spYear.selectedItemPosition != 0,
                    "Select complete DOB"
                )) return@setOnClickListener

            if (!validateOrStop(spMarital.selectedItemPosition != 0, "Select marital status")) return@setOnClickListener
            if (!validateOrStop(spReligion.selectedItemPosition != 0, "Select religion")) return@setOnClickListener

            if (!validateOrStop(etMobile.text.length == 10, "Enter valid mobile")) return@setOnClickListener
            if (!validateOrStop(isOtpVerified, "Verify mobile OTP first")) return@setOnClickListener

            if (!validateOrStop(
                    Patterns.EMAIL_ADDRESS.matcher(etEmail.text).matches(),
                    "Enter valid email"
                )) return@setOnClickListener

            if (!validateOrStop(etPassword.text.length >= 4, "Password too short")) return@setOnClickListener

            if (!validateOrStop(spEducation.selectedItemPosition != 0, "Select education")) return@setOnClickListener
            if (spEducation.selectedItem == "Other") {
                if (!validateOrStop(etEducationOther.text.isNotBlank(), "Enter education")) return@setOnClickListener
            }

            if (!validateOrStop(spOccupation.selectedItemPosition != 0, "Select occupation")) return@setOnClickListener
            if (spOccupation.selectedItem == "Other") {
                if (!validateOrStop(etOccupationOther.text.isNotBlank(), "Enter occupation")) return@setOnClickListener
            }

            if (!validateOrStop(spCountry.selectedItemPosition != 0, "Select country")) return@setOnClickListener
            if (spCountry.selectedItem == "Bharat (India)") {
                if (!validateOrStop(spStateIndia.selectedItemPosition != 0, "Select state")) return@setOnClickListener
            } else {
                if (!validateOrStop(etCountryOther.text.isNotBlank(), "Enter country")) return@setOnClickListener
                if (!validateOrStop(etStateOther.text.isNotBlank(), "Enter state")) return@setOnClickListener
            }

            if (!validateOrStop(etDistrict.text.isNotBlank(), "Enter district")) return@setOnClickListener
            if (!validateOrStop(etTehsil.text.isNotBlank(), "Enter tehsil")) return@setOnClickListener
            if (!validateOrStop(etAddress.text.isNotBlank(), "Enter address")) return@setOnClickListener
            if (!validateOrStop(etPincode.text.length == 6, "Enter valid pincode")) return@setOnClickListener

            if (!validateOrStop(rgNursingReg.checkedRadioButtonId != -1, "Select nursing registration")) return@setOnClickListener
            if (rbRegYes.isChecked) {
                if (!validateOrStop(etRegState.text.isNotBlank(), "Enter registration state")) return@setOnClickListener
                if (!validateOrStop(etRegNumber.text.isNotBlank(), "Enter registration number")) return@setOnClickListener
            }

            if (!validateOrStop(cbTerms.isChecked, "Accept Terms & Conditions")) return@setOnClickListener

            // ✅ Yahan se registration save / firebase / navigation chalegi
            // ---------- SAVE / NEXT STEP ----------
            // yahan Firebase profile save / Firestore / navigation aayega
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
