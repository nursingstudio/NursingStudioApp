package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentQuizBinding
import com.example.nursingstudio.utils.safeNavigate

/**
 * 🚀 2026 Gold Standard Quiz Selection Fragment
 * - Zero-leak ViewBinding pattern
 * - Direct, safe Navigation Component flow
 */
class QuizFragment : Fragment() {

    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!

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
            handleCategoryClick("quiz_full_syllabus_2026")
        }
        binding.cardSubjectWise.setOnClickListener {
            handleCategoryClick("quiz_subject_wise_2026")
        }
        binding.cardTopicWise.setOnClickListener {
            handleCategoryClick("quiz_topic_wise_2026")
        }
        binding.cardPreviousYear.setOnClickListener {
            handleCategoryClick("quiz_norcet_2026_01")
        }
    }

    private fun handleCategoryClick(quizId: String) {
        val args = Bundle().apply {
            putString("quiz_id", quizId)
        }
        findNavController().safeNavigate(
            currentDestinationId = R.id.nav_quiz,
            actionId = R.id.action_nav_quiz_to_nav_quiz_engine,
            args = args
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}