package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentVideoBinding

class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVideoLibraryClickListeners()
    }

    private fun setupVideoLibraryClickListeners() {
        binding.cardImportantVideo.setOnClickListener {
            // 🚀 2026 INDUSTRY GOLD STANDARD: Safe Key-Value Bundle Infrastructure
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "VIDEO")
                putString("TARGET_CATEGORY", "Important")
            }
            // 🚀 2026 INDUSTRY GOLD STANDARD: Context-safe explicit fragment navigation controller invoke
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                R.id.nav_media_content,
                bundle
            )
        }

        binding.cardTopicVideo.setOnClickListener {
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "VIDEO")
                putString("TARGET_CATEGORY", "Topic-wise")
            }
            // 🚀 2026 INDUSTRY GOLD STANDARD: Context-safe explicit fragment navigation controller invoke
            androidx.navigation.fragment.NavHostFragment.findNavController(this).navigate(
                R.id.nav_media_content,
                bundle
            )
        }

        binding.cardPlaylistVideo.setOnClickListener {
            val bundle = Bundle().apply {
                putString("TARGET_TYPE", "VIDEO")
                putString("TARGET_CATEGORY", "Playlists")
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