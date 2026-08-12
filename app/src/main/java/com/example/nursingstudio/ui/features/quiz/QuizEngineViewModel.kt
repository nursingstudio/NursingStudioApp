package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.model.QuizResultData
import com.example.nursingstudio.domain.usecase.GetQuizUseCase
import com.example.nursingstudio.domain.usecase.SubmitQuizResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

sealed interface QuizEngineUiState {
    data object Loading : QuizEngineUiState
    data class Success(
        val metadata: QuizMetadata,
        val questions: List<QuestionItem>,
        val currentQuestionIndex: Int = 0
    ) : QuizEngineUiState
    data class Error(val message: String) : QuizEngineUiState
    data class Completed(val resultData: QuizResultData) : QuizEngineUiState
}

@HiltViewModel
class QuizEngineViewModel @Inject constructor(
    private val getQuizUseCase: GetQuizUseCase,
    private val submitQuizResultUseCase: SubmitQuizResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizEngineUiState>(QuizEngineUiState.Loading)
    val uiState: StateFlow<QuizEngineUiState> = _uiState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun loadQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = QuizEngineUiState.Loading

            getQuizUseCase(quizId)
                .onSuccess { bundle ->
                    _uiState.value = QuizEngineUiState.Success(
                        metadata = bundle.metadata,
                        questions = bundle.questions,
                        currentQuestionIndex = 0
                    )
                    startTimer(bundle.metadata.totalDurationMinutes * 60L)
                }
                .onFailure { exception ->
                    _uiState.value = QuizEngineUiState.Error(
                        exception.localizedMessage ?: "Failed to load quiz content."
                    )
                }
        }
    }

    private fun startTimer(totalSeconds: Long) {
        timerJob?.cancel()
        _remainingSeconds.value = totalSeconds
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1.seconds)
                _remainingSeconds.value -= 1
            }
            submitQuiz()
        }
    }

    fun selectOption(questionPos: Int, optionIndex: Int) {
        val currentState = _uiState.value
        if (currentState is QuizEngineUiState.Success) {
            val updatedList = currentState.questions.toMutableList()
            if (questionPos in updatedList.indices) {
                updatedList[questionPos] = updatedList[questionPos].copy(selectedOptionIndex = optionIndex)
                _uiState.value = currentState.copy(questions = updatedList)
            }
        }
    }

    fun submitQuiz() {
        timerJob?.cancel()
        val currentState = _uiState.value
        if (currentState is QuizEngineUiState.Success) {
            var correctCount = 0
            var incorrectCount = 0
            var unattemptedCount = 0

            currentState.questions.forEach { q ->
                when (q.selectedOptionIndex) {
                    -1 -> unattemptedCount++
                    q.correctOptionIndex -> correctCount++
                    else -> incorrectCount++
                }
            }

            val totalQuestions = currentState.questions.size
            val attemptedCount = correctCount + incorrectCount
            val totalMarks = totalQuestions * 1.0
            val rawScore = (correctCount * 1.0) - (incorrectCount * 0.25)
            val finalScore = if (rawScore < 0) 0.0 else rawScore
            val percentage = if (totalMarks > 0) ((finalScore / totalMarks) * 100).toFloat() else 0f
            val timeTakenSec = (currentState.metadata.totalDurationMinutes * 60L) - _remainingSeconds.value

            val resultPayload = QuizResultData(
                quizId = currentState.metadata.quizId,
                totalQuestions = totalQuestions,
                attemptedQuestions = attemptedCount,
                unattemptedQuestions = unattemptedCount,
                correctAnswers = correctCount,
                incorrectAnswers = incorrectCount,
                totalPossibleMarks = totalMarks,
                finalScore = finalScore,
                scorePercentage = percentage,
                isPassed = percentage >= 50.0f,
                totalDurationSeconds = currentState.metadata.totalDurationMinutes * 60L,
                timeTakenSeconds = timeTakenSec,
                globalRank = 1,
                totalParticipants = 100
            )

            viewModelScope.launch {
                submitQuizResultUseCase(resultPayload)
            }

            _uiState.value = QuizEngineUiState.Completed(resultData = resultPayload)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}