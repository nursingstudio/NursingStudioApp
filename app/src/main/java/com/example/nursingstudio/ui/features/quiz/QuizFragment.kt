package com.example.nursingstudio.ui.features.quiz

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.nursingstudio.R
import com.google.android.material.card.MaterialCardView

class QuizFragment : Fragment() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_quiz, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardFull = view.findViewById<MaterialCardView>(R.id.cardFullSyllabus)
        val cardSubject = view.findViewById<MaterialCardView>(R.id.cardSubjectWise)
        val cardTopic = view.findViewById<MaterialCardView>(R.id.cardTopicWise)
        val cardPrev = view.findViewById<MaterialCardView>(R.id.cardPreviousYear)

        cardFull.setOnClickListener { handleCategoryClick("Full Syllabus Mock Test") }
        cardSubject.setOnClickListener { handleCategoryClick("Subject-wise Test") }
        cardTopic.setOnClickListener { handleCategoryClick("Topic-wise Practice") }
        cardPrev.setOnClickListener { handleCategoryClick("Previous Year Papers") }
    }

    private fun handleCategoryClick(title: String) {
        playFeedbackSound()
        Toast.makeText(requireContext(), "Opening $title...", Toast.LENGTH_SHORT).show()

        // Fragment Transaction to open QuizEngineFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, QuizEngineFragment()) // Make sure container ID matches your Activity XML container
            .addToBackStack(null)
            .commit()
    }

    private fun playFeedbackSound() {
        val sp = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        if (sp.getBoolean("enable_quiz_sound", true)) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.test_ping)
            mediaPlayer?.setOnCompletionListener { it.release() }
            mediaPlayer?.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
    }
}