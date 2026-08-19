package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.databinding.FragmentQuizReviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
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

        observeReviewContent()
    }

    private fun observeReviewContent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { currentState ->
                    if (currentState is QuizEngineState.Content) {
                        val questions: List<QuestionItem> = currentState.questions
                        val states: List<UserAnswerState> = currentState.userStates

                        val reviewList = questions.mapIndexed { index: Int, question: QuestionItem ->
                            val userState = states.getOrNull(index) ?: UserAnswerState()
                            ReviewItem(
                                question = question,
                                userState = userState,
                                questionNumber = index + 1
                            )
                        }

                        reviewAdapter.submitList(reviewList)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}