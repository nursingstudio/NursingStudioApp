package com.example.nursingstudio.ui.features.quiz

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentQuizBinding
import com.example.nursingstudio.utils.safeNavigate

class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategoryClickListeners()
    }

    private fun setupCategoryClickListeners() {
        binding.cardFullSyllabus.setOnClickListener {
            handleCategoryClick("Full Syllabus Mock Test", "quiz_full_syllabus_2026")
        }
        binding.cardSubjectWise.setOnClickListener {
            handleCategoryClick("Subject-wise Test", "quiz_subject_wise_2026")
        }
        binding.cardTopicWise.setOnClickListener {
            handleCategoryClick("Topic-wise Practice", "quiz_topic_wise_2026")
        }
        binding.cardPreviousYear.setOnClickListener {
            handleCategoryClick("Previous Year Papers", "quiz_norcet_2026_01")
        }
    }

    private fun handleCategoryClick(title: String, quizId: String) {
        playFeedbackSound()
        Toast.makeText(requireContext(), "Opening $title...", Toast.LENGTH_SHORT).show()

        // 🚀 2026 Gold Standard: Safe Navigation via Navigation Component
        val args = Bundle().apply {
            putString("quiz_id", quizId)
        }
        findNavController().safeNavigate(
            currentDestinationId = R.id.nav_quiz,
            actionId = R.id.action_nav_quiz_to_nav_quiz_engine,
            args = args
        )
    }

    private fun playFeedbackSound() {
        val sp = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        if (sp.getBoolean("enable_quiz_sound", true)) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.test_ping)
            mediaPlayer?.setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
}