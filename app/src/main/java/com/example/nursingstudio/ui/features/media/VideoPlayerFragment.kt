package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View // ✅ FIX: Force standard Android UI View over system file
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
import com.example.nursingstudio.databinding.FragmentVideoPlayerBinding
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import java.util.Random

@OptIn(UnstableApi::class)
class VideoPlayerFragment : Fragment() {

    private var _binding: FragmentVideoPlayerBinding? = null
    private val binding get() = _binding!!
    private var exoPlayer: ExoPlayer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var watermarkRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // ✅ FIX: Secure inflated type bounds context
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adjust system layout bounds inside the navigation frame context
        ViewCompat.setOnApplyWindowInsetsListener(binding.speedControlsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val displayCutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val density = resources.displayMetrics.density
            val margin16 = (16 * density).toInt()

            val params = v.layoutParams as android.widget.FrameLayout.LayoutParams
            params.topMargin = systemBars.top + displayCutout.top + margin16
            params.rightMargin = systemBars.right + displayCutout.right + margin16
            v.layoutParams = params
            insets
        }

        val streamUrl = arguments?.getString("CONTENT_URL") ?: ""
        val videoType = arguments?.getString("VIDEO_TYPE") ?: "FREE"

        lifecycle.addObserver(binding.youtubePlayerView)

        // 🚀 2026 ENGINE BRANCH CONTROL ROUTER
        if (videoType == "FREE") {
            initializeFreeYouTubeEngine(streamUrl)
        } else {
            initializePaidPremiumEngine(streamUrl)
        }

        binding.btnVideoQuality.setOnClickListener {
            showQualitySelectionDialog()
        }
    }

    private fun initializeFreeYouTubeEngine(videoId: String) {
        binding.playerView.visibility = View.GONE
        binding.speedControlsLayout.visibility = View.GONE
        binding.youtubePlayerView.visibility = View.VISIBLE
        binding.youtubeOverlayShield.visibility = View.VISIBLE

        binding.youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                binding.playerProgressBar.visibility = View.GONE
                youTubePlayer.loadVideo(videoId, 0f)
            }
        })
    }

    private fun initializePaidPremiumEngine(url: String) {
        binding.youtubePlayerView.visibility = View.GONE
        binding.youtubeOverlayShield.visibility = View.GONE
        binding.playerView.visibility = View.VISIBLE
        binding.speedControlsLayout.visibility = View.VISIBLE

        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this
            setMediaItem(MediaItem.fromUri(url.toUri()))
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (_binding == null) return
                    binding.playerProgressBar.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                }
            })
        }

        startDynamicAntiPiracyMatrix()
    }

    private fun startDynamicAntiPiracyMatrix() {
        binding.tvDynamicWatermark.visibility = View.VISIBLE
        binding.tvDynamicWatermark.text =
            getString(R.string.nursingstudent_studio_com_2026_secure_node)

        val random = Random()
        watermarkRunnable = object : Runnable {
            override fun run() {
                if (_binding == null) return

                // ✅ FIX: Safe access to layout view group dimensions explicitly
                val parentView = binding.root as? ViewGroup
                val rootWidth = (parentView?.width ?: 0) - binding.tvDynamicWatermark.width
                val rootHeight = (parentView?.height ?: 0) - binding.tvDynamicWatermark.height

                // ✅ FIX: Proper integer conditional boundaries without type mismatch
                if (rootWidth > 0 && rootHeight > 0) {
                    binding.tvDynamicWatermark.x = random.nextInt(rootWidth).toFloat()
                    binding.tvDynamicWatermark.y = random.nextInt(rootHeight).toFloat()
                }
                handler.postDelayed(this, 4000)
            }
        }
        handler.post(watermarkRunnable!!)
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
        watermarkRunnable?.let { handler.removeCallbacks(it) }
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}