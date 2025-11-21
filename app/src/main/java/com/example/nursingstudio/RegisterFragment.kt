package com.example.nursingstudio

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Views
        val etName = view.findViewById<EditText>(R.id.etName)
        val spGender = view.findViewById<Spinner>(R.id.spGender)
        val etDob = view.findViewById<EditText>(R.id.etDob)
        val spMarital = view.findViewById<Spinner>(R.id.spMarital)

        val etMobile = view.findViewById<EditText>(R.id.etMobile)
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

        // ----- Spinners data -----

        val genderList = listOf("Select gender", "Male", "Female", "Transgender")

        val maritalList = listOf(
            "Select marital status",
            "Unmarried",
            "Married",
            "Divorced",
            "Widow/Widower"
        )

        val educationList = listOf(
            "Select education",
            "ANM",
            "GNM",
            "B.Sc Nursing",
            "Post Basic B.Sc Nursing",
            "M.Sc Nursing",
            "PhD Nursing",
            "Other"
        )

        val occupationList = listOf(
            "Select occupation",
            "Student",
            "Teacher",
            "Tutor",
            "Nursing Officer",
            "CHO",
            "Private Job",
            "Preparation",
            "Other"
        )

        val countryList = listOf(
            "Select country",
            "Bharat (India)",
            "Other"
        )

        val indianStates = listOf(
            "Select state/UT",
            "Andhra Pradesh","Arunachal Pradesh","Assam","Bihar","Chhattisgarh","Goa",
            "Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka","Kerala",
            "Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland",
            "Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura",
            "Uttar Pradesh","Uttarakhand","West Bengal",
            "Andaman and Nicobar Islands","Chandigarh",
            "Dadra & Nagar Haveli and Daman & Diu","Delhi",
            "Jammu & Kashmir","Ladakh","Lakshadweep","Puducherry"
        )

        // ----- Adapters set -----

        spGender.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genderList)

        spMarital.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, maritalList)

        spEducation.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, educationList)

        spOccupation.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, occupationList)

        spCountry.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, countryList)

        spStateIndia.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, indianStates)

        // ----- Education Other show/hide -----

        spEducation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selected = educationList[position]
                etEducationOther.visibility = if (selected == "Other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ----- Occupation Other show/hide -----

        spOccupation.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selected = occupationList[position]
                etOccupationOther.visibility = if (selected == "Other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ----- Country change -> state UI change -----

        spCountry.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val selected = countryList[position]
                when (selected) {
                    "Bharat (India)" -> {
                        spStateIndia.visibility = View.VISIBLE
                        etStateOther.visibility = View.GONE
                        etCountryOther.visibility = View.GONE
                    }
                    "Other" -> {
                        spStateIndia.visibility = View.GONE
                        etStateOther.visibility = View.VISIBLE
                        etCountryOther.visibility = View.VISIBLE
                    }
                    else -> {
                        spStateIndia.visibility = View.GONE
                        etStateOther.visibility = View.GONE
                        etCountryOther.visibility = View.GONE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ----- Nursing registration yes/no -----

        rgNursingReg.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbRegYes) {
                layoutRegDetails.visibility = View.VISIBLE
            } else if (checkedId == R.id.rbRegNo) {
                layoutRegDetails.visibility = View.GONE
                etRegState.setText("")
                etRegNumber.setText("")
            }
        }

        // ----- DOB auto separator (DD-MM-YYYY) -----

        etDob.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s == null) return
                val digits = s.toString().filter { it.isDigit() }
                val sb = StringBuilder()
                for (i in digits.indices) {
                    sb.append(digits[i])
                    if (i == 1 || i == 3) sb.append('-')
                }
                etDob.removeTextChangedListener(this)
                etDob.setText(sb.toString())
                etDob.setSelection(etDob.text.length)
                etDob.addTextChangedListener(this)
            }
        })

        // ----- Register button -----

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val gender = spGender.selectedItem.toString()
            val dob = etDob.text.toString().trim()
            val marital = spMarital.selectedItem.toString()

            val mob = etMobile.text.toString().trim()
            val email = etEmail.text.toString().trim()

            var edu = spEducation.selectedItem.toString()
            val eduOther = etEducationOther.text.toString().trim()

            var occ = spOccupation.selectedItem.toString()
            val occOther = etOccupationOther.text.toString().trim()

            val countrySelected = spCountry.selectedItem.toString()
            val countryOther = etCountryOther.text.toString().trim()
            val stateIndiaSelected = spStateIndia.selectedItem.toString()
            val stateOther = etStateOther.text.toString().trim()

            val district = etDistrict.text.toString().trim()
            val tehsil = etTehsil.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val pincode = etPincode.text.toString().trim()

            val hasRegYes = rbRegYes.isChecked
            val hasRegNo = rbRegNo.isChecked
            val regState = etRegState.text.toString().trim()
            val regNumber = etRegNumber.text.toString().trim()

            val pass = etPassword.text.toString()

            // ---- VALIDATIONS ----

            if (name.isEmpty()) { showToast("Enter name"); return@setOnClickListener }
            if (gender == "Select gender") { showToast("Select gender"); return@setOnClickListener }

            if (!dob.matches(Regex("\\d{2}-\\d{2}-\\d{4}"))) {
                showToast("Enter DOB in DD-MM-YYYY")
                return@setOnClickListener
            }

            if (marital == "Select marital status") {
                showToast("Select marital status")
                return@setOnClickListener
            }

            if (mob.length != 10) {
                showToast("Enter 10-digit mobile")
                return@setOnClickListener
            }

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast("Enter valid email")
                return@setOnClickListener
            }

            if (edu == "Select education") {
                showToast("Select education")
                return@setOnClickListener
            }
            if (edu == "Other") {
                if (eduOther.isEmpty()) {
                    showToast("Enter your education")
                    return@setOnClickListener
                }
                edu = eduOther
            }

            if (occ == "Select occupation") {
                showToast("Select occupation")
                return@setOnClickListener
            }
            if (occ == "Other") {
                if (occOther.isEmpty()) {
                    showToast("Enter your occupation")
                    return@setOnClickListener
                }
                occ = occOther
            }

            if (countrySelected == "Select country") {
                showToast("Select country")
                return@setOnClickListener
            }

            var finalCountry = ""
            var finalState = ""

            if (countrySelected == "Bharat (India)") {
                finalCountry = "Bharat"
                if (stateIndiaSelected == "Select state/UT") {
                    showToast("Select state/UT")
                    return@setOnClickListener
                }
                finalState = stateIndiaSelected
            } else if (countrySelected == "Other") {
                if (countryOther.isEmpty()) {
                    showToast("Enter country name")
                    return@setOnClickListener
                }
                if (stateOther.isEmpty()) {
                    showToast("Enter state/region")
                    return@setOnClickListener
                }
                finalCountry = countryOther
                finalState = stateOther
            }

            if (district.isEmpty()) { showToast("Enter district"); return@setOnClickListener }
            if (tehsil.isEmpty()) { showToast("Enter tehsil"); return@setOnClickListener }
            if (address.isEmpty()) { showToast("Enter local address"); return@setOnClickListener }
            if (pincode.length != 6) {
                showToast("Enter valid 6-digit pincode")
                return@setOnClickListener
            }

            if (!hasRegYes && !hasRegNo) {
                showToast("Select nursing registration yes/no")
                return@setOnClickListener
            }

            var regStatus = "No"
            var finalRegState = ""
            var finalRegNumber = ""

            if (hasRegYes) {
                regStatus = "Yes"
                if (regState.isEmpty()) {
                    showToast("Enter registration state")
                    return@setOnClickListener
                }
                if (regNumber.isEmpty()) {
                    showToast("Enter registration number")
                    return@setOnClickListener
                }
                finalRegState = regState
                finalRegNumber = regNumber
            }

            if (pass.length < 4) {
                showToast("Password too short")
                return@setOnClickListener
            }

            // ---- SAVE ----

            val sp = requireContext().getSharedPreferences("session", 0)
            sp.edit()
                .putString("reg_name", name)
                .putString("reg_gender", gender)
                .putString("reg_dob", dob)
                .putString("reg_marital", marital)
                .putString("reg_mobile", mob)
                .putString("reg_email", email)
                .putString("reg_password", pass)
                .putString("reg_education", edu)
                .putString("reg_occupation", occ)
                .putString("reg_country", finalCountry)
                .putString("reg_state", finalState)
                .putString("reg_district", district)
                .putString("reg_tehsil", tehsil)
                .putString("reg_address", address)
                .putString("reg_pincode", pincode)
                .putString("reg_has_nursing_reg", regStatus)
                .putString("reg_nursing_reg_state", finalRegState)
                .putString("reg_nursing_reg_number", finalRegNumber)
                .apply()

            showToast("Registered! Now login.")

            // ---- Turant Login screen dikhana ----
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.auth_container, LoginFragment())
                .commit()
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}
