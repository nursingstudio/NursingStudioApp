package com.example.nursingstudio.domain.validation

import android.util.Patterns
import com.example.nursingstudio.data.model.User

object RegisterValidator {

    fun validate(user: User, pass: String): ValidationResult {

        // 🚀 2026 Gold Standard: Modular Number Sanitization
        val cleanNumber = user.mobile.replace("\\s".toRegex(), "").replace("-", "")
        val pureDigits = cleanNumber.filter { it.isDigit() }
        val isIndian = cleanNumber.startsWith("+91")

        // Correct logic for 10-digit check
        val isTenDigit = (isIndian && pureDigits.removePrefix("91").length == 10) ||
                (!isIndian && pureDigits.length >= 7 && pureDigits.length <= 15)

        return when {
            // ==========================================
            // 1. PERSONAL DETAILS
            // ==========================================
            user.fullName.isBlank() -> ValidationResult.Error("Full Name is required", "name")

            // 🚀 LINE EXPANSION: Strict compliance validation for gender dropdown selection
            user.gender.isBlank() || user.gender.equals("Select Gender", ignoreCase = true) ->
                ValidationResult.Error("Please select Gender", "gender")

            user.dob.isBlank() -> ValidationResult.Error("Date of Birth is required", "dob")

            user.maritalStatus.isBlank() || user.maritalStatus.equals("Select Marital", ignoreCase = true) ->
                ValidationResult.Error("Select Marital Status", "marital")

            user.religion.isBlank() || user.religion.equals("Select Religion", ignoreCase = true) ->
                ValidationResult.Error("Select Religion", "religion")

            // ==========================================
            // 2. EDUCATION & OCCUPATION
            // ==========================================
            // ==========================================
            // 2. EDUCATION & OCCUPATION (Strict Hierarchy Branching)
            // ==========================================
            // 🚀 2026 GOLD STANDARD: Explicit Context-Aware State Validation for Dropdown vs Manual Input
            (user.education.equals("Select Education", ignoreCase = true)) ->
                ValidationResult.Error("Select Education", "edu")

            (user.education.isBlank() || user.education.equals("Other", ignoreCase = true) || user.education.trim().length < 2) ->
                ValidationResult.Error("Please specify your education", "edu_other")

// 🚀 2026 GOLD STANDARD: Explicit Context-Aware State Validation for Dropdown vs Manual Input
            (user.occupation.equals("Select Occupation", ignoreCase = true)) ->
                ValidationResult.Error("Select Occupation", "occ")

            (user.occupation.isBlank() || user.occupation.equals("Other", ignoreCase = true) || user.occupation.trim().length < 2) ->
                ValidationResult.Error("Please specify your occupation", "occ_other")

            // ==========================================
            // 3. CONTACT DETAILS
            // ==========================================
            cleanNumber.isBlank() -> ValidationResult.Error("Mobile Number is required", "mobile")
            !isTenDigit -> ValidationResult.Error("Enter a valid mobile number", "mobile")

            user.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(user.email).matches() ->
                ValidationResult.Error("Valid Email required", "email")

            // ==========================================
            // 4. ADDRESS DETAILS (2026 Dynamic Logic Fix)
            // ==========================================
            // Condition A: Bharat selected but State dropdown is still on default prompt
            (user.country.equals("Bharat (India)", ignoreCase = true) &&
                    (user.state.isBlank() || user.state.startsWith("Select State", ignoreCase = true))) ->
                ValidationResult.Error("Select State", "state")

            // Condition B: 'Other' selected but Custom Country text input is left blank/short
            (!user.country.equals("Bharat (India)", ignoreCase = true) && user.country.trim().length < 2) ->
                ValidationResult.Error("Enter valid Country Name", "country_other")

            // Condition C: 'Other' selected, Country is specified, but Custom State text input is left blank/short
            (!user.country.equals("Bharat (India)", ignoreCase = true) && user.state.trim().length < 2) ->
                ValidationResult.Error("Enter State Name", "state_other")

            user.district.isBlank() -> ValidationResult.Error("District required", "district")
            user.address.isBlank() -> ValidationResult.Error("Full Address required", "address")

            // 🚀 FIXED: Secure Pincode digit filter validation against alphanumeric layout entry spoofing
            user.pincode.length != 6 || !user.pincode.all { it.isDigit() } ->
                ValidationResult.Error("Valid 6-digit Pincode required", "pincode")

            // ==========================================
            // 5. NURSING REGISTRATION
            // ==========================================
            user.isNursingRegistered == null -> ValidationResult.Error("Please select Nursing Registration status", "is_reg")

            (user.isNursingRegistered == true && user.regState.isNullOrBlank()) ->
                ValidationResult.Error("Registration State required", "reg_state")

            (user.isNursingRegistered == true && user.regNumber.isNullOrBlank()) ->
                ValidationResult.Error("Registration Number required", "reg_no")

            // ==========================================
            // 6. PASSWORD STRENGTH
            // ==========================================
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