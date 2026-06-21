package com.example.nursingstudio.ui.features.media

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

/**
 * 🚀 2026 INDUSTRY GOLD STANDARD: Isolated Hardware-Accelerated Video Engine
 * High-performance container completely free from custom builder context dependencies.
 */
class VideoPlayerActivity : AppCompatActivity() {

    private var youtubePlayerView: YouTubePlayerView? = null
    private var activePlayerInstance: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 Programmatic View Matrix Strategy
        youtubePlayerView = YouTubePlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(youtubePlayerView)

        val targetVideoId = intent.getStringExtra("EXTRA_VIDEO_ID") ?: "l1WjKspBxX8"

        youtubePlayerView?.let { playerView ->
            lifecycle.addObserver(playerView)

            // 🚀 2026 Standard Engine Initialization Loop without Context Overheads
            playerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    activePlayerInstance = youTubePlayer
                    // Automatically cues the NORCET video safely
                    youTubePlayer.cueVideo(targetVideoId, 0f)
                }
            })
        }
    }

    override fun onDestroy() {
        // 🚀 Isolated Memory Layer Cleanup Vector
        activePlayerInstance = null
        youtubePlayerView?.let { lifecycle.removeObserver(it) }
        youtubePlayerView = null
        super.onDestroy()
    }
}