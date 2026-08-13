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
            handleCategoryClick("category_full_syllabus")
        }
        binding.cardSubjectWise.setOnClickListener {
            handleCategoryClick("category_subject_wise")
        }
        binding.cardTopicWise.setOnClickListener {
            handleCategoryClick("category_topic_wise")
        }
        binding.cardPreviousYear.setOnClickListener {
            handleCategoryClick("category_pyq")
        }
    }

    private fun handleCategoryClick(categoryId: String) {
        val args = Bundle().apply {
            putString("category_id", categoryId)
        }
        findNavController().safeNavigate(
            currentDestinationId = R.id.nav_quiz,
            actionId = R.id.action_nav_quiz_to_nav_test_list,
            args = args
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}