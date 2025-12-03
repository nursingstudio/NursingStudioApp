package com.example.nursingstudio

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MyPageFragment : Fragment() {

    companion object {
        private const val PROFILE_PREF = "profile_prefs"
        private const val KEY_PROFILE_IMAGE = "profile_image_base64"
    }

    private lateinit var imgProfile: ImageView
    private lateinit var imgEditPhoto: ImageView

    private var currentBitmap: Bitmap? = null

    // Gallery se image lene ke liye launcher
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                handlePickedImage(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgProfile = view.findViewById(R.id.imgProfile)
        imgEditPhoto = view.findViewById(R.id.imgEditPhoto)

        val tvWelcome = view.findViewById<TextView>(R.id.tvWelcome)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvGender = view.findViewById<TextView>(R.id.tvGender)
        val tvDobWithAge = view.findViewById<TextView>(R.id.tvDobWithAge)
        val tvMarital = view.findViewById<TextView>(R.id.tvMarital)
        val tvReligion = view.findViewById<TextView>(R.id.tvReligion)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvMobile = view.findViewById<TextView>(R.id.tvMobile)
        val tvEducation = view.findViewById<TextView>(R.id.tvEducation)
        val tvOccupation = view.findViewById<TextView>(R.id.tvOccupation)
        val tvAddressFull = view.findViewById<TextView>(R.id.tvAddressFull)
        val tvNursingRegStatus = view.findViewById<TextView>(R.id.tvNursingRegStatus)
        val tvNursingRegDetails = view.findViewById<TextView>(R.id.tvNursingRegDetails)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        val sp = requireContext().getSharedPreferences("session", 0)

        val name = sp.getString("reg_name", "User")
        val gender = sp.getString("reg_gender", "-")
        val dob = sp.getString("reg_dob", "-")
        val marital = sp.getString("reg_marital", "-")
        val religion = sp.getString("reg_religion", "-")
        val email = sp.getString("reg_email", "-")
        val mobile = sp.getString("reg_mobile", "-")
        val education = sp.getString("reg_education", "-")
        val occupation = sp.getString("reg_occupation", "-")
        val country = sp.getString("reg_country", "")
        val state = sp.getString("reg_state", "")
        val district = sp.getString("reg_district", "")
        val tehsil = sp.getString("reg_tehsil", "")
        val address = sp.getString("reg_address", "")
        val pincode = sp.getString("reg_pincode", "")
        val hasReg = sp.getString("reg_has_nursing_reg", "No")
        val regState = sp.getString("reg_nursing_reg_state", "")
        val regNumber = sp.getString("reg_nursing_reg_number", "")

        // Session me saved profile image load karo
        val imageBase64 = sp.getString("reg_profile_image_base64", null)
        if (!imageBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                currentBitmap = bitmap
                imgProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        tvWelcome.text = "Welcome,"
        tvName.text = name
        tvGender.text = gender
        tvMarital.text = marital
        tvReligion.text = religion
        tvEmail.text = email
        tvMobile.text = mobile
        tvEducation.text = education
        tvOccupation.text = occupation

        val fullAddress = "$address, $tehsil, $district, $state, $country - $pincode"
        tvAddressFull.text = fullAddress

        val ageText = calculateAgeText(dob ?: "")
        tvDobWithAge.text = if (ageText.isNotEmpty()) "$dob ($ageText)" else dob

        tvNursingRegStatus.text = hasReg
        tvNursingRegDetails.text =
            if (hasReg == "Yes") "State: $regState | Reg. No: $regNumber" else ""

        btnLogout.setOnClickListener {
            sp.edit().putBoolean("logged_in", false).apply()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            requireActivity().finish()
        }

        // 👉 Photo change – click se gallery open
        val pickAction: (View) -> Unit = {
            pickImageLauncher.launch("image/*")
        }
        imgEditPhoto.setOnClickListener(pickAction)
        imgProfile.setOnClickListener(pickAction)

        // 👉 Long press to rotate (90°)
        imgProfile.setOnLongClickListener {
            rotateCurrentImage()
            true
        }

        // PROFILE_PREF se bhi image load (header ke liye)
        loadProfileImageIfAny()
    }

    private fun handlePickedImage(uri: Uri) {
        try {
            val inputStream: InputStream? =
                requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Optionally thoda resize kar lo (memory ke liye)
                val scaled = scaleDownBitmap(bitmap, 800)
                currentBitmap = scaled

                imgProfile.setImageBitmap(scaled)

                // Session me Base64 save
                val sessionSp = requireContext().getSharedPreferences("session", 0)
                val baos = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                val bytes = baos.toByteArray()
                val encoded = Base64.encodeToString(bytes, Base64.DEFAULT)
                sessionSp.edit()
                    .putString("reg_profile_image_base64", encoded)
                    .apply()

                // Header ke liye PROFILE_PREF me bhi save
                saveProfileImage(scaled)

                // Drawer header turant refresh
                (activity as? MainActivity)?.updateDrawerHeader()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rotateCurrentImage() {
        val bitmap = currentBitmap ?: return

        val matrix = Matrix().apply {
            postRotate(90f)
        }
        val rotated = Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )

        currentBitmap = rotated
        imgProfile.setImageBitmap(rotated)

        // Session + header me rotated image save
        val baos = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()
        val encoded = Base64.encodeToString(bytes, Base64.DEFAULT)

        val sessionSp = requireContext().getSharedPreferences("session", 0)
        sessionSp.edit()
            .putString("reg_profile_image_base64", encoded)
            .apply()

        saveProfileImage(rotated)
        (activity as? MainActivity)?.updateDrawerHeader()
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (ratio > 1f) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val bytes = baos.toByteArray()
        val encoded = Base64.encodeToString(bytes, Base64.DEFAULT)

        val sp = requireContext().getSharedPreferences(PROFILE_PREF, 0)
        sp.edit().putString(KEY_PROFILE_IMAGE, encoded).apply()
    }

    private fun loadProfileImageIfAny() {
        val sp = requireContext().getSharedPreferences(PROFILE_PREF, 0)
        val encoded = sp.getString(KEY_PROFILE_IMAGE, null) ?: return

        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        currentBitmap = bmp
        imgProfile.setImageBitmap(bmp)
    }

    private fun calculateAgeText(dobString: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return ""
            val dobCal = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()

            var years = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - dobCal.get(Calendar.MONTH)

            if (months < 0) {
                years--
                months += 12
            }

            if (years < 0) return ""

            "$years years, $months months"
        } catch (e: Exception) {
            ""
        }
    }
}
