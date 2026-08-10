package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.repository.QuizRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface QuizEngineUiState {
    object Loading : QuizEngineUiState
    data class Success(
        val metadata: QuizMetadata,
        val questions: List<QuestionItem>,
        val currentQuestionIndex: Int = 0
    ) : QuizEngineUiState
    data class Error(val message: String) : QuizEngineUiState
    data class Completed(val totalQuestions: Int, val correctAnswers: Int, val scorePercentage: Float) : QuizEngineUiState
}

class QuizEngineViewModel(
    private val repository: QuizRepository = QuizRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizEngineUiState>(QuizEngineUiState.Loading)
    val uiState: StateFlow<QuizEngineUiState> = _uiState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun loadQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = QuizEngineUiState.Loading

            val metaResult = repository.getQuizMetadata(quizId)
            val questionsResult = repository.getQuizQuestions(quizId)

            if (metaResult.isSuccess && questionsResult.isSuccess) {
                val metadata = metaResult.getOrThrow()
                val questions = questionsResult.getOrThrow()

                _uiState.value = QuizEngineUiState.Success(
                    metadata = metadata,
                    questions = questions,
                    currentQuestionIndex = 0
                )

                startTimer(metadata.totalDurationMinutes * 60L)
            } else {
                _uiState.value = QuizEngineUiState.Error("Failed to load quiz content. Please check network.")
            }
        }
    }

    private fun startTimer(totalSeconds: Long) {
        timerJob?.cancel()
        _remainingSeconds.value = totalSeconds
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L.milliseconds)
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
            currentState.questions.forEach { q ->
                if (q.selectedOptionIndex == q.correctOptionIndex && q.correctOptionIndex != -1) {
                    correctCount++
                }
            }
            val total = currentState.questions.size
            val percentage = if (total > 0) (correctCount.toFloat() / total) * 100 else 0f

            _uiState.value = QuizEngineUiState.Completed(
                totalQuestions = total,
                correctAnswers = correctCount,
                scorePercentage = percentage
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}