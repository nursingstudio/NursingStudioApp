package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.data.model.QuizResult
import com.example.nursingstudio.data.model.UserAnswerState
import com.example.nursingstudio.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class QuizEngineViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _questions = MutableStateFlow<List<QuestionItem>>(emptyList())
    val questions: StateFlow<List<QuestionItem>> = _questions.asStateFlow()

    private val _userStates = MutableStateFlow<List<UserAnswerState>>(emptyList())
    val userStates: StateFlow<List<UserAnswerState>> = _userStates.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _remainingTimeSeconds = MutableStateFlow(0L)
    val remainingTimeSeconds: StateFlow<Long> = _remainingTimeSeconds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var strikeCount = 0
    private val _forceSubmitTrigger = MutableStateFlow(false)
    val forceSubmitTrigger: StateFlow<Boolean> = _forceSubmitTrigger.asStateFlow()

    private val _warningAlertTrigger = MutableStateFlow(false)
    val warningAlertTrigger: StateFlow<Boolean> = _warningAlertTrigger.asStateFlow()

    private var timerJob: Job? = null

    fun loadQuiz(testId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = quizRepository.getQuestionsForTest(testId)
            result.onSuccess { loadedQuestions ->
                _questions.value = loadedQuestions
                val initialStates = List(loadedQuestions.size) { index ->
                    UserAnswerState(
                        status = if (index == 0) QuestionStatus.UNANSWERED else QuestionStatus.UNVISITED
                    )
                }
                _userStates.value = initialStates
                val totalDurationMinutes = loadedQuestions.size
                startTimer(totalDurationMinutes * 60L)
            }
            _isLoading.value = false
        }
    }

    private fun startTimer(seconds: Long) {
        timerJob?.cancel()
        _remainingTimeSeconds.value = seconds
        timerJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0) {
                delay(1000L.milliseconds)
                _remainingTimeSeconds.value -= 1
            }
            _forceSubmitTrigger.value = true
        }
    }

    fun selectOption(optionIndex: Int) {
        val currIdx = _currentIndex.value
        val currentStates = _userStates.value.toMutableList()
        val currentState = currentStates[currIdx]

        currentState.selectedOptionIndex = optionIndex
        currentState.status = if (currentState.isMarkedForReview) {
            QuestionStatus.ANSWERED_AND_MARKED
        } else {
            QuestionStatus.ANSWERED
        }

        currentStates[currIdx] = currentState
        _userStates.value = currentStates
    }

    fun clearSelection() {
        val currIdx = _currentIndex.value
        val currentStates = _userStates.value.toMutableList()
        val currentState = currentStates[currIdx]

        currentState.selectedOptionIndex = null
        currentState.status = if (currentState.isMarkedForReview) {
            QuestionStatus.MARKED_FOR_REVIEW
        } else {
            QuestionStatus.UNANSWERED
        }

        currentStates[currIdx] = currentState
        _userStates.value = currentStates
    }

    fun toggleMarkForReview() {
        val currIdx = _currentIndex.value
        val currentStates = _userStates.value.toMutableList()
        val currentState = currentStates[currIdx]

        val newReviewStatus = !currentState.isMarkedForReview
        currentState.isMarkedForReview = newReviewStatus

        currentState.status = when {
            newReviewStatus && currentState.selectedOptionIndex != null -> QuestionStatus.ANSWERED_AND_MARKED
            newReviewStatus -> QuestionStatus.MARKED_FOR_REVIEW
            currentState.selectedOptionIndex != null -> QuestionStatus.ANSWERED
            else -> QuestionStatus.UNANSWERED
        }

        currentStates[currIdx] = currentState
        _userStates.value = currentStates
    }

    fun navigateToQuestion(index: Int) {
        if (index in _questions.value.indices) {
            val currentStates = _userStates.value.toMutableList()
            if (currentStates[index].status == QuestionStatus.UNVISITED) {
                currentStates[index] = currentStates[index].copy(status = QuestionStatus.UNANSWERED)
                _userStates.value = currentStates
            }
            _currentIndex.value = index
        }
    }

    fun handleAppBackgrounded() {
        strikeCount++
        if (strikeCount == 1) {
            _warningAlertTrigger.value = true
        } else if (strikeCount >= 2) {
            _forceSubmitTrigger.value = true
        }
    }

    fun resetWarningAlert() {
        _warningAlertTrigger.value = false
    }

    fun calculateAndSubmitResults(
        userId: String,
        userName: String,
        testId: String,
        testTitle: String,
        onComplete: (QuizResult) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val qList = _questions.value
            val states = _userStates.value

            var correct = 0
            var wrong = 0
            var unvisited = 0

            qList.forEachIndexed { idx, q ->
                val state = states.getOrNull(idx)

                if (state?.isMarkedForReview == true) {
                    unvisited++
                } else {
                    when {
                        state?.selectedOptionIndex == null -> unvisited++
                        state.selectedOptionIndex == q.correctAnswerIndex -> correct++
                        else -> wrong++
                    }
                }
            }

            val score = (correct * 1.0) - (wrong * 0.25)
            val maxMarks = qList.size * 1.0
            val totalAttempted = correct + wrong
            val accuracy = if (totalAttempted > 0) (correct.toDouble() / totalAttempted) * 100.0 else 0.0

            val resultObj = QuizResult(
                userId = userId,
                userName = userName,
                testId = testId,
                testTitle = testTitle,
                totalQuestions = qList.size,
                correctCount = correct,
                wrongCount = wrong,
                unvisitedCount = unvisited,
                scoreObtained = score,
                totalMaxMarks = maxMarks,
                accuracyPercentage = accuracy,
                timeTakenSeconds = 0L
            )

            quizRepository.submitQuizResult(resultObj)
            _isLoading.value = false
            onComplete(resultObj)
        }
    }

    fun resetTestState() {
        val count = _questions.value.size
        _userStates.value = List(count) { UserAnswerState() }
        _currentIndex.value = 0
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}