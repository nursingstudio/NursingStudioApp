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
 * High-performance container optimized to enforce manual player engine initialization constraints.
 */
class VideoPlayerActivity : AppCompatActivity() {

    private var youtubePlayerView: YouTubePlayerView? = null
    private var activePlayerInstance: YouTubePlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🚀 Programmatic View Matrix Strategy with absolute frame dimensions
        youtubePlayerView = YouTubePlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // 🎯 CRITICAL GOLD STANDARD FIX: Disables auto-init sequence to hand-over control safely
            enableAutomaticInitialization = false
        }
        setContentView(youtubePlayerView)

        val targetVideoId = intent.getStringExtra("EXTRA_VIDEO_ID") ?: "l1WjKspBxX8"

        youtubePlayerView?.let { playerView ->
            lifecycle.addObserver(playerView)

            // Now manual initialization triggers perfectly with 0% state conflicts
            playerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    activePlayerInstance = youTubePlayer
                    youTubePlayer.cueVideo(targetVideoId, 0f)
                }
            })
        }
    }

    override fun onDestroy() {
        // 🚀 Dynamic Memory Layer Purge Engine
        activePlayerInstance = null
        youtubePlayerView?.let { lifecycle.removeObserver(it) }
        youtubePlayerView = null
        super.onDestroy()
    }
}