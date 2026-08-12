package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.nursingstudio.R
import com.example.nursingstudio.databinding.FragmentQuizEngineBinding
import com.example.nursingstudio.utils.safeNavigate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class QuizEngineFragment : Fragment() {

    private var _binding: FragmentQuizEngineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizEngineViewModel by viewModels()
    private lateinit var quizAdapter: QuizQuestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizEngineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val quizId = arguments?.getString("quiz_id") ?: "quiz_norcet_2026_01"

        setupAdapter()
        setupNavigationControls()
        observeViewModel()

        viewModel.loadQuiz(quizId)
    }

    private fun setupAdapter() {
        quizAdapter = QuizQuestionAdapter { questionIndex, optionIndex ->
            viewModel.selectOption(questionIndex, optionIndex)
        }
        binding.quizViewPager.adapter = quizAdapter

        binding.quizViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateProgressCounter(position + 1, quizAdapter.itemCount)
            }
        })
    }

    private fun setupNavigationControls() {
        binding.btnPreviousQuestion.setOnClickListener {
            val current = binding.quizViewPager.currentItem
            if (current > 0) {
                binding.quizViewPager.currentItem = current - 1
            }
        }

        binding.btnNextQuestion.setOnClickListener {
            val current = binding.quizViewPager.currentItem
            val total = quizAdapter.itemCount
            if (current < total - 1) {
                binding.quizViewPager.currentItem = current + 1
            } else {
                viewModel.submitQuiz()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is QuizEngineUiState.Loading -> {
                                // Handled via progress indicator
                            }
                            is QuizEngineUiState.Success -> {
                                binding.tvQuizTitle.text = state.metadata.title
                                binding.tvSubjectTag.text = state.metadata.subject
                                quizAdapter.submitList(state.questions)
                                updateProgressCounter(
                                    binding.quizViewPager.currentItem + 1,
                                    state.questions.size
                                )
                            }
                            is QuizEngineUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            }
                            is QuizEngineUiState.Completed -> {
                                val bundle = Bundle().apply {
                                    putInt("correct_answers", state.resultData.correctAnswers)
                                    putInt("total_questions", state.resultData.totalQuestions)
                                    putFloat("score_percentage", state.resultData.scorePercentage)
                                }
                                findNavController().safeNavigate(
                                    currentDestinationId = R.id.nav_quiz_engine,
                                    actionId = R.id.action_quizEngine_to_resultFragment,
                                    args = bundle
                                )
                            }
                        }
                    }
                }

                launch {
                    viewModel.remainingSeconds.collect { seconds ->
                        val minutes = seconds / 60
                        val secs = seconds % 60
                        binding.tvTimerClock.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
                    }
                }
            }
        }
    }

    private fun updateProgressCounter(current: Int, total: Int) {
        binding.tvQuestionProgressCounter.text = getString(R.string.of, current, total)
        binding.btnNextQuestion.text = if (current == total) "Submit" else "Next"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}