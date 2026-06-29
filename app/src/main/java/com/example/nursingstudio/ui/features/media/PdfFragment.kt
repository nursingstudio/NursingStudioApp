package com.example.nursingstudio.ui.features.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nursingstudio.databinding.FragmentPdfBinding

class PdfFragment : Fragment() {

    // 🚀 FIXED: Dropped slow findViewById and replaced with clean pre-compiled ViewBinding structures
    private var _binding: FragmentPdfBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPdfLibraryClickListeners()
    }

    private fun setupPdfLibraryClickListeners() {
        binding.cardImportantPdf.setOnClickListener {
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "PDF")
                putExtra("TARGET_CATEGORY", "Important")
            }
            startActivity(intent)
        }

        binding.cardTopicPdf.setOnClickListener {
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "PDF")
                putExtra("TARGET_CATEGORY", "Topic-wise")
            }
            startActivity(intent)
        }

        binding.cardPaperPdf.setOnClickListener {
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "PDF")
                putExtra("TARGET_CATEGORY", "Papers")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}