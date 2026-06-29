package com.example.nursingstudio.ui.features.media

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "VIDEO")
                putExtra("TARGET_CATEGORY", "Important")
            }
            startActivity(intent)
        }

        binding.cardTopicVideo.setOnClickListener {
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "VIDEO")
                putExtra("TARGET_CATEGORY", "Topic-wise")
            }
            startActivity(intent)
        }

        binding.cardPlaylistVideo.setOnClickListener {
            val intent = Intent(requireContext(), MediaContentActivity::class.java).apply {
                putExtra("TARGET_TYPE", "VIDEO")
                putExtra("TARGET_CATEGORY", "Playlists")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}