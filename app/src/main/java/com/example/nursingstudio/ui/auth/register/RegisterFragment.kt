package com.example.nursingstudio.ui.auth.register

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Editable
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.User
import com.example.nursingstudio.databinding.FragmentRegisterBinding
import com.example.nursingstudio.databinding.LayoutPolicyBottomSheetBinding
import com.example.nursingstudio.domain.validation.RegisterValidator
import com.example.nursingstudio.ui.main.MainActivity
import com.example.nursingstudio.utils.AppSettings
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()
    private var lastClickTime: Long = 0

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

        setupUniversalErrorCleaner()
        binding.ccp.registerCarrierNumberEditText(binding.etMobile)
        setupAllSpinners()
        setupTermsLink()
        binding.etDob.setOnClickListener { showDatePicker() }
        setupDynamicVisibility()
        setupPasswordStrengthChecker()
        setupButtonEffects()
        observeViewModel()
        setupButtonEffects()

        binding.btnRegister.setOnClickListener {
            hideKeyboard()
            if (SystemClock.elapsedRealtime() - lastClickTime < 2000) return@setOnClickListener
            lastClickTime = SystemClock.elapsedRealtime()

            if (validateInputsSerial()) {
                performRegistration()

            }
        }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // ✅ Professional Loading Overlay Control
                    binding.loadingOverlay.visibility = if (state is RegisterViewModel.RegisterState.Loading) View.VISIBLE else View.GONE
                    binding.btnRegister.isEnabled = state !is RegisterViewModel.RegisterState.Loading

                    when (state) {
                        is RegisterViewModel.RegisterState.Success -> {
                            AppSettings.triggerVibration(requireContext(), 200)
                            toast("Account Created! ✨")
                            startActivity(Intent(requireContext(), MainActivity::class.java))
                            requireActivity().finish()
                            }

                        is RegisterViewModel.RegisterState.Error -> {
                            handleErrorMessage(state.message)
                        }
                        is RegisterViewModel.RegisterState.Loading -> { }
                        is RegisterViewModel.RegisterState.Idle -> { }
                    }
                }
            }
        }
    }

    private fun handleErrorMessage(msg: String) {
        val friendlyMsg = when {
            msg.contains("network", true) || msg.contains("timeout", true) -> getString(R.string.no_internet)
            msg.contains("email-already-in-use", true) -> "This email is already registered. Please login."
            msg.contains("invalid-verification-code", true) -> "Incorrect OTP! Please check and try again."
            msg.contains("invalid-email", true) -> "Invalid email, Please try with correct email."
            msg.contains("too-many-requests", true) -> "Server busy! Please try again later. ⏳"
            msg.contains("session-expired", true) -> "OTP Expired! Please resend. ⏳"
            else -> msg
        }
        toast(friendlyMsg)
        AppSettings.triggerErrorEffect(requireContext(), binding.btnRegister)

        viewModel.resetState()
    }

    private fun setupButtonEffects() {
        with(binding) {
            AppSettings.setPushEffect(btnRegister)
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
            binding.etStateOther to binding.tilStateOther
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
                    binding.tilMobile.error = null
                    binding.tilMobile.isErrorEnabled = false
                    }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun showDatePicker() {
        val timeZoneUTC = TimeZone.getTimeZone("UTC")

        val today = Calendar.getInstance(timeZoneUTC).timeInMillis

        val startCalendar = Calendar.getInstance(timeZoneUTC)
        startCalendar.set(1947, Calendar.JANUARY, 1)
        val startDate = startCalendar.timeInMillis

        // Default Selection: Aaj se 17 saal piche (Nursing Rule)
        val defaultCalendar = Calendar.getInstance(timeZoneUTC)
        defaultCalendar.add(Calendar.YEAR, -17)
        val defaultSelection = defaultCalendar.timeInMillis

        val constraints = CalendarConstraints.Builder()
            .setStart(startDate)
            .setEnd(today)
            .setOpenAt(defaultSelection)
            .setValidator(DateValidatorPointBackward.now())
            .build()

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
        val user = prepareUserData()
        val pass = binding.etPassword.text.toString().trim()
        return when (val result = RegisterValidator.validate(user, pass)) {
            is RegisterValidator.ValidationResult.Success -> {
                if (!binding.cbTerms.isChecked) {
                    showError(binding.cbTerms, "Please accept Terms & Conditions")
                    false
                } else true
            }
            is RegisterValidator.ValidationResult.Error -> {
                val viewToFocus: View = when(result.field) {
                    "name" -> binding.tilName
                    "gender" -> binding.spGender
                    "dob" -> binding.tilDob
                    "marital" -> binding.spMarital
                    "religion" -> binding.spReligion
                    "edu" -> binding.spEducation
                    "edu_other" -> binding.tilEducationOther
                    "occ" -> binding.spOccupation
                    "occ_other" -> binding.tilOccupationOther
                    "mobile" -> binding.tilMobile
                    "email" -> binding.tilEmail
                    "country" -> binding.spCountry
                    "country_other" -> binding.tilCountryOther
                    "state" -> binding.spStateIndia
                    "state_other" -> binding.tilStateOther
                    "district" -> binding.tilDistrict
                    "address" -> binding.tilAddress
                    "pincode" -> binding.tilPincode
                    "is_reg" -> binding.rgNursingReg
                    "reg_state" -> binding.tilRegState
                    "reg_no" -> binding.tilRegNumber
                    "pass" -> binding.tilPassword
                    else -> binding.root
                }
                showError(viewToFocus, result.msg)
                false
            }
        }
    }

    private fun showError(view: View, message: String): Boolean {
        AppSettings.triggerErrorEffect(requireContext(), view)

        when (view) {
            is TextInputLayout -> {
                view.error = message
                view.requestFocus()
            }
            is Spinner -> {
                toast(message)
                view.requestFocus()
            }
            is CheckBox -> {
                view.buttonTintList = ColorStateList.valueOf(Color.RED)
                toast(message)
            }
        }

        // Professional Smooth Scroll
        binding.registrationScrollView.smoothScrollTo(0, view.top - 200)
        return false
    }

    private fun hideKeyboard() {
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
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

        val ppClick = object : ClickableSpan() {
            override fun onClick(v: View) {
                val privacyHtml = Html.fromHtml(getString(R.string.privacy_content), Html.FROM_HTML_MODE_COMPACT)
                showPolicyBottomSheet("Privacy Policy", privacyHtml)
            }
            override fun updateDrawState(ds: TextPaint) {
                ds.color = saffron
                ds.isUnderlineText = false
                ds.isFakeBoldText = true
            }
        }

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
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // ✅ Direct Registration (No OTP Check)
        viewModel.startRegistration(email, password, userData)
    }

    private fun prepareUserData(): User {
        val b = binding
        return User(
            fullName = b.etName.text.toString().trim(),
            gender = b.spGender.selectedItem.toString(),
            dob = b.etDob.text.toString(),
            maritalStatus = b.spMarital.selectedItem.toString(),
            religion = b.spReligion.selectedItem.toString(),
            education = if (b.spEducation.selectedItem == "Other") b.etEducationOther.text.toString() else b.spEducation.selectedItem.toString(),
            occupation = if (b.spOccupation.selectedItem == "Other") b.etOccupationOther.text.toString() else b.spOccupation.selectedItem.toString(),
            mobile = b.ccp.fullNumberWithPlus,
            email = b.etEmail.text.toString().trim(),
            country = if (b.spCountry.selectedItem == "Other") b.etCountryOther.text.toString() else b.spCountry.selectedItem.toString(),
            state = if (b.spCountry.selectedItem == "Bharat (India)") b.spStateIndia.selectedItem.toString() else b.etStateOther.text.toString(),
            district = b.etDistrict.text.toString(),
            address = b.etAddress.text.toString(),
            pincode = b.etPincode.text.toString(),
            isNursingRegistered = (b.rgNursingReg.checkedRadioButtonId == R.id.rbRegYes),
            regState = b.etRegState.text.toString().takeIf { it.isNotEmpty() },
            regNumber = b.etRegNumber.text.toString().takeIf { it.isNotEmpty() }
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
        val criteria = mapOf(
            "8+ chars" to (pass.length >= 8),
            "1 Upper" to pass.any { it.isUpperCase() },
            "1 Lower" to pass.any { it.isLowerCase() },
            "1 Number" to pass.any { it.isDigit() },
            "1 Special" to pass.any { "@$!%*#?&".contains(it) }
        )

        val score = criteria.values.count { it }
        val missing = criteria.filter { !it.value }.keys.joinToString(", ")

        val (color, label, progress) = when {
            score <= 2 -> Triple(Color.RED, "Weak: Need $missing", 25)
            score <= 4 -> Triple("#FFA500".toColorInt(), "Medium: Need $missing", 65)
            else -> Triple("#4CAF50".toColorInt(), "Strong Password ✨", 100)
        }

        binding.passwordStrengthProgress.setProgress(progress, true)
        binding.passwordStrengthProgress.setIndicatorColor(color)
        binding.tvStrengthLabel.apply {
            text = label
            setTextColor(color)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
                )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.resetState()
        _binding = null
    }
}