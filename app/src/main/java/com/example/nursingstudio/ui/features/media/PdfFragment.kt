package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
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
            // 🚀 2026 GOLD STANDARD: Safe Navigation args transaction over unified shell
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "PDF")
                putString("TARGET_CATEGORY", "Important")
            }

            // 🚀 2026 INDUSTRY GOLD STANDARD: Context-safe explicit fragment navigation controller invoke
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                R.id.nav_media_content,
                bundle
            )
        }

        binding.cardTopicPdf.setOnClickListener {
            // 🚀 2026 GOLD STANDARD: Safe Navigation args transaction over unified shell
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "PDF")
                putString("TARGET_CATEGORY", "Topic-wise")
            }

// 🚀 2026 INDUSTRY GOLD STANDARD: Context-safe explicit fragment navigation controller invoke
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                R.id.nav_media_content,
                bundle
            )
        }

        binding.cardPaperPdf.setOnClickListener {
            // 🚀 2026 GOLD STANDARD: Safe Navigation args transaction over unified shell
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "PDF")
                putString("TARGET_CATEGORY", "Papers")
            }

// 🚀 2026 INDUSTRY GOLD STANDARD: Context-safe explicit fragment navigation controller invoke
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                R.id.nav_media_content,
                bundle
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}