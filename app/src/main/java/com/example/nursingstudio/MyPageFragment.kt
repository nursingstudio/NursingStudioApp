package com.example.nursingstudio

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MyPageFragment : Fragment() {

    private lateinit var imgProfile: ImageView
    private lateinit var imgEditPhoto: ImageView
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Tumhara XML load ho raha hai
        return inflater.inflate(R.layout.fragment_mypage, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- View Mapping (Register ki IDs ke hisaab se) ---
        imgProfile = view.findViewById(R.id.imgProfile)
        imgEditPhoto = view.findViewById(R.id.imgEditPhoto)
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

        // --- Firestore Fetching Logic ---
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("Users").document(userId).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    // RegisterFragment ke Keys ke saath Matching
                    tvName.text = doc.getString("name") ?: "_"
                    tvGender.text = doc.getString("gender") ?: "-"
                    tvEmail.text = doc.getString("email") ?: "-"
                    tvMobile.text = doc.getString("mobile") ?: "-"
                    tvMarital.text = doc.getString("maritalStatus") ?: "-"
                    tvReligion.text = doc.getString("religion") ?: "-"
                    tvEducation.text = doc.getString("education") ?: "-"
                    tvOccupation.text = doc.getString("occupation") ?: "-"

                    // Address merge logic (Tehsil + District + State + Pin)
                    val addr = doc.getString("address") ?: ""
                    val teh = doc.getString("tehsil") ?: ""
                    val dist = doc.getString("district") ?: ""
                    val st = doc.getString("state") ?: ""
                    val pin = doc.getString("pincode") ?: ""
                    tvAddressFull.text = "$addr, $teh, $dist, $st - $pin"

                    // Age Calculation
                    val dob = doc.getString("dob") ?: ""
                    val ageResult = calculateAge(dob)
                    tvDobWithAge.text = if (ageResult.isNotEmpty()) "$dob ($ageResult)" else dob

                    // Registration Details
                    val isReg = doc.getString("nursingRegStatus") ?: "No"
                    tvNursingRegStatus.text = "Registered: $isReg"
                    if (isReg == "Yes") {
                        val regSt = doc.getString("regState") ?: ""
                        val regNum = doc.getString("regNumber") ?: ""
                        tvNursingRegDetails.text = "State: $regSt | Reg No: $regNum"
                        tvNursingRegDetails.visibility = View.VISIBLE
                    } else {
                        tvNursingRegDetails.visibility = View.GONE
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Data fetch failed!", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Photo & Logout Logic ---
        loadProfileImage()
        val clickAction = View.OnClickListener { showImageDialog() }
        imgEditPhoto.setOnClickListener(clickAction)
        imgProfile.setOnClickListener(clickAction)

        btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun calculateAge(dobString: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return ""
            val dobCal = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()
            var years = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - dobCal.get(Calendar.MONTH)
            if (months < 0) { years--; months += 12 }
            if (years > 0) "${years}y ${months}m" else "${months}m"
        } catch (e: Exception) { "" }
    }

    // --- Image Handling launchers ---
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { saveAndRefresh(it) }
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val stream = requireContext().contentResolver.openInputStream(it)
            val bmp = BitmapFactory.decodeStream(stream)
            bmp?.let { saveAndRefresh(it) }
        }
    }

    private fun showImageDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Update Profile Photo")
            .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                if (which == 0) cameraLauncher.launch(null) else galleryLauncher.launch("image/*")
            }.show()
    }

    private fun saveAndRefresh(bitmap: Bitmap) {
        imgProfile.setImageBitmap(bitmap)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        requireContext().getSharedPreferences("profile_prefs", 0).edit()
            .putString("profile_image_base64", encoded).apply()
        (activity as? MainActivity)?.updateDrawerHeader()
    }

    private fun loadProfileImage() {
        val encoded = requireContext().getSharedPreferences("profile_prefs", 0)
            .getString("profile_image_base64", null) ?: return
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        imgProfile.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }
}