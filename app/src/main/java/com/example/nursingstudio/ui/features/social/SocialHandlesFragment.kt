package com.example.nursingstudio.ui.features.social

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R

class SocialHandlesFragment : Fragment() {

    companion object {
        // Yahi wale links jo tumne MainActivity me use kiye the
        private const val URL_YOUTUBE  = "https://youtube.com/@NursingStudio2026"
        private const val URL_WHATSAPP = "https://whatsapp.com/channel/0029Vb6Sjdq6BIEapKtNUE2L"
        private const val URL_TELEGRAM = "https://telegram.me/NursingStudio"
        private const val URL_ARATTAI  = "https://aratt.ai/@nursingstudio"
        private const val URL_INSTA    = "https://instagram.com/NursingStudio2026"
        private const val URL_TWITTER  = "https://twitter.com/"
        private const val URL_FACEBOOK = "https://facebook.com/"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_social_handles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // sab card ids XML me same rakhe hain
        val cardYoutube  = view.findViewById<CardView>(R.id.cardYoutube)
        val cardWhatsapp = view.findViewById<CardView>(R.id.cardWhatsapp)
        val cardTelegram = view.findViewById<CardView>(R.id.cardTelegram)
        val cardArattai  = view.findViewById<CardView>(R.id.cardArattai)
        val cardInsta    = view.findViewById<CardView>(R.id.cardInstagram)
        val cardTwitter  = view.findViewById<CardView>(R.id.cardTwitter)
        val cardFacebook = view.findViewById<CardView>(R.id.cardFacebook)

        cardYoutube.setOnClickListener {
            trackSocialClick("YouTube")
            openUrl(URL_YOUTUBE, "com.google.android.youtube")
        }

        cardWhatsapp.setOnClickListener {
            trackSocialClick("WhatsApp")
            // whatsapp ke channel link par package force karne ki zaroorat nahi
            openUrl(URL_WHATSAPP, null)
        }

        cardTelegram.setOnClickListener {
            trackSocialClick("Telegram")
            openUrl(URL_TELEGRAM, "org.telegram.messenger")
        }

        cardArattai.setOnClickListener {
            trackSocialClick("Arattai")
            openUrl(URL_ARATTAI, "com.aratt.aratt")
        }

        cardInsta.setOnClickListener {
            trackSocialClick("Instagram")
            openUrl(URL_INSTA, "com.instagram.android")
        }

        cardTwitter.setOnClickListener {
            trackSocialClick("Twitter")
            openUrl(URL_TWITTER, "com.twitter.android")
        }

        cardFacebook.setOnClickListener {
            trackSocialClick("Facebook")
            openUrl(URL_FACEBOOK, "com.facebook.katana")
        }
    }

    // --------- Helpers ----------

    private fun openUrl(url: String, packageName: String?) {
        val ctx = requireContext()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (packageName != null) {
                intent.setPackage(packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // fallback: normal browser
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e2: Exception) {
                Toast.makeText(ctx, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun trackSocialClick(channel: String) {
        val ctx = requireContext()
        val sp = ctx.getSharedPreferences("analytics", Context.MODE_PRIVATE)
        val key = "social_click_${channel.lowercase()}"
        val newCount = sp.getInt(key, 0) + 1
        sp.edit().putInt(key, newCount).apply()

        Log.d("SocialAnalytics", "Fragment – clicked $channel, total = $newCount")
    }
}