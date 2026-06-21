package com.example.nursingstudio.ui.features.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nursingstudio.databinding.FragmentVideoBinding

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Modernized Media Pipeline Fragment
 * Fully optimized mapping tracking bounds, dropping old legacy findViewById layers entirely.
 */
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

        // Initialize pure synchronized action layers via binding parameters
        setupVideoLibraryClickListeners()
    }

    /**
     * 🚀 SANITIZED VIEW BINDING LISTENERS: Modernized up to top-tier enterprise guidelines
     */
    private fun setupVideoLibraryClickListeners() {

        // 1. Important Videos Card Configuration (Triggers your NORCET 2020 Live Stream Player)
        binding.cardImportantVideo.setOnClickListener {
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                putExtra("EXTRA_VIDEO_ID", "l1WjKspBxX8")
            }
            startActivity(intent)
        }

        // 2. Topic-wise Videos Modernized Binding Context
        binding.cardTopicVideo.setOnClickListener {
            Toast.makeText(requireContext(), "Topic-wise videos coming soon.", Toast.LENGTH_SHORT).show()
        }

        // 3. YouTube Playlists Modernized Binding Context
        binding.cardPlaylistVideo.setOnClickListener {
            Toast.makeText(requireContext(), "Opening YouTube playlists soon.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Prevents memory context leaks over UI dynamic adjustments
        _binding = null
    }
}