package com.example.nursingstudio.ui.features.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nursingstudio.data.model.QuizTestItem
import com.example.nursingstudio.data.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TestListUiState {
    object Loading : TestListUiState()
    data class Success(val tests: List<QuizTestItem>) : TestListUiState()
    data class Error(val message: String) : TestListUiState()
}

@HiltViewModel
class TestListViewModel @Inject constructor(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TestListUiState>(TestListUiState.Loading)
    val uiState: StateFlow<TestListUiState> = _uiState.asStateFlow()

    fun loadTests(categoryId: String) {
        viewModelScope.launch {
            _uiState.value = TestListUiState.Loading
            repository.getTestsByCategory(categoryId)
                .onSuccess { tests ->
                    _uiState.value = TestListUiState.Success(tests)
                }
                .onFailure { error ->
                    _uiState.value = TestListUiState.Error(
                        error.localizedMessage ?: "Failed to load test list"
                    )
                }
        }
    }
}