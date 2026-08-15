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
import com.example.nursingstudio.databinding.FragmentMediaContentBinding // ✅ FIX: Correct layout mapping
import com.google.firebase.firestore.FirebaseFirestore

// 🚀 2026 INDUSTRY GOLD STANDARD: Clean Embedded Architecture Type-Safe Layout Container
class MediaContentFragment : Fragment() {

    private var _binding: FragmentMediaContentBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ✅ FIX: Inflation type matched with correct view hierarchy
        _binding = FragmentMediaContentBinding.inflate(inflater, container, false)
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
        val collectionName = if (type.equals("PDF", ignoreCase = true)) "pdfs" else "videos"

        firestore.collection(collectionName)
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                val list = mutableListOf<MediaItemModel>()
                for (doc in result) {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val fileUrl = if (type.equals("PDF", ignoreCase = true)) {
                        doc.getString("pdfUrl") ?: doc.getString("fileUrl") ?: ""
                    } else {
                        doc.getString("videoUrl") ?: doc.getString("fileUrl") ?: ""
                    }

                    // 🔐 2026 Multi-Key Extraction Strategy (Checks pdfType, videoType, accessType, batchType)
                    val rawAccessType = doc.getString("accessType")
                        ?: doc.getString("pdfType")
                        ?: doc.getString("videoType")
                        ?: doc.getString("batchType")
                        ?: if (doc.getBoolean("isLocked") == true || doc.getBoolean("isFree") == false) "PAID" else "FREE"

                    val streamType = doc.getString("streamType") ?: "RECORDED"

                    list.add(
                        MediaItemModel(
                            id = id,
                            title = title,
                            fileUrl = fileUrl,
                            type = type,
                            accessType = rawAccessType,
                            videoType = rawAccessType,
                            pdfType = rawAccessType,
                            batchType = rawAccessType,
                            streamType = streamType
                        )
                    )
                }

                if (_binding == null) return@addOnSuccessListener

                binding.loadingBar.visibility = View.GONE
                binding.rvMediaContent.visibility = View.VISIBLE

                binding.rvMediaContent.adapter = ContentAdapter(list) { selectedItem ->
                    if (selectedItem.computedIsLocked) {
                        Toast.makeText(
                            requireContext(),
                            "This content is locked. Purchase premium subscription to unlock.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val bundle = Bundle().apply {
                            putString("CONTENT_URL", selectedItem.fileUrl)
                            putString("VIDEO_TYPE", selectedItem.accessType)
                            putString("STREAM_TYPE", selectedItem.streamType)
                        }

                        if (selectedItem.type.equals("PDF", ignoreCase = true)) {
                            findNavController().navigate(R.id.action_media_to_pdf, bundle)
                        } else {
                            findNavController().navigate(R.id.action_media_to_video, bundle)
                        }
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