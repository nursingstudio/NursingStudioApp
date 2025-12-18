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
import androidx.appcompat.app.AlertDialog
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

    // CAMERA
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { handleNewBitmap(it) }
        }

    // GALLERY
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    val input: InputStream? =
                        requireContext().contentResolver.openInputStream(it)
                    val bmp = BitmapFactory.decodeStream(input)
                    input?.close()
                    bmp?.let { handleNewBitmap(it) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // -------- Views --------
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

        // -------- Session --------
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

        // -------- Profile Image --------
        sp.getString("reg_profile_image_base64", null)?.let {
            try {
                val bytes = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                currentBitmap = bitmap
                imgProfile.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // -------- Set Data --------
        tvWelcome.text = "Welcome,"
        tvName.text = name
        tvGender.text = gender
        tvMarital.text = marital
        tvReligion.text = religion
        tvEmail.text = email
        tvMobile.text = mobile
        tvEducation.text = education
        tvOccupation.text = occupation

        tvAddressFull.text =
            "$address, $tehsil, $district, $state, $country - $pincode"

        val ageText = calculateAgeText(dob ?: "")
        tvDobWithAge.text =
            if (ageText.isNotEmpty()) "$dob ($ageText)" else dob

        tvNursingRegStatus.text = hasReg
        tvNursingRegDetails.text =
            if (hasReg == "Yes") "State: $regState | Reg. No: $regNumber" else ""

        // -------- Logout --------
        btnLogout.setOnClickListener {
            sp.edit().putBoolean("logged_in", false).apply()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            requireActivity().finish()
        }

        // -------- Photo Actions --------
        val pickAction: (View) -> Unit = { showImageSourceDialog() }
        imgEditPhoto.setOnClickListener(pickAction)
        imgProfile.setOnClickListener(pickAction)

        imgProfile.setOnLongClickListener {
            rotateCurrentImage()
            true
        }

        loadProfileImageIfAny()

        // ===============================
        // 🔥 STUDY PROGRESS (FINAL FIX)
        // ===============================
        val tvProgressTests = view.findViewById<TextView>(R.id.tvProgressTests)
        val tvProgressPdfs = view.findViewById<TextView>(R.id.tvProgressPdfs)
        val tvProgressVideos = view.findViewById<TextView>(R.id.tvProgressVideos)

        val tests = ProgressManager.get(requireContext(), "test_attempted")
        val pdfs = ProgressManager.get(requireContext(), "pdf_opened")
        val videos = ProgressManager.get(requireContext(), "video_watched")

        tvProgressTests.text = "Tests Attempted: $tests"
        tvProgressPdfs.text = "PDFs Opened: $pdfs"
        tvProgressVideos.text = "Videos Watched: $videos"
    }

    // ---------------- Helpers ----------------

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Select photo")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                if (which == 0) cameraLauncher.launch(null)
                else galleryLauncher.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleNewBitmap(bitmap: Bitmap) {
        val scaled = scaleDownBitmap(bitmap, 800)
        currentBitmap = scaled
        imgProfile.setImageBitmap(scaled)

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

        requireContext().getSharedPreferences("session", 0)
            .edit().putString("reg_profile_image_base64", encoded).apply()

        saveProfileImage(scaled)
        (activity as? MainActivity)?.updateDrawerHeader()
    }

    private fun rotateCurrentImage() {
        val bitmap = currentBitmap ?: return
        val matrix = Matrix().apply { postRotate(90f) }
        handleNewBitmap(
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        )
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val (w, h) =
            if (ratio > 1) maxSize to (maxSize / ratio).toInt()
            else (maxSize * ratio).toInt() to maxSize
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        requireContext().getSharedPreferences(PROFILE_PREF, 0)
            .edit().putString(KEY_PROFILE_IMAGE, encoded).apply()
    }

    private fun loadProfileImageIfAny() {
        val encoded =
            requireContext().getSharedPreferences(PROFILE_PREF, 0)
                .getString(KEY_PROFILE_IMAGE, null) ?: return
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
            if (months < 0) { years--; months += 12 }
            "$years years, $months months"
        } catch (e: Exception) { "" }
    }
}
