package com.example.nursingstudio

import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.TextPaint
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseException

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationId: String? = null
    private var isOtpVerified = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- View Mapping ---
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
        val layoutRegDetails = view.findViewById<LinearLayout>(R.id.layoutRegDetails)
        val etRegState = view.findViewById<EditText>(R.id.etRegState)
        val etRegNumber = view.findViewById<EditText>(R.id.etRegNumber)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val tvTermsText = view.findViewById<TextView>(R.id.tvTermsText)

        // --- Lists (Your Original Ones) ---
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

        // --- Adapters ---
        fun <T> Spinner.set(list: List<T>) {
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, list)
        }
        spGender.set(genderList); spMarital.set(maritalList); spReligion.set(religionList)
        spEducation.set(educationList); spOccupation.set(occupationList)
        spCountry.set(countryList); spStateIndia.set(states)
        spDay.set(dayList); spMonth.set(monthList); spYear.set(yearList)

        // --- UI Listeners ---
        spEducation.onItemSelectedListener = simpleListener { etEducationOther.visibility = if (educationList[it] == "Other") View.VISIBLE else View.GONE }
        spOccupation.onItemSelectedListener = simpleListener { etOccupationOther.visibility = if (occupationList[it] == "Other") View.VISIBLE else View.GONE }
        spCountry.onItemSelectedListener = simpleListener {
            when (countryList[it]) {
                "Bharat (India)" -> { spStateIndia.visibility = View.VISIBLE; etStateOther.visibility = View.GONE; etCountryOther.visibility = View.GONE }
                "Other" -> { spStateIndia.visibility = View.GONE; etStateOther.visibility = View.VISIBLE; etCountryOther.visibility = View.VISIBLE }
                else -> { spStateIndia.visibility = View.GONE; etStateOther.visibility = View.GONE; etCountryOther.visibility = View.GONE }
            }
        }
        rgNursingReg.setOnCheckedChangeListener { _, id -> layoutRegDetails.visibility = if (id == R.id.rbRegYes) View.VISIBLE else View.GONE }

        // --- TN&C AND PRIVACY POLICY LOGIC (PERFECTED) ---
        val fullText = "I have read and agree to the Terms & Conditions and Privacy Policy."
        val spannable = SpannableString(fullText)

        // Helper function for Saffron Clickable Links
        fun makeLink(start: Int, end: Int, action: () -> Unit): ClickableSpan {
            return object : ClickableSpan() {
                override fun onClick(widget: View) {
                    action()
                }
                override fun updateDrawState(ds: TextPaint) {
                    // ContextCompat se color uthana sabse safe hai
                    ds.color = ContextCompat.getColor(requireContext(), R.color.saffron)
                    ds.isUnderlineText = true // Isse professional link look aayega
                }
            }
        }

        // Terms & Conditions Link
        val termsStart = fullText.indexOf("Terms & Conditions")
        val termsEnd = termsStart + "Terms & Conditions".length
        if (termsStart != -1) {
            spannable.setSpan(makeLink(termsStart, termsEnd) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, TermsFragment())
                    .addToBackStack(null)
                    .commit()
            }, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Privacy Policy Link
        val privacyStart = fullText.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        if (privacyStart != -1) {
            spannable.setSpan(makeLink(privacyStart, privacyEnd) {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.auth_container, PrivacyFragment())
                    .addToBackStack(null)
                    .commit()
            }, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        tvTermsText.text = spannable
        tvTermsText.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        tvTermsText.highlightColor = android.graphics.Color.TRANSPARENT
        // --- LOGIC END ---


        // --- OTP ---
        btnSendOtp.setOnClickListener {
            val mob = etMobile.text.toString().trim()
            if (mob.length == 10) {
                PhoneAuthProvider.verifyPhoneNumber(PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber("+91$mob").setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(requireActivity()).setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(cred: PhoneAuthCredential) { isOtpVerified = true }
                        override fun onVerificationFailed(e: FirebaseException) { toast(e.message ?: "Error") }
                        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = id; toast("OTP Sent!") }
                    }).build())
            } else toast("Enter 10-digit mobile")
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (verificationId != null && code.isNotEmpty()) {
                auth.signInWithCredential(PhoneAuthProvider.getCredential(verificationId!!, code))
                    .addOnSuccessListener { isOtpVerified = true; toast("Verified! ✅") }
            }
        }

        // --- Final Pro Registration ---
        btnRegister.setOnClickListener {
            // --- 1. Basic Data Nikalna ---
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val district = etDistrict.text.toString().trim()
            val tehsil = etTehsil.text.toString().trim()
            val pincode = etPincode.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val dob = "${spDay.selectedItem}-${spMonth.selectedItem}-${spYear.selectedItem}"

            val marital = if(spMarital.selectedItemPosition != 0) spMarital.selectedItem.toString() else ""
            val religion = if(spReligion.selectedItemPosition != 0) spReligion.selectedItem.toString() else ""
            val gender = if(spGender.selectedItemPosition != 0) spGender.selectedItem.toString() else ""
            val education = if(spEducation.selectedItemPosition != 0) spEducation.selectedItem.toString() else ""
            val occupation = if(spOccupation.selectedItemPosition != 0) spOccupation.selectedItem.toString() else ""
            val country = if(spCountry.selectedItemPosition != 0) spCountry.selectedItem.toString() else ""
            val state = if(spCountry.selectedItemPosition == 1) spStateIndia.selectedItem.toString() else ""
            val regState = if(rbRegYes.isChecked) etRegState.text.toString().trim() else ""
            val regNumber = if(rbRegYes.isChecked) etRegNumber.text.toString().trim() else ""

            // --- 2. Mandatory Fields Validation ---
            if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || pass.isEmpty() ||
                district.isEmpty() || tehsil.isEmpty() || regState.isEmpty() || regNumber.isEmpty() ||
                pincode.isEmpty() || address.isEmpty() || gender.isEmpty() ||
                marital.isEmpty() || religion.isEmpty() || dob.isEmpty() || education.isEmpty() || occupation.isEmpty() || country.isEmpty() || state.isEmpty()) {
                toast("Please fill in all the required fields to proceed.")
                return@setOnClickListener
            }

            // --- 3. Special "Other" Validation (Jo tumne manga tha) ---
            if (education == "Other" && etEducationOther.text.toString().trim().isEmpty()) {
                toast("Please specify your details in the 'Other' field.")
                return@setOnClickListener
            }
            if (occupation == "Other" && etOccupationOther.text.toString().trim().isEmpty()) {
                toast("Please specify your details in the 'Other' field.")
                return@setOnClickListener
            }
            if (country == "Other" && etCountryOther.text.toString().trim().isEmpty()) {
                toast("Please specify your details in the 'Other' field.")
                return@setOnClickListener
            }

            // --- 4. OTP & Terms Check ---
            if (!isOtpVerified) {
                toast("Please verify your mobile number via OTP.")
                return@setOnClickListener
            }
            if (!cbTerms.isChecked) {
                toast("Please accept the Terms & Conditions and Privacy Policy.")
                return@setOnClickListener
            }

            // --- 5. Firebase Auth (Sirf Email se Account Banega - Ek hi UID ke liye) ---
            auth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener { result ->
                    val uid = result.user!!.uid

                    // Pura data jo Firestore mein jayega
                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "mobile" to mobile,
                        "gender" to gender,
                        "dob" to dob,
                        "religion" to spReligion.selectedItem.toString(),
                        "maritalStatus" to spMarital.selectedItem.toString(),
                        "education" to if(education == "Other") etEducationOther.text.toString() else education,
                        "occupation" to if(occupation == "Other") etOccupationOther.text.toString() else occupation,
                        "country" to if(country == "Other") etCountryOther.text.toString() else country,
                        "state" to if(country == "Other") etStateOther.text.toString() else state,
                        "tehsil" to tehsil,
                        "address" to address,
                        "district" to district,
                        "pincode" to pincode,
                        "nursingRegStatus" to if(rbRegYes.isChecked) "Yes" else "No",
                        "regState" to etRegState.text.toString(),
                        "regNumber" to etRegNumber.text.toString(),
                        "password" to pass,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    // Firestore mein save karna
                    db.collection("Users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            // Professional Success Toast
                            toast("Registration successful! Welcome to Nursing Studio.")

                            // Navigate to Login Fragment (Professional way)
                            requireActivity().supportFragmentManager.beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.auth_container, LoginFragment())
                                .commit()
                        }
                        .addOnFailureListener { e ->
                            toast("Registration failed: ${e.localizedMessage}")
                        }
                }
                .addOnFailureListener { e -> toast("Error: ${e.message}") }
        }


    }

    private fun setupTermsSaffron(tv: TextView) {
        val fullText = "I have read and agree to the Terms & Conditions and Privacy Policy."
        val spannable = android.text.SpannableString(fullText)
        val saffronLink = object : ClickableSpan() {
            override fun onClick(v: View) { /* Open Fragment logic */ }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.saffron)
                ds.isUnderlineText = false
            }
        }
        spannable.setSpan(saffronLink, 32, 50, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable
        tv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun simpleListener(block: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = block(pos)
        override fun onNothingSelected(p: AdapterView<*>?) {}
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}