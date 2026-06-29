package com.example.nursingstudio.ui.features.media

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.ActivityPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class PdfViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pdfUrl = intent.getStringExtra("PDF_URL") ?: return
        downloadAndRenderPdf(pdfUrl)
    }

    private fun downloadAndRenderPdf(urlStr: String) {
        lifecycleScope.launch {
            try {
                val localFile = withContext(Dispatchers.IO) {
                    val url = URL(urlStr)
                    val connection = url.openConnection()
                    connection.connect()

                    val cacheFile = File(cacheDir, "temp_nursing_doc.pdf")
                    url.openStream().use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheFile
                }

                binding.pdfProgressBar.visibility = View.GONE

                // 🚀 Jetpack Core Extension Injection Architecture
                val pdfViewerFragment = androidx.pdf.viewer.fragment.PdfViewerFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.pdfContainer, pdfViewerFragment)
                    .commitNow()

                pdfViewerFragment.documentUri = Uri.fromFile(localFile)

            } catch (e: Exception) {
                e.printStackTrace()
                binding.pdfProgressBar.visibility = View.GONE
                Toast.makeText(this@PdfViewerActivity, "Failed to stream digital artifact safely", Toast.LENGTH_LONG).show()
            }
        }
    }
}