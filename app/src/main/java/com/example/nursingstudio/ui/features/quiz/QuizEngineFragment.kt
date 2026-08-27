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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.fragment.findNavController
import coil.load
import coil.size.Scale
import com.example.nursingstudio.R
import com.example.nursingstudio.data.model.MediaType
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.databinding.FragmentQuizEngineBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class QuizEngineFragment : Fragment() {

    private var _binding: FragmentQuizEngineBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuizEngineViewModel by activityViewModels()
    private lateinit var paletteAdapter: QuestionPaletteAdapter

    private var exoPlayer: ExoPlayer? = null
    private var activeWarningDialog: AlertDialog? = null
    private var currentMediaUrl: String? = null

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

        val testId = arguments?.getString("testId").orEmpty()
        val title = arguments?.getString("title").orEmpty()

        if (viewModel.uiState.value is QuizEngineState.Loading && testId.isNotEmpty()) {
            viewModel.initQuizWithArgs(testId, title)
        }

        setupPaletteDrawer()
        setupListeners()
        setupAntiCheatObserver()
        observeViewModel()
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
            val state = viewModel.uiState.value
            if (state is QuizEngineState.Content && state.currentIndex > 0) {
                viewModel.navigateToQuestion(state.currentIndex - 1)
            }
        }

        binding.layoutMainQuizContent.btnNext.setOnClickListener {
            val state = viewModel.uiState.value
            if (state is QuizEngineState.Content) {
                if (state.currentIndex < state.questions.size - 1) {
                    viewModel.navigateToQuestion(state.currentIndex + 1)
                } else {
                    showSubmissionConfirmationDialog()
                }
            }
        }

        binding.btnRetry.setOnClickListener {
            val testId = arguments?.getString("testId").orEmpty()
            val title = arguments?.getString("title").orEmpty()
            viewModel.initQuizWithArgs(testId, title)
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
                viewModel.uiState.collect { state ->
                    when (state) {
                        is QuizEngineState.Loading -> {
                            binding.progressBar.isVisible = true
                            binding.layoutErrorState.isVisible = false
                            binding.drawerLayoutQuiz.isVisible = false
                        }
                        is QuizEngineState.Content -> {
                            binding.progressBar.isVisible = false
                            binding.layoutErrorState.isVisible = false
                            binding.drawerLayoutQuiz.isVisible = true
                            renderContent(state)
                        }
                        is QuizEngineState.Error -> {
                            binding.progressBar.isVisible = false
                            binding.drawerLayoutQuiz.isVisible = false
                            binding.layoutErrorState.isVisible = true

                            val errorMessage = when {
                                state.isNoQuestions -> getString(R.string.error_no_questions_found)
                                !state.customMessage.isNullOrBlank() -> state.customMessage
                                else -> getString(R.string.error_quiz_generic)
                            }
                            binding.tvErrorMessage.text = errorMessage
                        }
                    }
                }
            }
        }
    }

    private fun renderContent(state: QuizEngineState.Content) {
        val questions = state.questions
        val currIdx = state.currentIndex
        if (questions.isEmpty() || currIdx !in questions.indices) return

        val passedTitle = arguments?.getString("title").orEmpty()
        val displayTitle = when {
            state.testTitle.isNotBlank() -> state.testTitle
            passedTitle.isNotBlank() -> passedTitle
            else -> getString(R.string.default_test_title)
        }
        binding.layoutMainQuizContent.tvQuizTitle.text = displayTitle

        val question = questions[currIdx]
        val currentState = state.userStates.getOrNull(currIdx)

        val minutes = state.remainingTimeSeconds / 60
        val secs = state.remainingTimeSeconds % 60
        binding.layoutMainQuizContent.tvTimer.text = String.format(
            Locale.getDefault(),
            "%02d:%02d",
            minutes,
            secs
        )

        binding.layoutMainQuizContent.tvQuestionCounter.text = getString(
            R.string.question_counter,
            currIdx + 1,
            questions.size
        )

        binding.layoutMainQuizContent.tvQuestionText.text = HtmlCompat.fromHtml(
            question.questionText,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        // Optimized Media Renderer
        bindMediaContent(question.mediaType, question.mediaUrl)

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

        paletteAdapter.submitList(state.userStates)
        updateLegendCounters(state.userStates)
        updateReviewButtonState(currentState)

        binding.layoutMainQuizContent.btnPrevious.isEnabled = currIdx > 0
        if (currIdx == questions.size - 1) {
            binding.layoutMainQuizContent.btnNext.text = getString(R.string.btn_submit)
        } else {
            binding.layoutMainQuizContent.btnNext.text = getString(R.string.btn_next)
        }

        // Single Instance Warning Dialog Fix
        if (state.isWarningVisible) {
            showAntiCheatWarningDialog()
        } else {
            dismissAntiCheatWarningDialog()
        }

        if (state.isForceSubmitNeeded) {
            executeFinalSubmission()
        }
    }

    /**
     * 2026 Gold Standard Media Binding (Fixes Image Revisit Delay & ExoPlayer Video Stream)
     */
    private fun bindMediaContent(mediaType: MediaType, mediaUrl: String?) {
        val bindingContent = binding.layoutMainQuizContent

        if (mediaUrl.isNullOrBlank() || mediaType == MediaType.NONE) {
            bindingContent.frameMediaContainer.visibility = View.GONE
            releasePlayer()
            currentMediaUrl = null
            return
        }

        bindingContent.frameMediaContainer.visibility = View.VISIBLE

        when (mediaType) {
            MediaType.IMAGE -> {
                releasePlayer()
                currentMediaUrl = mediaUrl
                bindingContent.playerViewQuestion.visibility = View.GONE
                bindingContent.ivQuestionImage.visibility = View.VISIBLE

                bindingContent.ivQuestionImage.load(mediaUrl) {
                    crossfade(true)
                    scale(Scale.FIT)
                    placeholder(R.drawable.ic_placeholder_image)
                    error(R.drawable.ic_placeholder_image)
                }
            }
            MediaType.VIDEO -> {
                bindingContent.ivQuestionImage.visibility = View.GONE
                bindingContent.playerViewQuestion.visibility = View.VISIBLE

                // Avoid re-preparing player if same video is already loaded
                if (currentMediaUrl == mediaUrl && exoPlayer != null) return

                releasePlayer()
                currentMediaUrl = mediaUrl

                val player = ExoPlayer.Builder(requireContext()).build().also {
                    it.repeatMode = Player.REPEAT_MODE_OFF
                    it.playWhenReady = true
                }

                exoPlayer = player
                bindingContent.playerViewQuestion.player = player

                val mediaItem = MediaItem.fromUri(mediaUrl)
                player.setMediaItem(mediaItem)
                player.prepare()
            }
        }
    }

    private fun releasePlayer() {
        binding.layoutMainQuizContent.playerViewQuestion.player = null
        exoPlayer?.apply {
            stop()
            clearMediaItems()
            release()
        }
        exoPlayer = null
    }

    private fun updateReviewButtonState(currentState: UserAnswerState?) {
        if (currentState == null) return
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

    private fun updateLegendCounters(states: List<UserAnswerState>) {
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

    /**
     * Prevents multiple stacked dialogs on timer tick state emissions
     */
    private fun showAntiCheatWarningDialog() {
        if (activeWarningDialog?.isShowing == true) return

        activeWarningDialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.anti_cheat_warning_title))
            .setMessage(getString(R.string.anti_cheat_warning_message))
            .setPositiveButton(getString(R.string.btn_back_to_test)) { dialog, _ ->
                viewModel.resetWarningAlert()
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        activeWarningDialog?.show()
    }

    private fun dismissAntiCheatWarningDialog() {
        activeWarningDialog?.dismiss()
        activeWarningDialog = null
    }

    private fun showSubmissionConfirmationDialog() {
        val state = viewModel.uiState.value
        if (state !is QuizEngineState.Content) return

        val total = state.questions.size
        val answered = state.userStates.count { it.status == QuestionStatus.ANSWERED }
        val unanswered = state.userStates.count { it.status == QuestionStatus.UNANSWERED }
        val review = state.userStates.count { it.status == QuestionStatus.MARKED_FOR_REVIEW }
        val answeredMarked = state.userStates.count { it.status == QuestionStatus.ANSWERED_AND_MARKED }
        val unvisited = state.userStates.count { it.status == QuestionStatus.UNVISITED }

        val minutes = state.remainingTimeSeconds / 60
        val secs = state.remainingTimeSeconds % 60
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)

        val bottomSheet = TestSubmitBottomSheetFragment(
            total = total,
            answered = answered,
            unanswered = unanswered,
            review = review,
            answeredMarked = answeredMarked,
            unvisited = unvisited,
            timeFormatted = formattedTime,
            onSubmitConfirmed = {
                executeFinalSubmission()
            }
        )
        bottomSheet.show(childFragmentManager, "TestSubmitBottomSheet")
    }

    private fun executeFinalSubmission() {
        val state = viewModel.uiState.value
        if (state !is QuizEngineState.Content) return

        viewModel.calculateAndSubmitResults { result ->
            val bundle = Bundle().apply {
                putFloat("scoreObtained", result.scoreObtained.toFloat())
                putFloat("totalMaxMarks", result.totalMaxMarks.toFloat())
                putString("testId", result.testId)
                putFloat("accuracyPercentage", result.accuracyPercentage.toFloat())
                putLong("timeTakenSeconds", result.timeTakenSeconds)
            }

            findNavController().navigate(
                R.id.action_quizEngineFragment_to_quizResultFragment,
                bundle
            )
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissAntiCheatWarningDialog()
        releasePlayer()
        _binding = null
    }
}