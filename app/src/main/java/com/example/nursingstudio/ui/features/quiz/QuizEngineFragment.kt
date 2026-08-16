package com.example.nursingstudio.ui.features.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.databinding.FragmentQuizEngineBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuizEngineFragment : Fragment() {

    private var _binding: FragmentQuizEngineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizEngineViewModel by viewModels()
    private lateinit var paletteAdapter: QuestionPaletteAdapter
    private var testId: String = ""

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

        arguments?.let {
            testId = it.getString("testId", "")
        }

        setupPaletteDrawer()
        setupListeners()
        setupAntiCheatObserver()
        observeViewModel()

        if (testId.isNotEmpty()) {
            viewModel.loadQuiz(testId)
        }
    }

    private fun setupPaletteDrawer() {
        paletteAdapter = QuestionPaletteAdapter { index ->
            viewModel.navigateToQuestion(index)
            binding.drawerLayoutQuiz.closeDrawer(GravityCompat.END)
        }
        binding.layoutPaletteContent.rvQuestionPalette.adapter = paletteAdapter
    }

    private fun setupListeners() {
        binding.layoutMainQuizContent.btnOpenPalette.setOnClickListener {
            binding.drawerLayoutQuiz.openDrawer(GravityCompat.END)
        }

        binding.layoutMainQuizContent.btnMarkForReview.setOnClickListener {
            viewModel.toggleMarkForReview()
        }

        binding.layoutMainQuizContent.btnClearSelection.setOnClickListener {
            viewModel.clearSelection()
        }

        binding.layoutMainQuizContent.btnPrevious.setOnClickListener {
            val currIdx = viewModel.currentIndex.value
            if (currIdx > 0) {
                viewModel.navigateToQuestion(currIdx - 1)
            }
        }

        binding.layoutMainQuizContent.btnNext.setOnClickListener {
            val currIdx = viewModel.currentIndex.value
            val total = viewModel.questions.value.size
            if (currIdx < total - 1) {
                viewModel.navigateToQuestion(currIdx + 1)
            } else {
                showSubmissionConfirmationDialog()
            }
        }
    }

    private fun setupAntiCheatObserver() {
        requireActivity().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                viewModel.handleAppBackgrounded()
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.questions.collect { questions ->
                        if (questions.isNotEmpty()) {
                            renderCurrentQuestion()
                        }
                    }
                }

                launch {
                    viewModel.currentIndex.collect { index ->
                        renderCurrentQuestion()
                    }
                }

                launch {
                    viewModel.userStates.collect { states ->
                        paletteAdapter.submitList(states)
                        updateLegendCounters(states)
                        updateReviewButtonState()
                    }
                }

                launch {
                    viewModel.remainingTimeSeconds.collect { seconds ->
                        val minutes = seconds / 60
                        val secs = seconds % 60
                        binding.layoutMainQuizContent.tvTimer.text = String.format("%02d:%02d", minutes, secs)
                    }
                }

                launch {
                    viewModel.warningAlertTrigger.collect { trigger ->
                        if (trigger) {
                            showAntiCheatWarningDialog()
                        }
                    }
                }

                launch {
                    viewModel.forceSubmitTrigger.collect { trigger ->
                        if (trigger) {
                            executeFinalSubmission()
                        }
                    }
                }
            }
        }
    }

    private fun renderCurrentQuestion() {
        val questions = viewModel.questions.value
        val currIdx = viewModel.currentIndex.value
        if (questions.isEmpty() || currIdx !in questions.indices) return

        val question = questions[currIdx]
        val states = viewModel.userStates.value
        val currentState = states.getOrNull(currIdx)

        binding.layoutMainQuizContent.tvQuestionCounter.text = getString(
            R.string.question_counter,
            currIdx + 1,
            questions.size
        )

        binding.layoutMainQuizContent.tvQuestionText.text = HtmlCompat.fromHtml(
            question.questionText,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        if (!question.imageUrl.isNullOrEmpty()) {
            binding.layoutMainQuizContent.frameMediaContainer.visibility = View.VISIBLE
            binding.layoutMainQuizContent.ivQuestionImage.visibility = View.VISIBLE
            binding.layoutMainQuizContent.playerViewQuestion.visibility = View.GONE

            binding.layoutMainQuizContent.ivQuestionImage.load(question.imageUrl) {
                placeholder(R.drawable.ic_placeholder_image)
                error(R.drawable.ic_placeholder_image)
            }
        } else {
            binding.layoutMainQuizContent.frameMediaContainer.visibility = View.GONE
        }

        binding.layoutMainQuizContent.rgOptionsContainer.removeAllViews()
        question.options.forEachIndexed { optIdx, optionText ->
            val radioButton = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                text = optionText
                textSize = 15f
                setPadding(16, 16, 16, 16)
                isChecked = currentState?.selectedOptionIndex == optIdx
                setOnClickListener {
                    viewModel.selectOption(optIdx)
                }
            }
            binding.layoutMainQuizContent.rgOptionsContainer.addView(radioButton)
        }

        binding.layoutMainQuizContent.btnPrevious.isEnabled = currIdx > 0
        if (currIdx == questions.size - 1) {
            binding.layoutMainQuizContent.btnNext.text = getString(R.string.btn_submit)
        } else {
            binding.layoutMainQuizContent.btnNext.text = getString(R.string.btn_next)
        }
    }

    private fun updateReviewButtonState() {
        val currIdx = viewModel.currentIndex.value
        val currentState = viewModel.userStates.value.getOrNull(currIdx) ?: return

        if (currentState.isMarkedForReview) {
            binding.layoutMainQuizContent.ivReviewStar.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.palette_review)
            )
            binding.layoutMainQuizContent.tvReviewStatus.text = getString(R.string.marked_for_review_active)
            binding.layoutMainQuizContent.tvReviewStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.palette_review)
            )
        } else {
            binding.layoutMainQuizContent.ivReviewStar.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.review_star_inactive)
            )
            binding.layoutMainQuizContent.tvReviewStatus.text = getString(R.string.mark_for_review)
            binding.layoutMainQuizContent.tvReviewStatus.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_secondary)
            )
        }
    }

    private fun updateLegendCounters(states: List<com.example.nursingstudio.data.model.UserAnswerState>) {
        val answeredCount = states.count { it.status == QuestionStatus.ANSWERED }
        val unansweredCount = states.count { it.status == QuestionStatus.UNANSWERED }
        val reviewCount = states.count { it.status == QuestionStatus.MARKED_FOR_REVIEW }
        val answeredMarkedCount = states.count { it.status == QuestionStatus.ANSWERED_AND_MARKED }
        val unvisitedCount = states.count { it.status == QuestionStatus.UNVISITED }

        binding.layoutPaletteContent.tvLegendAnswered.text = getString(R.string.answered_0, answeredCount)
        binding.layoutPaletteContent.tvLegendUnanswered.text = getString(R.string.unanswered_0, unansweredCount)
        binding.layoutPaletteContent.tvLegendReview.text = getString(R.string.marked_for_review_0, reviewCount)
        binding.layoutPaletteContent.tvLegendAnsweredMarked.text = getString(R.string.answered_marked_0, answeredMarkedCount)
        binding.layoutPaletteContent.tvLegendUnvisited.text = getString(R.string.unvisited_0, unvisitedCount)
    }

    private fun showAntiCheatWarningDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.anti_cheat_warning_title))
            .setMessage(getString(R.string.anti_cheat_warning_message))
            .setPositiveButton(getString(R.string.btn_back_to_test)) { dialog, _ ->
                viewModel.resetWarningAlert()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSubmissionConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.submit_test_dialog_title))
            .setMessage(getString(R.string.confirm_submit_prompt))
            .setPositiveButton(getString(R.string.btn_submit)) { _, _ ->
                executeFinalSubmission()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun executeFinalSubmission() {
        // Triggers calculation engine & nav transition to results (Phase 3)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}