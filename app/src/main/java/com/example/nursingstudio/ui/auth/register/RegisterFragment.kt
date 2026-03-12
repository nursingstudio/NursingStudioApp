package com.example.nursingstudio.ui.auth.register

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.text.Editable
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.utils.AppSettings
import com.example.nursingstudio.AuthActivity
import com.example.nursingstudio.MainActivity
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentRegisterBinding
import com.example.nursingstudio.databinding.LayoutPolicyBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.graphics.toColorInt

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val viewModel: RegisterViewModel by viewModels()
    private var verificationId: String? = null
    private var isOtpVerified = false
    private var countDownTimer: CountDownTimer? = null
    private var lastClickTime: Long = 0
    // Naya Pattern: 8 Chars, 1 Upper, 1 Lower, 1 Number, 1 Special Char
    private val passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setupUniversalErrorCleaner()
        binding.ccp.registerCarrierNumberEditText(binding.etMobile)
        setupAllSpinners()
        setupTermsLink()
        binding.etDob.setOnClickListener { showDatePicker() }
        setupDynamicVisibility()
        setupPasswordStrengthChecker()

        binding.btnResendOtp.setOnClickListener {
            sendOtp()
        }

        binding.btnSendOtp.setOnClickListener {
            if (SystemClock.elapsedRealtime() - lastClickTime < 2000) return@setOnClickListener
            lastClickTime = SystemClock.elapsedRealtime()
            hideKeyboard()
            sendOtp()
        }

        binding.btnVerifyOtp.setOnClickListener {
            hideKeyboard()
            verifyOtpManual()
        }

        binding.tvChangeNumber.setOnClickListener {
            unlockMobileField()
        }

        viewModel.regStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is RegResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnRegister.isEnabled = false
                }

                is RegResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    AppSettings.startNewUserSession(requireContext())
                    AppSettings.triggerVibration(requireContext(), 200)
                    toast("Registration Successful! ✨")
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }

                is RegResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnRegister.isEnabled = true

                    val msg = result.message

                    // 2026 Gold Standard: Professional Mapping
                    val friendlyMsg = when {
                        msg.contains("network", true) || msg.contains("timeout", true) -> getString(R.string.no_internet)
                        msg.contains("email-already-in-use", true) -> "This email is already registered. Please Login."
                        msg.contains("invalid-email", true) -> "Invalid email, Please try again."
                        msg.contains("too-many-requests", true) -> "Server busy! Please try again after some time. ⏳"
                        else -> msg
                    }

                    // Specific field par error dikhana
                    if (msg.contains("email", true)) {
                        binding.tilEmail.isErrorEnabled = true
                        binding.tilEmail.error = friendlyMsg
                        AppSettings.triggerErrorEffect(requireContext(), binding.tilEmail)
                    } else {
                        toast(friendlyMsg)
                        AppSettings.triggerErrorEffect(requireContext(), binding.btnRegister)
                    }
                }
            }
        }

        // OTP Result ko observe karein
        viewModel.otpStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is OtpResult.Loading -> binding.progressBar.visibility = View.VISIBLE
                is OtpResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    isOtpVerified = true
                    handleOtpSuccessUI() // UI handle karne wala function
                }

                is OtpResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    val msg = result.message

                    val friendlyOtpMsg = when {
                        msg.contains("invalid-verification-code", true) -> "Incorrect OTP! Please check again. 🔢"
                        msg.contains("session-expired", true) -> "OTP Expired! Please resend. ⏳"
                        msg.contains("network", true) -> getString(R.string.no_internet)
                        else -> msg
                    }

                    binding.tilOtp.isErrorEnabled = true
                    binding.tilOtp.error = friendlyOtpMsg
                    AppSettings.triggerErrorEffect(requireContext(), binding.tilOtp)
                }
            }
        }

        binding.btnRegister.setOnClickListener {
            hideKeyboard()
            if (SystemClock.elapsedRealtime() - lastClickTime < 2000) return@setOnClickListener
            lastClickTime = SystemClock.elapsedRealtime()

            if (validateInputsSerial()) {
                performRegistration()

            }
        }
        setupButtonEffects()
    }
    private fun setupButtonEffects() {
        with(binding) {
            AppSettings.setPushEffect(btnSendOtp)
            AppSettings.setPushEffect(btnVerifyOtp)
            AppSettings.setPushEffect(btnRegister)
            AppSettings.setPushEffect(btnResendOtp)
        }
    }
    private fun setupUniversalErrorCleaner() {
        val inputMap = mapOf(
            binding.etName to binding.tilName,
            binding.etDob to binding.tilDob,
            binding.etEmail to binding.tilEmail,
            binding.etDistrict to binding.tilDistrict,
            binding.etAddress to binding.tilAddress,
            binding.etPincode to binding.tilPincode,
            binding.etPassword to binding.tilPassword,
            binding.etRegState to binding.tilRegState,
            binding.etRegNumber to binding.tilRegNumber,
            binding.etEducationOther to binding.tilEducationOther,
            binding.etOccupationOther to binding.tilOccupationOther,
            binding.etCountryOther to binding.tilCountryOther,
            binding.etStateOther to binding.tilStateOther,
            binding.etOtp to binding.tilOtp
        )

        inputMap.forEach { (editText, layout) ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        layout.error = null
                        layout.isErrorEnabled = false
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        binding.etMobile.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    binding.layoutMobile.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.edittext_background)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showDatePicker() {
        // 1. TimeZone set karein
        val timeZoneUTC = TimeZone.getTimeZone("UTC")

        // 2. Aaj ki date (Max Limit)
        val today = Calendar.getInstance(timeZoneUTC).timeInMillis

        // 3. 1947 ki date (Min Limit)
        val startCalendar = Calendar.getInstance(timeZoneUTC)
        startCalendar.set(1947, Calendar.JANUARY, 1)
        val startDate = startCalendar.timeInMillis

        // 4. Default Selection: Aaj se 17 saal piche (Nursing Rule)
        val defaultCalendar = Calendar.getInstance(timeZoneUTC)
        defaultCalendar.add(Calendar.YEAR, -17)
        val defaultSelection = defaultCalendar.timeInMillis

        // 5. Constraints banana
        val constraints = CalendarConstraints.Builder()
            .setStart(startDate) // 1947 se pehle block
            .setEnd(today)       // Aaj ke baad block
            .setOpenAt(defaultSelection) // Calendar khulte hi 17 saal piche wala saal dikhayega
            .setValidator(DateValidatorPointBackward.now()) // Future dates block
            .build()

        // 6. DatePicker build karna
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.CustomMaterialCalendar)
            .setTitleText("Select Date of Birth")
            .setCalendarConstraints(constraints)
            .setSelection(defaultSelection)
            .build()

        datePicker.show(childFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { selection ->
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            binding.etDob.setText(sdf.format(Date(selection)))
            binding.tilDob.error = null
            binding.tilDob.isErrorEnabled = false
        }
    }

    private fun validateInputsSerial(): Boolean {
        with(binding) {
            if (etName.text.isNullOrEmpty()) return showError(tilName, "Full Name is required")
            if (spGender.selectedItemPosition == 0) return showError(spGender, "Select Gender")
            if (etDob.text.isNullOrEmpty()) return showError(tilDob, "DOB is required")
            if (spMarital.selectedItemPosition == 0) return showError(
                spMarital,
                "Select Marital Status"
            )
            if (spReligion.selectedItemPosition == 0) return showError(
                spReligion,
                "Select Religion"
            )
            if (spEducation.selectedItemPosition == 0) return showError(
                spEducation,
                "Select Education"
            )
            if (spEducation.selectedItem == "Other" && etEducationOther.text.isNullOrEmpty()) return showError(
                tilEducationOther,
                "Specify Education"
            )
            if (spOccupation.selectedItemPosition == 0) return showError(
                spOccupation,
                "Select Occupation"
            )
            if (spOccupation.selectedItem == "Other" && etOccupationOther.text.isNullOrEmpty()) return showError(
                tilOccupationOther,
                "Specify Occupation"
            )
            if (!isOtpVerified) return showError(layoutMobile, "Mobile verification mandatory")
            if (etEmail.text.isNullOrEmpty() || !Patterns.EMAIL_ADDRESS.matcher(etEmail.text.toString())
                    .matches()
            ) return showError(tilEmail, "Valid Email required")
            if (spCountry.selectedItem == "Bharat (India)" && spStateIndia.selectedItemPosition == 0) return showError(
                spStateIndia,
                "Select State"
            )
            if (spCountry.selectedItem == "Other") {
                if (etCountryOther.text.isNullOrEmpty()) return showError(
                    tilCountryOther,
                    "Enter Country Name"
                )
                if (etStateOther.text.isNullOrEmpty()) return showError(
                    tilStateOther,
                    "Enter State Name"
                )
            } else if (spStateIndia.selectedItemPosition == 0) {
                return showError(spStateIndia, "Select State")
            }
            if (etDistrict.text.isNullOrEmpty()) return showError(tilDistrict, "District required")
            if (etAddress.text.isNullOrEmpty()) return showError(
                tilAddress,
                "Full Address required"
            )
            if (etPincode.text?.length != 6) return showError(
                tilPincode,
                "Valid 6-digit Pincode required"
            )
            // Radio Button Mandatory Check
            if (rgNursingReg.checkedRadioButtonId == -1) return showError(
                rgNursingReg,
                "Please select Nursing Registration status"
            )
            if (rgNursingReg.checkedRadioButtonId == R.id.rbRegYes) {
                if (etRegState.text.isNullOrEmpty()) return showError(
                    tilRegState,
                    "Reg. State required"
                )
                if (etRegNumber.text.isNullOrEmpty()) return showError(
                    tilRegNumber,
                    "Reg. Number required"
                )
            }
            // --- UPDATE: Strong Password Validation (8 Chars + Complexity) ---
            val password = binding.etPassword.text.toString().trim()
            if (!password.matches(passwordPattern.toRegex())) {
                return showError(tilPassword, "Password needs: 8+ chars, 1 Upper, 1 Lower, 1 Number & 1 Special Char")
            }

            if (!cbTerms.isChecked) return showError(cbTerms, "Please accept Terms & Conditions")
        }
        return true
    }

    private fun showError(view: View, message: String): Boolean {
        toast(message)
        AppSettings.triggerErrorEffect(requireContext(), view)

        when (view) {
            is Spinner -> {
                view.background = ContextCompat.getDrawable(requireContext(), R.drawable.spinner_error_bg)
            }
            is TextInputLayout -> {
                view.isErrorEnabled = true
                view.error = message
            }
            is CheckBox -> {
                view.buttonTintList = ColorStateList.valueOf(Color.RED)
            }
        }
        view.requestFocus()

        binding.registrationScrollView.postDelayed({
            if (isAdded && _binding != null) {
                val vTop = view.top
                val parent = view.parent as? View
                val finalTop =
                    if (parent != null && parent !is ScrollView) vTop + parent.top else vTop
                binding.registrationScrollView.smoothScrollTo(0, finalTop - 150)
            }
        }, 100)

        return false
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    private fun unlockMobileField() {
        countDownTimer?.cancel()

        binding.etMobile.isEnabled = true
        binding.ccp.isEnabled = true
        binding.etMobile.alpha = 1.0f

        binding.layoutOtpBox.visibility = View.GONE
        binding.tvChangeNumber.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnSendOtp.visibility = View.VISIBLE
        binding.btnSendOtp.text = getString(R.string.send_otp)

        binding.etOtp.setText("")
        binding.etMobile.requestFocus()
    }

    private fun verifyOtpManual() {
        val code = binding.etOtp.text.toString().trim()
        if (code.length != 6) {
            toast("Enter 6 digit OTP")
            return
        }

        if (verificationId != null) {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
            viewModel.verifyOtp(credential)
        } else {
            toast("Verification ID missing, try resending")
        }
    }

    private fun sendOtp() {
        val mobile = binding.etMobile.text.toString().trim()

        if (mobile.length != 10) {
            toast("Enter 10 digit mobile number")
            binding.etMobile.requestFocus()
            AppSettings.triggerErrorEffect(requireContext(), binding.etMobile)
            return
        }

        hideKeyboard()
        binding.loadingOverlay.visibility = View.VISIBLE

        binding.etMobile.isEnabled = false
        binding.ccp.isEnabled = false
        binding.etMobile.alpha = 0.6f

        (activity as? AuthActivity)?.sendOtp(mobile) { id ->
            binding.loadingOverlay.visibility = View.GONE
            verificationId = id

            binding.layoutOtpBox.visibility = View.VISIBLE
            binding.btnSendOtp.visibility = View.GONE
            binding.tvChangeNumber.visibility = View.VISIBLE

            startTimer()
            toast("OTP Sent Successfully! ✨")
        }
    }

    private fun startTimer() {
        binding.tvTimer.visibility = View.VISIBLE
        binding.tvTimer.text = getString(R.string.resend_in_seconds, 60)
        binding.btnResendOtp.visibility = View.GONE
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(m: Long) {
                val secondsRemaining = (m / 1000).toInt()
                binding.tvTimer.text = getString(R.string.resend_in_seconds, secondsRemaining)
            }
            override fun onFinish() {
                binding.tvTimer.visibility = View.GONE
                binding.btnResendOtp.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun showPolicyBottomSheet(title: String, content: CharSequence) {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val sheetBinding = LayoutPolicyBottomSheetBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(sheetBinding.root)

        sheetBinding.tvPolicyTitle.text = title
        sheetBinding.tvPolicyContent.text = content

        sheetBinding.scrollContainer.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val child = v.getChildAt(0)
            val diff = (child.bottom - (v.height + scrollY))

            if (diff <= 0 && !sheetBinding.btnAccept.isEnabled) {
                sheetBinding.btnAccept.isEnabled = true
                sheetBinding.btnAccept.animate().alpha(1f).setDuration(500).start()
                AppSettings.triggerVibration(requireContext(), 50)
            }
        })

        sheetBinding.btnAccept.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun setupTermsLink() {
        val fullText = "I have read and agree to the Terms & Conditions and Privacy Policy."
        val spannable = SpannableString(fullText)
        val saffron = ContextCompat.getColor(requireContext(), R.color.saffron_dark)

        // 1. Terms & Conditions Block
        val tcClick = object : ClickableSpan() {
            override fun onClick(v: View) {
                val tncHtml = Html.fromHtml(getString(R.string.tnc_content), Html.FROM_HTML_MODE_COMPACT)
                showPolicyBottomSheet("Terms & Conditions", tncHtml)
            }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = saffron
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        // 2. Privacy Policy Block
        val ppClick = object : ClickableSpan() {
            override fun onClick(v: View) {
                // Modern 2026 Standard Way
                val privacyHtml = Html.fromHtml(getString(R.string.privacy_content), Html.FROM_HTML_MODE_COMPACT)
                showPolicyBottomSheet("Privacy Policy", privacyHtml)
            }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = saffron
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

        // ... index calculation ...
        val tcStart = fullText.indexOf("Terms & Conditions")
        val ppStart = fullText.indexOf("Privacy Policy")

        if (tcStart != -1) spannable.setSpan(tcClick, tcStart, tcStart + 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (ppStart != -1) spannable.setSpan(ppClick, ppStart, ppStart + 14, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvTermsLink.apply {
            text = spannable
            movementMethod = LinkMovementMethod.getInstance()
            highlightColor = Color.TRANSPARENT
        }
    }

    private fun setupDynamicVisibility() {
        binding.spEducation.onItemSelectedListener = simpleListener(binding.tilEducationOther) {
            binding.tilEducationOther.visibility = if (it == "Other") View.VISIBLE else View.GONE
        }

        binding.spOccupation.onItemSelectedListener = simpleListener(binding.tilOccupationOther) {
            binding.tilOccupationOther.visibility = if (it == "Other") View.VISIBLE else View.GONE
        }

        binding.rgNursingReg.setOnCheckedChangeListener { _, id ->
            binding.layoutRegDetails.visibility = if (id == R.id.rbRegYes) View.VISIBLE else View.GONE
        }

        // Country Spinner: null pass kar sakte hain kyunki iska koi fix TextInputLayout nahi hai
        binding.spCountry.onItemSelectedListener = simpleListener(null) {
            val isIndia = it == "Bharat (India)"
            binding.spStateIndia.visibility = if (isIndia) View.VISIBLE else View.GONE
            binding.tilCountryOther.visibility = if (isIndia) View.GONE else View.VISIBLE
            binding.tilStateOther.visibility = if (isIndia) View.GONE else View.VISIBLE
        }
    }

    private fun setupAllSpinners() {
        val lists = listOf(
            listOf("Select Gender", "Male", "Female", "Transgender"),
            listOf("Select Marital", "Unmarried", "Married", "Divorced", "Widow/Widower"),
            listOf("Select Religion", "Hindu", "Sikh", "Jain", "Muslim", "Christian", "Buddhist", "Other"),
            listOf("Select Education", "ANM", "GNM", "B.Sc. Nursing", "Post Basic B.Sc. Nursing", "M.Sc. Nursing", "PhD Nursing", "Other"),
            listOf("Select Occupation", "Student", "Preparation (Competitive Exam)", "Private Job (Hospital/Clinic)", "Contractual Job (Govt. Hospital)", "Private Job + Preparation", "Nursing Officer", "CHO", "Nursing Tutor", "SNO", "Ward Incharge", "Metron", "ANS", "DNS", "Nursing Superintendent", "Other"),
            listOf("Bharat (India)", "Other"),
            listOf("Select State/UT", "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland","Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura","Uttar Pradesh","Uttarakhand","West Bengal","Andaman and Nicobar Islands","Chandigarh","Dadra & Nagar Haveli and Daman & Diu","Delhi","Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry")
        )
        val spins = arrayOf(binding.spGender, binding.spMarital, binding.spReligion, binding.spEducation, binding.spOccupation, binding.spCountry, binding.spStateIndia)

        for (i in spins.indices) {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                lists[i]
            )
            spins[i].adapter = adapter

            spins[i].onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (position > 0) {
                        parent?.background = ContextCompat.getDrawable(requireContext(), R.drawable.spinner_bg)
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }
    private fun performRegistration() {
        if (!isNetworkAvailable()) {
            toast(getString(R.string.no_internet))
            AppSettings.triggerErrorEffect(requireContext(), binding.btnRegister)
            return
        }

        val userData = prepareUserData()
        val currentOtp = binding.etOtp.text.toString().trim()

        if (isOtpVerified) {
            val phoneCred = if (verificationId != null) {
                PhoneAuthProvider.getCredential(verificationId!!, currentOtp)
            } else null

            viewModel.startRegistration(
                binding.etEmail.text.toString().trim(),
                binding.etPassword.text.toString().trim(),
                phoneCred!!,
                userData
            )
        } else {
            toast("Verify mobile first")
        }
    }

    private fun prepareUserData(): MutableMap<String, Any> {
        val b = binding
        return mutableMapOf(
            "fullName" to b.etName.text.toString().trim(),
            "gender" to b.spGender.selectedItem.toString(),
            "dob" to b.etDob.text.toString(),
            "maritalStatus" to b.spMarital.selectedItem.toString(),
            "religion" to b.spReligion.selectedItem.toString(),
            "mobile" to b.ccp.fullNumberWithPlus,
            "email" to b.etEmail.text.toString().trim(),
            "education" to if (b.spEducation.selectedItem == "Other") b.etEducationOther.text.toString() else b.spEducation.selectedItem.toString(),
            "occupation" to if (b.spOccupation.selectedItem == "Other") b.etOccupationOther.text.toString() else b.spOccupation.selectedItem.toString(),
            "country" to if (b.spCountry.selectedItem == "Other") b.etCountryOther.text.toString() else b.spCountry.selectedItem.toString(),
            "state" to if (b.spCountry.selectedItem == "Bharat (India)") b.spStateIndia.selectedItem.toString() else b.etStateOther.text.toString(),
            "district" to b.etDistrict.text.toString(),
            "address" to b.etAddress.text.toString(),
            "pincode" to b.etPincode.text.toString(),
            "nursingReg" to (b.rgNursingReg.checkedRadioButtonId == R.id.rbRegYes),
            "regState" to b.etRegState.text.toString(),
            "regNumber" to b.etRegNumber.text.toString(),
            "registeredAt" to FieldValue.serverTimestamp()
        )
    }

    private fun simpleListener(layout: TextInputLayout?, block: (String) -> Unit) =
        object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val selected = p0?.getItemAtPosition(p2).toString()

                if (p2 > 0) {
                    p0?.background = ContextCompat.getDrawable(requireContext(), R.drawable.spinner_bg)
                    layout?.error = null
                    layout?.isErrorEnabled = false
                }
                block(selected)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

    private fun handleOtpSuccessUI() {
        binding.layoutOtpBox.visibility = View.GONE
        binding.tvTimer.visibility = View.GONE
        binding.btnResendOtp.visibility = View.GONE
        countDownTimer?.cancel()
        binding.tvChangeNumber.visibility = View.GONE
        binding.etMobile.isEnabled = false
        binding.ccp.isEnabled = false
        binding.etMobile.alpha = 0.7f

        binding.btnSendOtp.apply {
            visibility = View.VISIBLE
            text = context.getString(R.string.verified)
            isEnabled = false
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        toast("Mobile Verified Successfully! 🎉")
    }
    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    private fun setupPasswordStrengthChecker() {
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val pass = s.toString()
                if (pass.isEmpty()) {
                    binding.layoutPasswordStrength.visibility = View.GONE
                } else {
                    binding.layoutPasswordStrength.visibility = View.VISIBLE
                    updateStrengthIndicator(pass)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateStrengthIndicator(pass: String) {
        var score = 0
        val missingCriteria = mutableListOf<String>()

        // Live Check Logic
        if (pass.length >= 8) score++ else missingCriteria.add("8+ chars")
        if (pass.any { it.isUpperCase() }) score++ else missingCriteria.add("1 Upper Case")
        if (pass.any { it.isLowerCase() }) score++ else missingCriteria.add("1 Lower Case")
        if (pass.any { it.isDigit() }) score++ else missingCriteria.add("1 Number")
        if (pass.any { "@$!%*#?&".contains(it) }) score++ else missingCriteria.add("1 Special Char")

        val color: Int
        val label: String
        val progress: Int

        when {
            score <= 2 -> {
                color = Color.RED
                label = "Weak: Need ${missingCriteria.joinToString(", ")}"
                progress = 25
            }
            score <= 4 -> {
                color = "#FFA500".toColorInt() // Orange
                label = "Medium: Need ${missingCriteria.joinToString(", ")}"
                progress = 60
            }
            score == 5 -> {
                color = "#4CAF50".toColorInt() // Green
                label = "Strong Password"
                progress = 100
            }
            else -> {
                color = Color.GRAY
                label = "Too Short"
                progress = 10
            }
        }

        binding.passwordStrengthProgress.setProgress(progress, true)
        binding.passwordStrengthProgress.setIndicatorColor(color)
        binding.tvStrengthLabel.text = label
        binding.tvStrengthLabel.setTextColor(color)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() { super.onDestroyView(); countDownTimer?.cancel(); _binding = null }
}