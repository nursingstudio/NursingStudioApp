package com.example.nursingstudio.domain.validation

import android.util.Patterns
import com.example.nursingstudio.data.model.User

object RegisterValidator {

    fun validate(user: User, pass: String): ValidationResult {
        return when {
            // Personal Info
            user.fullName.isEmpty() -> ValidationResult.Error("Full Name is required", "name")
            user.gender == "Select Gender" -> ValidationResult.Error("Please select Gender", "gender")
            user.dob.isEmpty() -> ValidationResult.Error("Date of Birth is required", "dob")
            user.maritalStatus == "Select Marital" -> ValidationResult.Error("Select Marital Status", "marital")
            user.religion == "Select Religion" -> ValidationResult.Error("Select Religion", "religion")

            // Education & Occupation
            user.education == "Select Education" -> ValidationResult.Error("Select Education", "edu")
            user.education.isEmpty() -> ValidationResult.Error("Specify Education", "edu_other")
            user.occupation == "Select Occupation" -> ValidationResult.Error("Select Occupation", "occ")
            user.occupation.isEmpty() -> ValidationResult.Error("Specify Occupation", "occ_other")

            // Contact & Address
            // ✅ Mobile Validation (Removing +91 or code to check pure 10 digits if needed)
            user.mobile.isEmpty() -> ValidationResult.Error("Mobile Number is required", "mobile")
            // Assuming CCP provides full number, we check if it's long enough
            user.mobile.length < 10 -> ValidationResult.Error("Enter a valid 10-digit number", "mobile")
            user.email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(user.email).matches() ->
                ValidationResult.Error("Valid Email required", "email")
            user.country.isEmpty() -> ValidationResult.Error("Enter Country Name", "country")
            user.state.isEmpty() || user.state == "Select State/UT" -> ValidationResult.Error("Select State", "state")
            user.district.isEmpty() -> ValidationResult.Error("District required", "district")
            user.address.isEmpty() -> ValidationResult.Error("Full Address required", "address")
            user.pincode.length != 6 -> ValidationResult.Error("Valid 6-digit Pincode required", "pincode")

            // Nursing Registration (Yes/No Logic)
            user.isNursingRegistered && user.regState.isNullOrEmpty() ->
                ValidationResult.Error("Registration State required", "reg_state")
            user.isNursingRegistered && user.regNumber.isNullOrEmpty() ->
                ValidationResult.Error("Registration Number required", "reg_no")

            // Password
            !pass.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$".toRegex()) ->
                ValidationResult.Error("Password must be strong (8+ chars, Upper, Lower, Number, Special)", "pass")

            else -> ValidationResult.Success
        }
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val msg: String, val field: String) : ValidationResult()
    }
}