package com.example.nursingstudio.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@Keep
@IgnoreExtraProperties
data class User(
    @get:PropertyName("uid") @set:PropertyName("uid") var uid: String = "",
    @get:PropertyName("fullName") @set:PropertyName("fullName") var fullName: String = "",
    @get:PropertyName("gender") @set:PropertyName("gender") var gender: String = "",
    @get:PropertyName("dob") @set:PropertyName("dob") var dob: String = "",
    @get:PropertyName("maritalStatus") @set:PropertyName("maritalStatus") var maritalStatus: String = "",
    @get:PropertyName("religion") @set:PropertyName("religion") var religion: String = "",
    @get:PropertyName("education") @set:PropertyName("education") var education: String = "",
    @get:PropertyName("occupation") @set:PropertyName("occupation") var occupation: String = "",
    @get:PropertyName("mobile") @set:PropertyName("mobile") var mobile: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = "",
    @get:PropertyName("country") @set:PropertyName("country") var country: String = "",
    @get:PropertyName("state") @set:PropertyName("state") var state: String = "",
    @get:PropertyName("district") @set:PropertyName("district") var district: String = "",
    @get:PropertyName("address") @set:PropertyName("address") var address: String = "",
    @get:PropertyName("pincode") @set:PropertyName("pincode") var pincode: String = "",
    @get:PropertyName("isNursingRegistered") @set:PropertyName("isNursingRegistered") var isNursingRegistered: Boolean? = null,
    @get:PropertyName("regState") @set:PropertyName("regState") var regState: String? = null,
    @get:PropertyName("regNumber") @set:PropertyName("regNumber") var regNumber: String? = null,
    @get:PropertyName("nursingState") @set:PropertyName("nursingState") var nursingState: String? = null,
    @get:PropertyName("nursingRegNo") @set:PropertyName("nursingRegNo") var nursingRegNo: String? = null,
    @get:PropertyName("profileImageUrl") @set:PropertyName("profileImageUrl") var profileImageUrl: String? = null,
    @get:PropertyName("uniqueNsId") @set:PropertyName("uniqueNsId") var uniqueNsId: String? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Timestamp? = null
)