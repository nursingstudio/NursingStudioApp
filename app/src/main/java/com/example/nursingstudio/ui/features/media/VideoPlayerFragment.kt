package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.ActivityVideoPlayerBinding

@OptIn(UnstableApi::class)
// 🚀 2026 INDUSTRY GOLD STANDARD: Converted from Activity window layer to a modular embedded Fragment container
class VideoPlayerFragment : Fragment() {

    private var _binding: ActivityVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var exoPlayer: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🎨 Layout boundaries adjustment inside navigation host framework constraints
        ViewCompat.setOnApplyWindowInsetsListener(binding.speedControlsLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())

            val density = resources.displayMetrics.density
            val margin16 = (16 * density).toInt()

            val params = view.layoutParams as android.widget.FrameLayout.LayoutParams
            params.topMargin = systemBars.top + displayCutout.top + margin16
            params.rightMargin = systemBars.right + displayCutout.right + margin16
            view.layoutParams = params

            insets
        }

        // 🔒 Dynamically map data streams from secure transaction args pipeline bundle
        val streamUrl = arguments?.getString("CONTENT_URL") ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        initializeStreamingEngine(streamUrl)

        binding.btnVideoQuality.setOnClickListener {
            showQualitySelectionDialog()
        }
    }

    private fun initializeStreamingEngine(url: String) {
        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this
            val mediaItem = MediaItem.fromUri(url.toUri())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (_binding == null) return
                    binding.playerProgressBar.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }
    }

    private fun showQualitySelectionDialog() {
        val player = exoPlayer ?: return
        val currentTracks = player.currentTracks
        val videoOptions = mutableListOf<Pair<String, TrackSelectionOverride>>()

        for (trackGroup in currentTracks.groups) {
            if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_VIDEO) {
                for (i in 0 until trackGroup.length) {
                    if (trackGroup.isTrackSupported(i)) {
                        val format = trackGroup.getTrackFormat(i)
                        val height = format.height
                        if (height > 0) {
                            videoOptions.add(Pair("${height}p", TrackSelectionOverride(trackGroup.mediaTrackGroup, i)))
                        }
                    }
                }
            }
        }

        val labels = arrayOf("Auto (Adaptive)") + videoOptions.map { it.first }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select Video Quality")
            .setItems(labels) { _, which ->
                if (which == 0) {
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .clearOverridesOfType(androidx.media3.common.C.TRACK_TYPE_VIDEO)
                        .build()
                    binding.btnVideoQuality.text = getString(R.string.quality_auto)
                } else {
                    val selectedOption = videoOptions[which - 1]
                    player.trackSelectionParameters = player.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(selectedOption.second)
                        .build()
                    binding.btnVideoQuality.text = selectedOption.first
                }
            }.show()
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}