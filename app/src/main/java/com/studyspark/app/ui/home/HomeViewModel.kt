package com.studyspark.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.Quiz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val savedQuizzes: List<Quiz> = emptyList()
)

class HomeViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeQuizzes()
    }

    private fun observeQuizzes() {
        viewModelScope.launch {
            repository.observeAllQuizzes().collectLatest { quizzes ->
                _uiState.value = _uiState.value.copy(savedQuizzes = quizzes)
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        viewModelScope.launch {
            repository.deleteQuiz(quizId)
        }
    }
}
