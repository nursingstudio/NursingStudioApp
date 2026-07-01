package com.example.nursingstudio.ui.features.media

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentPdfViewerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

// 🚀 2026 INDUSTRY GOLD STANDARD: Converted from AppCompatActivity to pure Fragment lifecycle stream
class PdfViewerFragment : Fragment() {

    private var _binding: FragmentPdfViewerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔒 Safely unwrap parameters passed from MediaContentFragment via Bundle arguments
        val pdfUrl = arguments?.getString("CONTENT_URL") ?: return
        downloadAndRenderPdf(pdfUrl)
    }

    private fun downloadAndRenderPdf(urlStr: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val localFile = withContext(Dispatchers.IO) {
                    val url = URL(urlStr)
                    val connection = url.openConnection()
                    connection.connect()

                    // Securely scope download artifact inside context cache directory space
                    val cacheFile = File(requireContext().cacheDir, "temp_nursing_doc.pdf")
                    url.openStream().use { input ->
                        FileOutputStream(cacheFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    cacheFile
                }

                if (_binding == null) return@launch
                binding.pdfProgressBar.visibility = View.GONE

                // 🚀 2026 CRITICAL CORE FIX: Using childFragmentManager instead of supportFragmentManager to prevent view hierarchies conflict
                val pdfViewerFragment = androidx.pdf.viewer.fragment.PdfViewerFragment()
                childFragmentManager.beginTransaction()
                    .replace(R.id.pdfContainer, pdfViewerFragment)
                    .commitNow()

                pdfViewerFragment.documentUri = Uri.fromFile(localFile)

            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding == null) return@launch
                binding.pdfProgressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to stream digital artifact safely", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}