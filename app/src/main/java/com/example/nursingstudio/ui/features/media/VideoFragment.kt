package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
import com.google.android.material.card.MaterialCardView

class VideoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardImportant = view.findViewById<MaterialCardView>(R.id.cardImportantVideo)
        val cardTopic = view.findViewById<MaterialCardView>(R.id.cardTopicVideo)
        val cardPlaylist = view.findViewById<MaterialCardView>(R.id.cardPlaylistVideo)

        cardImportant.setOnClickListener {
            Toast.makeText(requireContext(), "Important videos coming soon.", Toast.LENGTH_SHORT).show()
        }

        cardTopic.setOnClickListener {
            Toast.makeText(requireContext(), "Topic-wise videos coming soon.", Toast.LENGTH_SHORT).show()
        }

        cardPlaylist.setOnClickListener {
            Toast.makeText(requireContext(), "Opening YouTube playlists soon.", Toast.LENGTH_SHORT).show()
        }
    }
}