package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.databinding.FragmentQuizReviewBinding

class QuizReviewFragment : Fragment() {

    private var _binding: FragmentQuizReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizEngineViewModel by activityViewModels()
    private val reviewAdapter = QuizReviewAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvReviewQuestions.adapter = reviewAdapter

        binding.btnBackReview.setOnClickListener {
            findNavController().navigateUp()
        }

        val questions = viewModel.questions.value
        val states = viewModel.userStates.value

        val reviewList = questions.mapIndexed { index, question ->
            val userState = states.getOrNull(index) ?: UserAnswerState()
            ReviewItem(
                question = question,
                userState = userState,
                questionNumber = index + 1
            )
        }

        reviewAdapter.submitList(reviewList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}