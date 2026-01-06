package com.example.nursingstudio.profile

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.nursingstudio.MainActivity
import com.example.nursingstudio.R
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var imgProfile: ImageView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imgProfile = view.findViewById(R.id.imgProfile)
        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val btnLogout = view.findViewById<Button>(R.id.btnLogout)

        // --- Step 1: Observe ViewModel (Asli MVVM yahi hai) ---
        viewModel.userData.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                // 1. Name aur Email set karo
                tvName.text = data["fullName"]?.toString() ?: "User"
                tvEmail.text = data["email"]?.toString() ?: "-"

                // 2. Gender Row (id: rowGender)
                val rowGender = view?.findViewById<View>(R.id.rowGender)
                rowGender?.findViewById<TextView>(R.id.tvLabel)?.text = "GENDER"
                rowGender?.findViewById<TextView>(R.id.tvValue)?.text = data["gender"]?.toString() ?: "-"

                // 3. DOB Row (id: rowDob)
                val rowDob = view?.findViewById<View>(R.id.rowDob)
                rowDob?.findViewById<TextView>(R.id.tvLabel)?.text = "DATE OF BIRTH"
                val dob = data["dob"]?.toString() ?: ""
                rowDob?.findViewById<TextView>(R.id.tvValue)?.text = "$dob ${calculateAge(dob)}"

                // 4. Mobile Row (id: rowMobile)
                val rowMobile = view?.findViewById<View>(R.id.rowMobile)
                rowMobile?.findViewById<TextView>(R.id.tvLabel)?.text = "MOBILE"
                rowMobile?.findViewById<TextView>(R.id.tvValue)?.text = data["mobile"]?.toString() ?: "-"

                // 5. Education Row (id: rowEducation)
                val rowEdu = view?.findViewById<View>(R.id.rowEducation)
                rowEdu?.findViewById<TextView>(R.id.tvLabel)?.text = "EDUCATION"
                rowEdu?.findViewById<TextView>(R.id.tvValue)?.text = data["education"]?.toString() ?: "-"

                // 6. Address Row (id: rowAddress)
                val rowAddr = view?.findViewById<View>(R.id.rowAddress)
                rowAddr?.findViewById<TextView>(R.id.tvLabel)?.text = "FULL ADDRESS"
                val fullAddr = "${data["address"]}, ${data["district"]}, ${data["state"]} - ${data["pincode"]}"
                rowAddr?.findViewById<TextView>(R.id.tvValue)?.text = fullAddr

                // 7. Nursing Status
                val isReg = data["nursingReg"] as? Boolean ?: false
                view?.findViewById<TextView>(R.id.tvNursingStatus)?.text = "Nursing Registered: ${if(isReg) "Yes" else "No"}"
            }
        }

        // --- Step 2: Fetch Data ---
        viewModel.fetchProfile()

        // --- Step 3: Photo Logic ---
        loadProfileImage()
        view.findViewById<View>(R.id.imgEditPhoto).setOnClickListener { showImageDialog() }

        // --- Step 4: Logout ---
        btnLogout.setOnClickListener {
            auth.signOut()
            // Clear activity stack and go to login
            activity?.finish()
        }
    }

    private fun calculateAge(dobString: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dobString) ?: return ""
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { time = birthDate }
            var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--
            "($age Years)"
        } catch (e: Exception) { "" }
    }

    // --- Image Pickers ---
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let { saveAndRefresh(it) }
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val inputStream = requireContext().contentResolver.openInputStream(it)
            val bmp = BitmapFactory.decodeStream(inputStream)
            bmp?.let { saveAndRefresh(it) }
        }
    }

    private fun showImageDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Update Profile Picture")
            .setItems(options) { _, which ->
                if (which == 0) cameraLauncher.launch(null) else galleryLauncher.launch("image/*")
            }.show()
    }

    private fun saveAndRefresh(bitmap: Bitmap) {
        imgProfile.setImageBitmap(bitmap)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val encoded = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)

        // Save locally for quick load
        requireContext().getSharedPreferences("profile_prefs", 0).edit()
            .putString("profile_image_base64", encoded).apply()

        // Optional: Update in Firestore too (ViewModel ke through)
        val update = mapOf("profileImage" to encoded)
        viewModel.updateProfile(update)
    }

    private fun loadProfileImage() {
        val encoded = requireContext().getSharedPreferences("profile_prefs", 0)
            .getString("profile_image_base64", null) ?: return
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        imgProfile.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }
}