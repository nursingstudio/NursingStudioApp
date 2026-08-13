package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuestionItem
import com.example.nursingstudio.data.model.QuestionPaletteItem
import com.example.nursingstudio.data.model.QuestionStatus
import com.example.nursingstudio.data.model.QuizMetadata
import com.example.nursingstudio.data.model.QuizResultData
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

@HiltViewModel
class QuizEngineViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuizEngineUiState>(QuizEngineUiState.Loading)
    val uiState: StateFlow<QuizEngineUiState> = _uiState.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private var currentQuizId: String = ""
    private var totalDurationSec: Int = 0
    private var currentQuestions = mutableListOf<QuestionItem>()

    fun loadQuiz(quizId: String) {
        currentQuizId = quizId
        viewModelScope.launch {
            _uiState.value = QuizEngineUiState.Loading
            try {
                val (metadata, questions) = repository.fetchQuizMetadataAndQuestions(quizId)
                currentQuestions = questions.toMutableList()
                totalDurationSec = metadata.totalDurationMinutes * 60
                _remainingSeconds.value = totalDurationSec
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

    fun toggleMarkForReview(questionIndex: Int) {
        if (questionIndex in currentQuestions.indices) {
            val item = currentQuestions[questionIndex]
            currentQuestions[questionIndex] = item.copy(isMarkedForReview = !item.isMarkedForReview)

            val currentState = _uiState.value
            if (currentState is QuizEngineUiState.Success) {
                _uiState.value = currentState.copy(questions = currentQuestions.toList())
            }
        }
    }

    /**
     * Generates active Palette state list for BottomSheet UI
     */
    fun getPaletteItems(currentPosition: Int): List<QuestionPaletteItem> {
        return currentQuestions.mapIndexed { index, item ->
            val status = when {
                item.isMarkedForReview -> QuestionStatus.REVIEW
                item.selectedOptionIndex != -1 -> QuestionStatus.ANSWERED
                else -> QuestionStatus.UNANSWERED
            }
            QuestionPaletteItem(
                questionIndex = index,
                displayIndex = index + 1,
                status = status,
                isCurrent = index == currentPosition
            )
        }
    }

    /**
     * Calculates NORCET Standard Results (+1.0 Correct, -0.25 Incorrect)
     */
    fun submitQuiz() {
        var correct = 0
        var incorrect = 0
        var attempted = 0

        currentQuestions.forEach { item ->
            if (item.selectedOptionIndex != -1) {
                attempted++
                if (item.selectedOptionIndex == item.correctOptionIndex) {
                    correct++
                } else {
                    incorrect++
                }
            }
        }

        val totalQuestions = currentQuestions.size
        val unattempted = totalQuestions - attempted
        val totalMarks = totalQuestions * 1.0f
        val finalScore = (correct * 1.0f) - (incorrect * 0.25f)
        val percentage = if (totalMarks > 0) (finalScore / totalMarks) * 100f else 0f
        val timeTaken = totalDurationSec - _remainingSeconds.value

        val resultData = QuizResultData(
            quizId = currentQuizId,
            isPassed = percentage >= 50f,
            finalScore = if (finalScore < 0f) 0f else finalScore,
            totalPossibleMarks = totalMarks,
            scorePercentage = if (percentage < 0f) 0f else percentage,
            globalRank = 1,
            totalParticipants = 100,
            timeTakenSeconds = timeTaken,
            totalDurationSeconds = totalDurationSec,
            attemptedQuestions = attempted,
            unattemptedQuestions = unattempted,
            correctAnswers = correct,
            incorrectAnswers = incorrect
        )

        _uiState.value = QuizEngineUiState.Completed(resultData)
    }
}