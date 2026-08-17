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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

sealed interface QuizEngineState {
    object Loading : QuizEngineState
    data class Content(
        val testTitle: String = "",
        val questions: List<QuestionItem> = emptyList(),
        val userStates: List<UserAnswerState> = emptyList(),
        val currentIndex: Int = 0,
        val remainingTimeSeconds: Long = 0L,
        val isWarningVisible: Boolean = false,
        val isForceSubmitNeeded: Boolean = false
    ) : QuizEngineState
    data class Error(val message: String) : QuizEngineState
}

@HiltViewModel
class QuizEngineViewModel @Inject constructor(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizEngineState>(QuizEngineState.Loading)
    val uiState: StateFlow<QuizEngineState> = _uiState.asStateFlow()

    private var strikeCount = 0
    private var timerJob: Job? = null

    fun loadQuiz(testId: String, testTitle: String = "Test Series", durationMinutes: Long = 60L) {
        viewModelScope.launch {
            _uiState.value = QuizEngineState.Loading
            quizRepository.getQuestionsForTest(testId)
                .onSuccess { loadedQuestions ->
                    if (loadedQuestions.isNotEmpty()) {
                        val initialStates = List(loadedQuestions.size) { index ->
                            UserAnswerState(
                                status = if (index == 0) QuestionStatus.UNANSWERED else QuestionStatus.UNVISITED
                            )
                        }
                        val totalTimeSeconds = durationMinutes * 60L
                        _uiState.value = QuizEngineState.Content(
                            testTitle = testTitle,
                            questions = loadedQuestions,
                            userStates = initialStates,
                            currentIndex = 0,
                            remainingTimeSeconds = totalTimeSeconds
                        )
                        startTimer(totalTimeSeconds)
                    } else {
                        _uiState.value = QuizEngineState.Error("No questions found for this test.")
                    }
                }
                .onFailure { error ->
                    _uiState.value = QuizEngineState.Error(error.localizedMessage ?: "Unknown Error")
                }
        }
    }

    private fun startTimer(seconds: Long) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var timeLeft = seconds
            while (timeLeft > 0) {
                delay(1.seconds)
                timeLeft--
                _uiState.update { currentState ->
                    if (currentState is QuizEngineState.Content) {
                        currentState.copy(remainingTimeSeconds = timeLeft)
                    } else currentState
                }
            }
            _uiState.update { currentState ->
                if (currentState is QuizEngineState.Content) {
                    currentState.copy(isForceSubmitNeeded = true)
                } else currentState
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            val currIdx = state.currentIndex
            val updatedStates = state.userStates.toMutableList()
            if (currIdx !in updatedStates.indices) return@update state

            val currentState = updatedStates[currIdx]
            val newStatus = if (currentState.isMarkedForReview) {
                QuestionStatus.ANSWERED_AND_MARKED
            } else {
                QuestionStatus.ANSWERED
            }

            updatedStates[currIdx] = currentState.copy(
                selectedOptionIndex = optionIndex,
                status = newStatus
            )
            state.copy(userStates = updatedStates)
        }
    }

    fun clearSelection() {
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            val currIdx = state.currentIndex
            val updatedStates = state.userStates.toMutableList()
            if (currIdx !in updatedStates.indices) return@update state

            val currentState = updatedStates[currIdx]
            val newStatus = if (currentState.isMarkedForReview) {
                QuestionStatus.MARKED_FOR_REVIEW
            } else {
                QuestionStatus.UNANSWERED
            }

            updatedStates[currIdx] = currentState.copy(
                selectedOptionIndex = null,
                status = newStatus
            )
            state.copy(userStates = updatedStates)
        }
    }

    fun toggleMarkForReview() {
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            val currIdx = state.currentIndex
            val updatedStates = state.userStates.toMutableList()
            if (currIdx !in updatedStates.indices) return@update state

            val currentState = updatedStates[currIdx]
            val newReviewStatus = !currentState.isMarkedForReview

            val newStatus = when {
                newReviewStatus && currentState.selectedOptionIndex != null -> QuestionStatus.ANSWERED_AND_MARKED
                newReviewStatus -> QuestionStatus.MARKED_FOR_REVIEW
                currentState.selectedOptionIndex != null -> QuestionStatus.ANSWERED
                else -> QuestionStatus.UNANSWERED
            }

            updatedStates[currIdx] = currentState.copy(
                isMarkedForReview = newReviewStatus,
                status = newStatus
            )
            state.copy(userStates = updatedStates)
        }
    }

    fun navigateToQuestion(index: Int) {
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            if (index !in state.questions.indices) return@update state

            val updatedStates = state.userStates.toMutableList()
            if (updatedStates[index].status == QuestionStatus.UNVISITED) {
                updatedStates[index] = updatedStates[index].copy(status = QuestionStatus.UNANSWERED)
            }

            state.copy(
                currentIndex = index,
                userStates = updatedStates
            )
        }
    }

    fun handleAppBackgrounded() {
        strikeCount++
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            if (strikeCount == 1) {
                state.copy(isWarningVisible = true)
            } else {
                state.copy(isForceSubmitNeeded = true)
            }
        }
    }

    fun resetWarningAlert() {
        _uiState.update { state ->
            if (state !is QuizEngineState.Content) return@update state
            state.copy(isWarningVisible = false)
        }
    }


    fun calculateAndSubmitResults(
        userId: String = "guest_user",
        userName: String = "Student",
        testId: String,
        onComplete: (QuizResult) -> Unit
    ) {
        val state = _uiState.value
        if (state !is QuizEngineState.Content) return

        viewModelScope.launch {
            _uiState.value = QuizEngineState.Loading
            val qList = state.questions
            val userStates = state.userStates

            var correct = 0
            var wrong = 0
            var unvisited = 0

            qList.forEachIndexed { idx, q ->
                val ansState = userStates.getOrNull(idx)
                when {
                    ansState == null || ansState.selectedOptionIndex == null -> unvisited++
                    ansState.selectedOptionIndex == q.correctAnswerIndex -> correct++
                    else -> wrong++
                }
            }

            val score = (correct * 1.0) - (wrong * 0.25)
            val maxMarks = qList.size * 1.0
            val attempted = correct + wrong
            val accuracy = if (attempted > 0) (correct.toDouble() / attempted) * 100.0 else 0.0

            val timeTaken = (state.questions.size * 60L) - state.remainingTimeSeconds

            val resultObj = QuizResult(
                userId = userId,
                userName = userName,
                testId = testId,
                testTitle = state.testTitle,
                totalQuestions = qList.size,
                correctCount = correct,
                wrongCount = wrong,
                unvisitedCount = unvisited,
                scoreObtained = score,
                totalMaxMarks = maxMarks,
                accuracyPercentage = accuracy,
                timeTakenSeconds = if (timeTaken > 0) timeTaken else 0L
            )

            quizRepository.submitQuizResult(resultObj)
            onComplete(resultObj)
        }
    }

    /**
     * Resets the active test state to enable clean re-attempts without residual user state.
     */
    fun resetTestState() {
        _uiState.value = QuizEngineState.Loading
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}