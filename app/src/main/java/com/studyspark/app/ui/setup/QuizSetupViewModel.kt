package com.studyspark.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.Quiz
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizSetupUiState(
    val quiz: Quiz? = null,
    val isLoading: Boolean = true,
    val questionCount: Int = 0,
    val isTimed: Boolean = false,
    val timeLimitMinutes: Int = 10,
    val randomizeQuestions: Boolean = false,
    val errorMessage: String? = null
) {
    val maxQuestions: Int get() = quiz?.questions?.size ?: 0
    val canStart: Boolean get() = quiz != null && questionCount > 0
}

class QuizSetupViewModel(
    private val repository: QuizRepository,
    private val quizId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizSetupUiState())
    val uiState: StateFlow<QuizSetupUiState> = _uiState.asStateFlow()

    init {
        loadQuiz()
    }

    private fun loadQuiz() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val quiz = repository.getQuiz(quizId)
            if (quiz == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Quiz not found"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    quiz = quiz,
                    isLoading = false,
                    questionCount = quiz.questions.size
                )
            }
        }
    }

    fun updateQuestionCount(count: Int) {
        _uiState.value = _uiState.value.copy(
            questionCount = count.coerceIn(1, _uiState.value.maxQuestions)
        )
    }

    fun toggleTimed(isTimed: Boolean) {
        _uiState.value = _uiState.value.copy(isTimed = isTimed)
    }

    fun updateTimeLimitMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(
            timeLimitMinutes = minutes.coerceIn(1, 120)
        )
    }

    fun toggleRandomize(randomize: Boolean) {
        _uiState.value = _uiState.value.copy(randomizeQuestions = randomize)
    }
}
