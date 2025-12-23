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
import android.util.Log
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseException
import com.hbb20.CountryCodePicker

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationId: String? = null
    private var isOtpVerified = false
    private var countDownTimer: android.os.CountDownTimer? = null

    private lateinit var ccp: CountryCodePicker
    private lateinit var tvTimer: TextView
    private lateinit var btnResendOtp: TextView
    private lateinit var btnSendOtp: Button
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

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
        btnSendOtp = view.findViewById(R.id.btnSendOtp)
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
        val layoutRegDetails = view.findViewById<LinearLayout>(R.id.layoutRegDetails)
        val etRegState = view.findViewById<EditText>(R.id.etRegState)
        val etRegNumber = view.findViewById<EditText>(R.id.etRegNumber)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val tvTermsText = view.findViewById<TextView>(R.id.tvTermsText)

        ccp = view.findViewById(R.id.ccp)
        ccp.registerCarrierNumberEditText(etMobile)
        tvTimer = view.findViewById(R.id.tvTimer)
        btnResendOtp = view.findViewById(R.id.btnResendOtp)

        // --- Spinners Lists & Adapters ---
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
        spGender.set(genderList); spMarital.set(maritalList); spReligion.set(religionList)
        spEducation.set(educationList); spOccupation.set(occupationList)
        spCountry.set(countryList); spStateIndia.set(states)
        spDay.set(dayList); spMonth.set(monthList); spYear.set(yearList)

        // --- Dynamic UI Logic ---
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

        // --- Terms & Policy (Saffron Theme) ---
        setupTermsSaffron(tvTermsText)

        // --- OTP SEND LOGIC (Refined) ---
        btnSendOtp.setOnClickListener {
            val fullPhoneNumber = ccp.fullNumberWithPlus.trim()
            val mobileOnly = etMobile.text.toString().trim()

            if (mobileOnly.length == 10) {
                btnSendOtp.isEnabled = false // Disable to prevent double click
                startResendTimer()

                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(fullPhoneNumber)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(requireActivity())
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            isOtpVerified = true
                            etOtp.setText(credential.smsCode)
                            toast("Auto-Verified! ✅")
                        }

                        override fun onVerificationFailed(e: FirebaseException) {
                            btnSendOtp.isEnabled = true // Re-enable if failed
                            btnSendOtp.alpha = 1.0f
                            countDownTimer?.cancel()
                            tvTimer.visibility = View.GONE
                            toast("Failed: ${e.message}")
                        }

                        override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                            verificationId = id
                            resendToken = token
                            toast("OTP Sent to $fullPhoneNumber")
                        }
                    })
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            } else {
                toast("Please enter a valid 10-digit mobile number")
            }
        }

        // --- RESEND OTP ---
        btnResendOtp.setOnClickListener {
            val fullPhoneNumber = ccp.fullNumberWithPlus.trim()
            startResendTimer()

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(fullPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) { isOtpVerified = true }
                    override fun onVerificationFailed(e: FirebaseException) { toast(e.message ?: "Error") }
                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = id
                        resendToken = token
                        toast("OTP Resent!")
                    }
                })
                .setForceResendingToken(resendToken!!)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        // --- VERIFY OTP ---
        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (verificationId == null) {
                toast("Action Required: Please initiate mobile verification first.")
                return@setOnClickListener
            }
            if (code.length < 6) {
                etOtp.error = "Full 6-digit code required"
                return@setOnClickListener
            }

            val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            auth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    isOtpVerified = true
                    etOtp.isEnabled = false
                    btnVerifyOtp.visibility = View.GONE
                    btnSendOtp.visibility = View.GONE
                    tvTimer.visibility = View.GONE
                    btnResendOtp.visibility = View.GONE
                    toast("Mobile Verified Successfully! ✅")
                    btnRegister.isEnabled = true
                } else {
                    val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                    etOtp.startAnimation(shake)
                    etOtp.error = "Incorrect Verification Code"
                    toast("Invalid OTP. Please check and try again.")
                }
            }
        }

        // --- REGISTER LOGIC ---
        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val district = etDistrict.text.toString().trim()
            val tehsil = etTehsil.text.toString().trim()
            val pincode = etPincode.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val dob = "${spDay.selectedItem}-${spMonth.selectedItem}-${spYear.selectedItem}"

            val gender = if(spGender.selectedItemPosition != 0) spGender.selectedItem.toString() else ""
            val marital = if(spMarital.selectedItemPosition != 0) spMarital.selectedItem.toString() else ""
            val religion = if(spReligion.selectedItemPosition != 0) spReligion.selectedItem.toString() else ""
            val education = if(spEducation.selectedItemPosition != 0) spEducation.selectedItem.toString() else ""
            val occupation = if(spOccupation.selectedItemPosition != 0) spOccupation.selectedItem.toString() else ""
            val country = if(spCountry.selectedItemPosition != 0) spCountry.selectedItem.toString() else ""
            val state = if(spCountry.selectedItemPosition == 1) spStateIndia.selectedItem.toString() else ""

            if (name.isEmpty() || email.isEmpty() || mobile.isEmpty() || pass.isEmpty() || gender.isEmpty()) {
                toast("Please fill in all the required fields.")
                return@setOnClickListener
            }

            if (!isOtpVerified) {
                toast("Please verify your mobile number.")
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                toast("Accept the Terms & Conditions to proceed.")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { result ->
                val uid = result.user!!.uid
                val userMap = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "mobile" to mobile,
                    "gender" to gender,
                    "dob" to dob,
                    "district" to district,
                    "tehsil" to tehsil,
                    "address" to address,
                    "education" to if(education == "Other") etEducationOther.text.toString() else education,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                db.collection("Users").document(uid).set(userMap).addOnSuccessListener {
                    toast("Registration successful! Welcome baby! ✨")
                    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.auth_container, LoginFragment()).commit()
                }
            }.addOnFailureListener { e -> toast("Error: ${e.message}") }
        }
    }

    private fun startResendTimer() {
        countDownTimer?.cancel()
        btnSendOtp.isEnabled = false
        btnSendOtp.alpha = 0.5f
        btnResendOtp.visibility = View.GONE
        tvTimer.visibility = View.VISIBLE

        countDownTimer = object : android.os.CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = (millisUntilFinished / 1000)
                tvTimer.text = "Resend OTP in ${String.format("%02d:%02d", sec/60, sec%60)}"
            }
            override fun onFinish() {
                tvTimer.visibility = View.GONE
                btnResendOtp.visibility = View.VISIBLE
                btnSendOtp.isEnabled = true
                btnSendOtp.alpha = 1.0f
            }
        }.start()
    }

    private fun setupTermsSaffron(tv: TextView) {
        val fullText = "I have read and agree to the Terms & Conditions and Privacy Policy."
        val spannable = SpannableString(fullText)
        fun makeLink(start: Int, end: Int, fragment: Fragment): ClickableSpan {
            return object : ClickableSpan() {
                override fun onClick(v: View) {
                    requireActivity().supportFragmentManager.beginTransaction().replace(R.id.auth_container, fragment).addToBackStack(null).commit()
                }
                override fun updateDrawState(ds: TextPaint) {
                    ds.color = ContextCompat.getColor(requireContext(), R.color.saffron)
                    ds.isUnderlineText = true
                }
            }
        }
        spannable.setSpan(makeLink(fullText.indexOf("Terms & Conditions"), fullText.indexOf("Terms & Conditions") + 18, TermsFragment()), fullText.indexOf("Terms & Conditions"), fullText.indexOf("Terms & Conditions") + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(makeLink(fullText.indexOf("Privacy Policy"), fullText.indexOf("Privacy Policy") + 14, PrivacyFragment()), fullText.indexOf("Privacy Policy"), fullText.indexOf("Privacy Policy") + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable
        tv.movementMethod = android.text.method.LinkMovementMethod.getInstance()
    }

    private fun simpleListener(block: (Int) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) = block(pos)
        override fun onNothingSelected(p: AdapterView<*>?) {}
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}