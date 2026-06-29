package com.example.nursingstudio.ui.features.media

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nursingstudio.data.model.MediaItemModel
import com.example.nursingstudio.databinding.ActivityMediaContentBinding
import com.google.firebase.firestore.FirebaseFirestore

class MediaContentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMediaContentBinding
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMediaContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvMediaContent.layoutManager = LinearLayoutManager(this)

        val targetType = intent.getStringExtra("TARGET_TYPE") ?: "PDF"
        val targetCategory = intent.getStringExtra("TARGET_CATEGORY") ?: "Important"

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

                binding.loadingBar.visibility = View.GONE
                binding.rvMediaContent.visibility = View.VISIBLE

                binding.rvMediaContent.adapter = ContentAdapter(list) { selectedItem ->
                    if (selectedItem.type == "PDF") {
                        val intent = Intent(this, PdfViewerActivity::class.java).apply {
                            putExtra("PDF_URL", selectedItem.fileUrl)
                        }
                        startActivity(intent)
                    } else {
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            putExtra("VIDEO_URL", selectedItem.fileUrl)
                        }
                        startActivity(intent)
                    }
                }
            }
            .addOnFailureListener { e ->
                binding.loadingBar.visibility = View.GONE
                // 🚀 2026 Gold Standard: Advanced Error Telemetry Log for strict architectural tracing
                android.util.Log.e("FIRESTORE_FAULT", "Fail reason: ${e.localizedMessage}")
                Toast.makeText(this, "Cloud Sync Error: Ensure collections exist and rules are published.", Toast.LENGTH_LONG).show()
            }
    }
}