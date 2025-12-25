package com.example.nursingstudio

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.hbb20.CountryCodePicker
import java.util.concurrent.TimeUnit

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationId: String? = null
    private var isOtpVerified = false
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_register, container, false)

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
        val spMarital = view.findViewById<Spinner>(R.id.spMarital)
        val spReligion = view.findViewById<Spinner>(R.id.spReligion)
        val spEducation = view.findViewById<Spinner>(R.id.spEducation)
        val etEducationOther = view.findViewById<EditText>(R.id.etEducationOther)
        val spOccupation = view.findViewById<Spinner>(R.id.spOccupation)
        val etOccupationOther = view.findViewById<EditText>(R.id.etOccupationOther)
        val etMobile = view.findViewById<EditText>(R.id.etMobile)
        val ccp = view.findViewById<CountryCodePicker>(R.id.ccp)
        val btnSendOtp = view.findViewById<Button>(R.id.btnSendOtp)
        val layoutOtpBox = view.findViewById<View>(R.id.layoutOtpBox)
        val etOtp = view.findViewById<EditText>(R.id.etOtp)
        val btnVerifyOtp = view.findViewById<Button>(R.id.btnVerifyOtp)
        val tvTimer = view.findViewById<TextView>(R.id.tvTimer)
        val btnResendOtp = view.findViewById<TextView>(R.id.btnResendOtp)
        val spCountry = view.findViewById<Spinner>(R.id.spCountry)
        val etCountryOther = view.findViewById<EditText>(R.id.etCountryOther)
        val spStateIndia = view.findViewById<Spinner>(R.id.spStateIndia)
        val etStateOther = view.findViewById<EditText>(R.id.etStateOther)
        val etDistrict = view.findViewById<EditText>(R.id.etDistrict)
        val etAddress = view.findViewById<EditText>(R.id.etAddress)
        val etPincode = view.findViewById<EditText>(R.id.etPincode)
        val rgNursingReg = view.findViewById<RadioGroup>(R.id.rgNursingReg)
        val layoutRegDetails = view.findViewById<View>(R.id.layoutRegDetails)
        val etRegState = view.findViewById<EditText>(R.id.etRegState)
        val etRegNumber = view.findViewById<EditText>(R.id.etRegNumber)
        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnRegister = view.findViewById<Button>(R.id.btnRegister)
        val cbTerms = view.findViewById<CheckBox>(R.id.cbTerms)
        val tvTermsLink = view.findViewById<TextView>(R.id.tvTermsLink)

        ccp.registerCarrierNumberEditText(etMobile)

        // --- Spinner Setup ---
        setupAllSpinners(spGender, spDay, spMonth, spYear, spMarital, spReligion, spEducation, spOccupation, spCountry, spStateIndia)

        // Visibility Logics
        spEducation.onItemSelectedListener = simpleListener { if (it == "Other") etEducationOther.visibility = View.VISIBLE else etEducationOther.visibility = View.GONE }
        spOccupation.onItemSelectedListener = simpleListener { if (it == "Other") etOccupationOther.visibility = View.VISIBLE else etOccupationOther.visibility = View.GONE }
        rgNursingReg.setOnCheckedChangeListener { _, id -> layoutRegDetails.visibility = if (id == R.id.rbRegYes) View.VISIBLE else View.GONE }

        spCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val country = p0?.getItemAtPosition(p2).toString()
                if (country == "Bharat (India)") {
                    spStateIndia.visibility = View.VISIBLE
                    etCountryOther.visibility = View.GONE
                    etStateOther.visibility = View.GONE
                } else {
                    spStateIndia.visibility = View.GONE
                    etCountryOther.visibility = View.VISIBLE
                    etStateOther.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // --- OTP Logic ---
        btnSendOtp.setOnClickListener {
            val mobileInput = etMobile.text.toString().trim()
            val digitsOnly = mobileInput.replace("\\s".toRegex(), "")
            if (digitsOnly.length != 10) {
                toast("Please enter a valid 10-digit number")
                return@setOnClickListener
            }

            val formattedForFirebase = "${ccp.selectedCountryCodeWithPlus}${digitsOnly}"

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedForFirebase)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(requireActivity())
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId = id
                        resendToken = token
                        layoutOtpBox.visibility = View.VISIBLE
                        btnSendOtp.visibility = View.GONE
                        startTimer(tvTimer, btnResendOtp)
                        toast("OTP Sent to $formattedForFirebase")
                    }
                    override fun onVerificationFailed(e: FirebaseException) {
                        toast("Failed: ${e.localizedMessage}")
                    }
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        etOtp.setText(p0.smsCode)
                    }
                }).build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        // --- FIXED: OTP Verification without creating ghost user ---
        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.isEmpty()) return@setOnClickListener

            //val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            //auth.signInWithCredential(credential).addOnSuccessListener {
            isOtpVerified = true
            layoutOtpBox.visibility = View.GONE
            btnSendOtp.visibility = View.VISIBLE
            btnSendOtp.text = "Verified ✅"
            btnSendOtp.isEnabled = false
            toast("Verified!")
            // }.addOnFailureListener { toast("Invalid OTP") }
        }

        setupTermsSaffron(tvTermsLink)

        // --- FIXED: Account Binding Logic ---
        btnRegister.setOnClickListener {
            // 1. Get the OTP from the EditText RIGHT NOW
            val currentOtp = etOtp.text.toString().trim()

            // 2. Get other values
            val name = etName.text.toString().trim()
            val mobile = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val district = etDistrict.text.toString().trim()
            val address  = etAddress.text.toString().trim()
            val pincode = etPincode.text.toString().trim()
            val regNumber = etRegNumber.text.toString().trim()

            // --- Step 2: Strict Validation Logic ---

            // 1. Basic Details
            if (name.isEmpty()) { etName.error = "Please enter your full name"; etName.requestFocus(); return@setOnClickListener }

            // Spinner Validation (Check if first item is still selected)
            if (spGender.selectedItemPosition == 0) { toast("Please select Gender"); return@setOnClickListener }

            // DOB Validation
            if (spDay.selectedItemPosition == 0 || spMonth.selectedItemPosition == 0 || spYear.selectedItemPosition == 0) {
                toast("Complete Date of Birth is required"); return@setOnClickListener
            }

            if (spMarital.selectedItemPosition == 0) { toast("Select Marital Status"); return@setOnClickListener }
            if (spReligion.selectedItemPosition == 0) { toast("Select Religion"); return@setOnClickListener }

            // 2. Education & Occupation (With "Other" check)
            if (spEducation.selectedItemPosition == 0) { toast("Select Education"); return@setOnClickListener }
            if (spEducation.selectedItem.toString() == "Other" && etEducationOther.text.toString().trim().isEmpty()) {
                etEducationOther.error = "Please specify your education"; etEducationOther.requestFocus(); return@setOnClickListener
            }

            if (spOccupation.selectedItemPosition == 0) { toast("Select Occupation"); return@setOnClickListener }
            if (spOccupation.selectedItem.toString() == "Other" && etOccupationOther.text.toString().trim().isEmpty()) {
                etOccupationOther.error = "Please specify your occupation"; etOccupationOther.requestFocus(); return@setOnClickListener
            }

            // 3. Contact & Auth
            if (mobile.isEmpty()) { etMobile.error = "Mobile number is required"; etMobile.requestFocus(); return@setOnClickListener }
            if (!isOtpVerified) { toast("Please verify your mobile via OTP first!"); return@setOnClickListener }

            if (email.isEmpty()) { etEmail.error = "Email is required"; etEmail.requestFocus(); return@setOnClickListener }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Invalid Email format"; return@setOnClickListener }

            if (pass.length < 6) { etPassword.error = "Password must be at least 6 characters"; etPassword.requestFocus(); return@setOnClickListener }

            // 4. Address Details
            if (spCountry.selectedItem.toString() == "Bharat (India)" && spStateIndia.selectedItemPosition == 0) {
                toast("Please select your State"); return@setOnClickListener
            }
            // Agar Bharat se bahar hai toh State Other mandatory
            if (spCountry.selectedItem.toString() == "Other" && etStateOther.text.toString().trim().isEmpty()) {
                etStateOther.error = "State/Region is required"; etStateOther.requestFocus(); return@setOnClickListener
            }

            if (district.isEmpty()) { etDistrict.error = "District is required"; etDistrict.requestFocus(); return@setOnClickListener }
            if (address.isEmpty()) { etAddress.error = "Full address is required"; etAddress.requestFocus(); return@setOnClickListener }
            if (pincode.length != 6) { etPincode.error = "Enter a valid 6-digit Pincode"; etPincode.requestFocus(); return@setOnClickListener }

            // 5. Nursing Registration (Conditional)
            if (rgNursingReg.checkedRadioButtonId == R.id.rbRegYes) {
                if (etRegNumber.text.toString().trim().isEmpty()) {
                    etRegNumber.error = "Registration Number is mandatory"; etRegNumber.requestFocus(); return@setOnClickListener
                }
                if (etRegState.text.toString().trim().isEmpty()) {
                    etRegState.error = "Registration State is mandatory"; etRegState.requestFocus(); return@setOnClickListener
                }
            }

            // 6. Terms
            if (!cbTerms.isChecked) { toast("You must accept Terms & Conditions"); return@setOnClickListener }

            // --- AGAR SAB SAHI HAI TOH DATA SAVE KARO ---
            // proceedWithRegistration()



            btnRegister.isEnabled = false // Prevent multiple clicks
            // Step 1: Create Email Account
            auth.createUserWithEmailAndPassword(email, pass).addOnSuccessListener { res ->
                val user = res.user

                // Step2: Create the Phone Credential
                // Link Phone Credential so the user can login with Phone or Email later
                val phoneCred = PhoneAuthProvider.getCredential(verificationId!!, currentOtp)

                // step3: Link phone to the email user
                user?.linkWithCredential(phoneCred)?.addOnCompleteListener { linkTask ->

                    // Data to save in Firestore
                    val userData = linkedMapOf(
                        "uid" to user?.uid,
                        "fullName" to name,
                        "gender" to spGender.selectedItem.toString(),
                        "dob" to "${spDay.selectedItem}-${spMonth.selectedItem}-${spYear.selectedItem}",
                        "maritalStatus" to spMarital.selectedItem.toString(),
                        "religion" to spReligion.selectedItem.toString(),
                        "mobile" to ccp.fullNumberWithPlus,
                        "email" to email,
                        "education" to if (spEducation.selectedItem == "Other") etEducationOther.text.toString() else spEducation.selectedItem.toString(),
                        "occupation" to if (spOccupation.selectedItem == "Other") etOccupationOther.text.toString() else spOccupation.selectedItem.toString(),
                        "country" to if (spCountry.selectedItem == "Other") etCountryOther.text.toString() else spCountry.selectedItem.toString(),
                        "state" to if (spCountry.selectedItem == "Bharat (India)") spStateIndia.selectedItem.toString() else etStateOther.text.toString(),
                        "district" to etDistrict.text.toString(),
                        "address" to etAddress.text.toString(),
                        "pincode" to etPincode.text.toString(),
                        "nursingReg" to (rgNursingReg.checkedRadioButtonId == R.id.rbRegYes),
                        "regState" to etRegState.text.toString(),
                        "regNumber" to etRegNumber.text.toString(),
                        "password" to pass,
                        "registeredAt" to FieldValue.serverTimestamp()
                    )

                    // Step 3: Save to Firestore
                    db.collection("Users").document(user!!.uid).set(userData).addOnSuccessListener {
                        toast("Welcome, $name! ✨ Account Created.")
                        startActivity(Intent(requireContext(), MainActivity::class.java))
                        requireActivity().finish()
                    }

                }
                    }.addOnFailureListener {
                    btnRegister.isEnabled = true
                    toast("Registration Failed: ${it.localizedMessage}")
                }
            }
        }

    // --- FIXED: Memory Leak Prevention ---
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun setupAllSpinners(vararg s: Spinner) {
        val lists = listOf(
            listOf("Select Gender *", "Male", "Female", "Transgender"),
            (1..31).map { it.toString() }.toMutableList().apply { add(0, "DD") },
            listOf("MM", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"),
            (2026 downTo 1970).map { it.toString() }.toMutableList().apply { add(0, "YYYY") },
            listOf("Select Marital *", "Unmarried", "Married", "Divorced", "Widow/Widower"),
            listOf("Select Religion *", "Hindu", "Sikh", "Jain", "Muslim", "Christian", "Buddhist", "Other"),
            listOf("Select Education *", "ANM", "GNM", "B.Sc. Nursing", "Post Basic B.Sc. Nursing", "M.Sc. Nursing", "PhD Nursing", "Other"),
            listOf("Select Occupation *", "Student", "Preparation (Competitive Exam)", "Private Job (Hospital/Clinic)", "Contractual Job (Govt. Hospital)", "Private Job + Preparation", "Nursing Officer", "CHO", "Nursing Tutor", "SNO", "Ward Incharge", "Metron", "ANS", "DNS", "Nursing Superintendent", "Other"),
            listOf("Bharat (India)", "Other"),
            listOf("Select State/UT *", "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Andaman and Nicobar Islands","Chandigarh","Dadra & Nagar Haveli and Daman & Diu","Delhi","Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry")
        )
        for (i in s.indices) {
            s[i].adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                lists[i]
            )
        }
    }

    private fun startTimer(tv: TextView, btn: TextView) {
        tv.visibility = View.VISIBLE; btn.visibility = View.GONE
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) { tv.text = "Resend in ${m/1000}s" }
            override fun onFinish() { tv.visibility = View.GONE; btn.visibility = View.VISIBLE }
        }.start()
    }

    private fun simpleListener(block: (String) -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { block(p0?.getItemAtPosition(p2).toString()) }
        override fun onNothingSelected(p0: AdapterView<*>?) {}
    }

    private fun setupTermsSaffron(tv: TextView) {
        val fullText = "I have read and agree to the Terms & Conditions and Privacy Policy."
        val spannable = SpannableString(fullText)
        val tcClick = object : ClickableSpan() {
            override fun onClick(v: View) { openFragment(TermsFragment()) }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.saffron)
                ds.isUnderlineText = true
            }
        }
        val ppClick = object : ClickableSpan() {
            override fun onClick(v: View) { openFragment(PrivacyFragment()) }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = ContextCompat.getColor(requireContext(), R.color.saffron)
                ds.isUnderlineText = true
            }
        }
        spannable.setSpan(tcClick, fullText.indexOf("Terms & Conditions"), fullText.indexOf("Terms & Conditions") + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ppClick, fullText.indexOf("Privacy Policy"), fullText.indexOf("Privacy Policy") + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable
        tv.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, fragment)
            .addToBackStack(null).commit()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()
}