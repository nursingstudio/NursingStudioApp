package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.MediaItemModel
import com.example.nursingstudio.databinding.ActivityMediaContentBinding
import com.google.firebase.firestore.FirebaseFirestore

// 🚀 2026 INDUSTRY GOLD STANDARD: Migrated from Activity to Fragment to preserve Main Toolbar & BottomNav layout shell
class MediaContentFragment : Fragment() {

    private var _binding: ActivityMediaContentBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMediaContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvMediaContent.layoutManager = LinearLayoutManager(requireContext())

        // 🔒 Refactored safely from Intent to Jetpack Bundle Arguments pipeline
        val targetType = arguments?.getString("TARGET_TYPE") ?: "PDF"
        val targetCategory = arguments?.getString("TARGET_CATEGORY") ?: "Important"

        fetchDataFromFirestoreCloud(targetType, targetCategory)
    }

    private fun fetchDataFromFirestoreCloud(type: String, category: String) {
        val collectionName = if (type == "PDF") "pdfs" else "videos"

        firestore.collection(collectionName)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<MediaItemModel>()
                for (doc in result) {
                    val id = doc.getString("id") ?: ""
                    val title = doc.getString("title") ?: ""
                    val fileUrl = if (type == "PDF") doc.getString("pdfUrl") ?: "" else doc.getString("videoUrl") ?: ""
                    list.add(MediaItemModel(id, title, fileUrl, type))
                }

                if (_binding == null) return@addOnSuccessListener

                binding.loadingBar.visibility = View.GONE
                binding.rvMediaContent.visibility = View.VISIBLE

                binding.rvMediaContent.adapter = ContentAdapter(list) { selectedItem ->
                    val bundle = Bundle().apply {
                        putString("CONTENT_URL", selectedItem.fileUrl)
                    }

                    // 🚀 Jetpack Dynamic Nav Component redirection mapping over same host viewport
                    if (selectedItem.type == "PDF") {
                        findNavController().navigate(R.id.action_media_to_pdf, bundle)
                    } else {
                        findNavController().navigate(R.id.action_media_to_video, bundle)
                    }
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.loadingBar.visibility = View.GONE
                android.util.Log.e("FIRESTORE_FAULT", "Fail reason: ${e.localizedMessage}")
                Toast.makeText(requireContext(), "Cloud Sync Error: Verify Firestore Rules Context.", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}