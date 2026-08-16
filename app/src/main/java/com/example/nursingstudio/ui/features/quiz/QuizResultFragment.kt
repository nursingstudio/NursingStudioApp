package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuizResult
import com.example.nursingstudio.data.repository.QuizRepository
import com.example.nursingstudio.databinding.FragmentQuizResultBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuizResultFragment : Fragment() {

    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizEngineViewModel by activityViewModels()

    @Inject
    lateinit var repository: QuizRepository

    private var currentResult: QuizResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val score = arguments?.getDouble("scoreObtained") ?: 0.0
        val maxMarks = arguments?.getDouble("totalMaxMarks") ?: 100.0
        val testId = arguments?.getString("testId") ?: ""
        val accuracy = arguments?.getDouble("accuracyPercentage") ?: 0.0

        setupScoreCard(score, maxMarks, accuracy)
        loadRanks(testId, score)

        binding.btnReattemptTest.setOnClickListener {
            viewModel.resetTestState()
            findNavController().popBackStack(R.id.quizEngineFragment, false)
        }

        binding.btnReviewAnswers.setOnClickListener {
            findNavController().navigate(R.id.quizReviewFragment)
        }
    }

    private fun setupScoreCard(score: Double, maxMarks: Double, accuracy: Double) {
        binding.tvScoreDisplay.text = getString(R.string.score_ratio_fmt, score, maxMarks)
        binding.tvPercentageDisplay.text = getString(R.string.percentage_fmt, accuracy)

        val isPassed = accuracy >= 50.0
        val passFailText = if (isPassed) getString(R.string.status_passed) else getString(R.string.status_failed)
        val bgColorRes = if (isPassed) R.color.result_pass_bg else R.color.result_fail_bg
        val textColorRes = if (isPassed) R.color.result_pass_text else R.color.result_fail_text

        binding.tvPassFailBadge.text = passFailText
        binding.tvPassFailBadge.setBackgroundColor(ContextCompat.getColor(requireContext(), bgColorRes))
        binding.tvPassFailBadge.setTextColor(ContextCompat.getColor(requireContext(), textColorRes))
    }

    private fun loadRanks(testId: String, score: Double) {
        viewLifecycleOwner.lifecycleScope.launch {
            val rankResult = repository.fetchUserRanks(testId, score)
            rankResult.onSuccess { rankData ->
                binding.tvTestwiseRank.text = getString(R.string.rank_fmt, rankData.testRank, rankData.totalTestParticipants)
                binding.tvGlobalRank.text = getString(R.string.rank_fmt, rankData.globalRank, rankData.totalGlobalUsers)
            }.onFailure {
                binding.tvTestwiseRank.text = getString(R.string.rank_fmt, 1, 1)
                binding.tvGlobalRank.text = getString(R.string.rank_fmt, 1, 1)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}