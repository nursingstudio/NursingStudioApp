package com.example.nursingstudio.ui.features.quiz

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.nursingstudio.data.model.QuizResultData
import com.example.nursingstudio.databinding.FragmentQuizResultBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class QuizResultBottomSheetFragment(
    private val onRetakeClicked: () -> Unit = {},
    private val onReviewClicked: () -> Unit = {}
) : BottomSheetDialogFragment() {

    private var _binding: FragmentQuizResultBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable(KEY_RESULT_DATA, QuizResultData::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(KEY_RESULT_DATA)
        }

        resultData?.let { bindResultData(it) }

        binding.btnRetakeTest.setOnClickListener {
            dismiss()
            onRetakeClicked()
        }

        binding.btnReviewAnswers.setOnClickListener {
            dismiss()
            onReviewClicked()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindResultData(data: QuizResultData) {
        val context = requireContext()

        // Pass/Fail State Styling
        if (data.isPassed) {
            binding.tvPassStatus.text = "PASSED 🎉"
            binding.tvPassStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            binding.cardScoreBadge.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
        } else {
            binding.tvPassStatus.text = "NEEDS IMPROVEMENT ⚠️"
            binding.tvPassStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            binding.cardScoreBadge.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
        }

        // Score & Percentage Formatting
        binding.tvFinalScore.text = String.format(Locale.getDefault(), "%.2f / %.2f", data.finalScore, data.totalPossibleMarks)
        binding.tvPercentage.text = String.format(Locale.getDefault(), "Percentage: %.1f%% (NORCET Marks Rules)", data.scorePercentage)

        // Rank Metrics
        binding.tvGlobalRank.text = if (data.globalRank > 0) "# ${data.globalRank} / ${data.totalParticipants}" else "Calculating..."

        // Time Spent Formatting
        val takenMin = data.timeTakenSeconds / 60
        val takenSec = data.timeTakenSeconds % 60
        val totalMin = data.totalDurationSeconds / 60
        binding.tvTimeTaken.text = String.format(Locale.getDefault(), "%02d:%02d / %02d:00", takenMin, takenSec, totalMin)

        // Detailed Breakdown
        binding.tvAttemptedMetrics.text = "Attempted: ${data.attemptedQuestions} | Unattempted: ${data.unattemptedQuestions}"
        binding.tvAccuracyMetrics.text = "Correct: ${data.correctAnswers} (+1.0) | Incorrect: ${data.incorrectAnswers} (-0.25)"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_RESULT_DATA = "key_result_data"

        fun newInstance(
            resultData: QuizResultData,
            onRetake: () -> Unit,
            onReview: () -> Unit
        ): QuizResultBottomSheetFragment {
            val fragment = QuizResultBottomSheetFragment(onRetake, onReview)
            val args = Bundle().apply {
                putParcelable(KEY_RESULT_DATA, resultData)
            }
            fragment.arguments = args
            return fragment
        }
    }
}