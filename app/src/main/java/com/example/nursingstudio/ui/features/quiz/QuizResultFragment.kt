package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

        val score = arguments?.getFloat("scoreObtained", 0f)?.toDouble() ?: 0.0
        val maxMarks = arguments?.getFloat("totalMaxMarks", 100f)?.toDouble() ?: 100.0
        val testId = arguments?.getString("testId").orEmpty()
        val accuracy = arguments?.getFloat("accuracyPercentage", 0f)?.toDouble() ?: 0.0
        val timeTakenSeconds = arguments?.getLong("timeTakenSeconds", 0L) ?: 0L

        currentResult = QuizResult(
            testId = testId,
            scoreObtained = score,
            totalMaxMarks = maxMarks,
            accuracyPercentage = accuracy,
            timeTakenSeconds = timeTakenSeconds
        )

        setupScoreCard(score, maxMarks, accuracy)
        loadRanks(testId, score, timeTakenSeconds)

        binding.btnReattemptTest.setOnClickListener {
            val currentTestId = currentResult?.testId ?: testId
            if (currentTestId.isNotEmpty()) {
                viewModel.restartTest(currentTestId, "")

                val bundle = Bundle().apply {
                    putString("testId", currentTestId)
                }
                findNavController().navigate(
                    R.id.action_quizResultFragment_to_quizEngineFragment,
                    bundle
                )
            }
        }

        binding.btnReviewAnswers.setOnClickListener {
            findNavController().navigate(
                R.id.action_quizResultFragment_to_quizReviewFragment
            )
        }
    }

    private fun setupScoreCard(score: Double, maxMarks: Double, accuracy: Double) {
        binding.tvScoreDisplay.text = getString(R.string.score_ratio_fmt, score, maxMarks)

        val scorePercentage = if (maxMarks > 0.0) {
            ((score / maxMarks) * 100.0).coerceIn(0.0, 100.0)
        } else {
            0.0
        }

        binding.tvScorePercentageVal.text = getString(R.string.percentage_fmt_val, scorePercentage)
        binding.tvAccuracyVal.text = getString(R.string.percentage_fmt_val, accuracy)

        val (passFailText, colorRes, bgTintRes) = when {
            scorePercentage < 40.0 -> Triple(
                getString(R.string.status_failed),
                R.color.result_fail_primary,
                R.color.result_fail_bg_tint
            )
            scorePercentage >= 75.0 -> Triple(
                getString(R.string.status_excellent),
                R.color.result_excellent_primary,
                R.color.result_excellent_bg_tint
            )
            else -> Triple(
                getString(R.string.status_passed),
                R.color.result_pass_primary,
                R.color.result_pass_bg_tint
            )
        }

        val primaryColor = ContextCompat.getColor(requireContext(), colorRes)
        val bgTint = ContextCompat.getColor(requireContext(), bgTintRes)

        binding.tvPassFailBadge.text = passFailText
        binding.tvPassFailBadge.setTextColor(primaryColor)
        binding.tvPassFailBadge.setBackgroundColor(bgTint)

        binding.progressScore.setIndicatorColor(primaryColor)
        binding.progressScore.setProgressCompat(scorePercentage.toInt(), true)

        val accuracyColorRes = when {
            accuracy >= 70.0 -> R.color.result_pass_primary
            accuracy >= 40.0 -> R.color.brand_saffron_dark
            else -> R.color.result_fail_primary
        }
        val accuracyColor = ContextCompat.getColor(requireContext(), accuracyColorRes)
        binding.progressAccuracy.setIndicatorColor(accuracyColor)
        binding.progressAccuracy.setProgressCompat(accuracy.toInt(), true)
    }

    private fun loadRanks(testId: String, score: Double, timeTakenSeconds: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val rankResult = repository.fetchUserRanks(testId, score, timeTakenSeconds)
                rankResult.onSuccess { rankData ->
                    binding.tvTestwiseRank.text = getString(
                        R.string.rank_fmt,
                        rankData.testRank,
                        rankData.totalTestParticipants
                    )
                    binding.tvGlobalRank.text = getString(
                        R.string.rank_fmt,
                        rankData.globalRank,
                        rankData.totalGlobalUsers
                    )
                }.onFailure {
                    binding.tvTestwiseRank.text = getString(R.string.rank_fmt, 1, 1)
                    binding.tvGlobalRank.text = getString(R.string.rank_fmt, 1, 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}