package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.TestItem
import com.example.nursingstudio.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TestListUiState {
    object Loading : TestListUiState
    data class Success(val tests: List<TestItem>) : TestListUiState
    data class Error(val message: String) : TestListUiState
}

@HiltViewModel
class TestListViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TestListUiState>(TestListUiState.Loading)
    val uiState: StateFlow<TestListUiState> = _uiState.asStateFlow()

    fun fetchTestsForCategory(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = TestListUiState.Loading
            val result = repository.getTestsByCategory(categoryId)
            result.fold(
                onSuccess = { testList ->
                    if (testList.isEmpty()) {
                        _uiState.value = TestListUiState.Error("No tests found in this category.")
                    } else {
                        _uiState.value = TestListUiState.Success(testList)
                    }
                },
                onFailure = { error ->
                    _uiState.value = TestListUiState.Error(
                        error.localizedMessage ?: "Failed to fetch tests"
                    )
                }
            )
        }
    }
}