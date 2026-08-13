package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class QuizEngineUiState {
    object Loading : QuizEngineUiState()
    data class Success(
        val metadata: QuizMetadata,
        val questions: List<QuestionItem>
    ) : QuizEngineUiState()
    data class Error(val message: String) : QuizEngineUiState()
    data class Completed(val resultData: QuizResultData) : QuizEngineUiState()
}

data class QuizResultData(
    val correctAnswers: Int,
    val totalQuestions: Int,
    val scorePercentage: Float
)

@HiltViewModel
class QuizEngineViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizEngineUiState>(QuizEngineUiState.Loading)
    val uiState: StateFlow<QuizEngineUiState> = _uiState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private var currentQuestions = mutableListOf<QuestionItem>()

    fun loadQuiz(quizId: String) {
        viewModelScope.launch {
            _uiState.value = QuizEngineUiState.Loading
            try {
                val (metadata, questions) = repository.fetchQuizMetadataAndQuestions(quizId)
                currentQuestions = questions.toMutableList()
                _remainingSeconds.value = metadata.totalDurationMinutes * 60
                _uiState.value = QuizEngineUiState.Success(metadata, currentQuestions)
            } catch (e: Exception) {
                _uiState.value = QuizEngineUiState.Error(
                    e.localizedMessage ?: "Quiz metadata not found"
                )
            }
        }
    }

    fun selectOption(questionIndex: Int, optionIndex: Int) {
        if (questionIndex in currentQuestions.indices) {
            val updatedItem = currentQuestions[questionIndex].copy(
                selectedOptionIndex = optionIndex
            )
            currentQuestions[questionIndex] = updatedItem

            val currentState = _uiState.value
            if (currentState is QuizEngineUiState.Success) {
                _uiState.value = currentState.copy(questions = currentQuestions.toList())
            }
        }
    }

    fun submitQuiz() {
        val total = currentQuestions.size
        val answered = currentQuestions.count { it.selectedOptionIndex != -1 }
        _uiState.value = QuizEngineUiState.Completed(
            QuizResultData(
                correctAnswers = answered,
                totalQuestions = total,
                scorePercentage = if (total > 0) (answered.toFloat() / total) * 100 else 0f
            )
        )
    }
}