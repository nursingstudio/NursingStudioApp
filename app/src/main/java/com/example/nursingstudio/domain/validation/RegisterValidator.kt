package com.example.nursingstudio.domain.validation

import android.util.Patterns
import com.example.nursingstudio.data.model.User

object RegisterValidator {

    fun validate(user: User, pass: String): ValidationResult {


        // 🚀 2026 Gold Standard: Modular Number Sanitization
        val cleanNumber = user.mobile.replace("\\s".toRegex(), "").replace("-", "") // Spaces/Dashes hatao
        val pureDigits = cleanNumber.filter { it.isDigit() }
        val isIndian = cleanNumber.startsWith("+91")

        // Correct logic for 10-digit check
        val isTenDigit = (isIndian && pureDigits.removePrefix("91").length == 10) ||
                (!isIndian && pureDigits.length >= 7 && pureDigits.length <= 15)

        return when {
            // 1. Personal Details
            user.fullName.isBlank() -> ValidationResult.Error("Full Name is required", "name")
            user.gender == "Select Gender" -> ValidationResult.Error("Please select Gender", "gender")
            user.dob.isBlank() -> ValidationResult.Error("Date of Birth is required", "dob")
            user.maritalStatus == "Select Marital" -> ValidationResult.Error("Select Marital Status", "marital")
            user.religion == "Select Religion" -> ValidationResult.Error("Select Religion", "religion")

            // 2. Education & Occupation
            user.education == "Select Education" -> ValidationResult.Error("Select Education", "edu")
            user.education.isBlank() -> ValidationResult.Error("Please specify your education", "edu_other")
            user.occupation == "Select Occupation" -> ValidationResult.Error("Select Occupation", "occ")
            user.occupation.isBlank() -> ValidationResult.Error("Please specify your occupation", "occ_other")

            // 3. Contact Details

            cleanNumber.isBlank() -> ValidationResult.Error("Mobile Number is required", "mobile")
            !isTenDigit -> ValidationResult.Error("Enter a valid mobile number", "mobile")

            user.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(user.email).matches() ->
                ValidationResult.Error("Valid Email required", "email")

            // 4. Address Details (Refined Logic)
            // ✅ Fixed: Checks if manual entry is too short
            (user.country != "Bharat (India)" && user.country.length < 2) -> ValidationResult.Error("Enter valid Country Name", "country_other")
            user.country.isBlank() -> ValidationResult.Error("Country is required", "country")

            // State specific validation
            (user.country == "Bharat (India)" && user.state == "Select State/UT") -> ValidationResult.Error("Select State", "state")
            (user.country != "Bharat (India)" && user.state.isBlank()) -> ValidationResult.Error("Enter State Name", "state_other")

            user.district.isBlank() -> ValidationResult.Error("District required", "district")
            user.address.isBlank() -> ValidationResult.Error("Full Address required", "address")
            user.pincode.length != 6 -> ValidationResult.Error("Valid 6-digit Pincode required", "pincode")

            // 5. Nursing Registration (SERIAL POSITION FIXED)
            user.isNursingRegistered == null -> ValidationResult.Error("Please select Nursing Registration status", "is_reg")

            // Agar Yes select kiya hai tabhi niche wale check chale
            user.isNursingRegistered && user.regState.isNullOrBlank() -> ValidationResult.Error("Registration State required", "reg_state")
            user.isNursingRegistered && user.regNumber.isNullOrBlank() -> ValidationResult.Error("Registration Number required", "reg_no")

            // 6. Password Strength
            !pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$".toRegex()) ->
                ValidationResult.Error("Password must be 8+ chars with Upper, Lower, Number & Special char", "pass")

            else -> ValidationResult.Success
        }
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val msg: String, val field: String) : ValidationResult()
    }
}