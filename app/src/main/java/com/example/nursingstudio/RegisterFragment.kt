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
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.auth.register.RegisterViewModel
import com.example.nursingstudio.auth.register.RegResult
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import com.hbb20.CountryCodePicker
import java.util.concurrent.TimeUnit

class RegisterFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private val viewModel: RegisterViewModel by viewModels()

    private var verificationId: String? = null
    private var isOtpVerified = false
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

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
            val digitsOnly = etMobile.text.toString().trim().replace("\\s".toRegex(), "")
            if (digitsOnly.length != 10) { toast("Please enter a valid 10-digit number"); return@setOnClickListener }

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
                    override fun onVerificationFailed(e: FirebaseException) { toast("Failed: ${e.localizedMessage}") }
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) { etOtp.setText(p0.smsCode) }
                }).build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtp.text.toString().trim()
            if (code.isEmpty()) return@setOnClickListener
            isOtpVerified = true
            layoutOtpBox.visibility = View.GONE
            btnSendOtp.visibility = View.VISIBLE
            btnSendOtp.text = "Verified ✅"; btnSendOtp.isEnabled = false
            toast("Verified!")
        }

        setupTermsSaffron(tvTermsLink)

        // --- MVVM OBSERVER ---
        viewModel.regStatus.observe(viewLifecycleOwner) { result ->
            when(result) {
                is RegResult.Loading -> { btnRegister.isEnabled = false; toast("Creating Account...") }
                is RegResult.Success -> {
                    toast("Welcome! ✨ Account Created.")
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
                is RegResult.Error -> { btnRegister.isEnabled = true; toast("Error: ${result.message}") }
            }
        }

        // --- REGISTER CLICK ---
        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val currentOtp = etOtp.text.toString().trim()

            if (name.isEmpty()) { etName.error = "Name required"; etName.requestFocus(); return@setOnClickListener }
            if (spGender.selectedItemPosition == 0) { toast("Select Gender"); return@setOnClickListener }
            if (spDay.selectedItemPosition == 0 || spMonth.selectedItemPosition == 0 || spYear.selectedItemPosition == 0) { toast("Complete DOB required"); return@setOnClickListener }
            if (!isOtpVerified) { toast("Verify OTP first!"); return@setOnClickListener }
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.error = "Invalid Email"; return@setOnClickListener }
            if (pass.length < 6) { etPassword.error = "Min 6 chars"; return@setOnClickListener }
            if (!cbTerms.isChecked) { toast("Accept Terms"); return@setOnClickListener }

            val userData = mutableMapOf<String, Any>(
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

            if (verificationId != null) {
                val phoneCred = PhoneAuthProvider.getCredential(verificationId!!, currentOtp)
                viewModel.startRegistration(email, pass, phoneCred, userData)
            } else {
                toast("Please verify mobile number")
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); countDownTimer?.cancel() }

    private fun setupAllSpinners(vararg s: Spinner) {
        val lists = listOf(
            listOf("Select Gender", "Male", "Female", "Transgender"),
            (1..31).map { it.toString() }.toMutableList().apply { add(0, "DD") },
            listOf("MM", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12"),
            (2026 downTo 1970).map { it.toString() }.toMutableList().apply { add(0, "YYYY") },
            listOf("Select Marital", "Unmarried", "Married", "Divorced", "Widow/Widower"),
            listOf("Select Religion", "Hindu", "Sikh", "Jain", "Muslim", "Christian", "Buddhist", "Other"),
            listOf("Select Education", "ANM", "GNM", "B.Sc. Nursing", "Post Basic B.Sc. Nursing", "M.Sc. Nursing", "PhD Nursing", "Other"),
            listOf("Select Occupation", "Student", "Preparation (Competitive Exam)", "Private Job (Hospital/Clinic)", "Contractual Job (Govt. Hospital)", "Private Job + Preparation", "Nursing Officer", "CHO", "Nursing Tutor", "SNO", "Ward Incharge", "Metron", "ANS", "DNS", "Nursing Superintendent", "Other"),
            listOf("Bharat (India)", "Other"),
            listOf("Select State/UT", "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Andaman and Nicobar Islands","Chandigarh","Dadra & Nagar Haveli and Daman & Diu","Delhi","Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry")
        )
        for (i in s.indices) {
            s[i].adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, lists[i])
        }
    }

    private fun startTimer(tv: TextView, btn: TextView) {
        tv.visibility = View.VISIBLE; btn.visibility = View.GONE
        countDownTimer?.cancel()
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
        val tcStart = fullText.indexOf("Terms & Conditions")
        val ppStart = fullText.indexOf("Privacy Policy")
        if (tcStart != -1) spannable.setSpan(tcClick, tcStart, tcStart + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (ppStart != -1) spannable.setSpan(ppClick, ppStart, ppStart + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        tv.text = spannable; tv.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun openFragment(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.auth_container, fragment)
            .addToBackStack(null).commit()
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()
}