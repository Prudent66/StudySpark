package com.studyspark.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.domain.model.DraftQuestion
import com.studyspark.app.domain.model.Quiz
import com.studyspark.app.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ManualEditorUiState(
    val quizId: String? = null, // Null means we are creating a new quiz
    val quizTitle: String = "",
    val questions: List<DraftQuestion> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val isEditing: Boolean get() = quizId != null
    val canSave: Boolean
        get() = quizTitle.isNotBlank() &&
                questions.isNotEmpty() &&
                questions.all { it.isValid() } &&
                !isSaving
}

class ManualEditorViewModel(
    private val repository: QuizRepository,
    initialQuizId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEditorUiState(quizId = initialQuizId))
    val uiState: StateFlow<ManualEditorUiState> = _uiState.asStateFlow()

    init {
        if (initialQuizId != null) {
            loadExistingQuiz(initialQuizId)
        }
    }

    private fun loadExistingQuiz(quizId: String) {
        viewModelScope.launch {
            val quiz = repository.getQuiz(quizId)
            if (quiz != null) {
                _uiState.value = _uiState.value.copy(
                    quizId = quiz.id,
                    quizTitle = quiz.title,
                    questions = quiz.questions.map { DraftQuestion.fromQuestion(it) }
                )
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(quizTitle = newTitle, errorMessage = null)
    }

    fun addQuestion() {
        _uiState.value = _uiState.value.copy(
            questions = _uiState.value.questions + DraftQuestion(),
            errorMessage = null
        )
    }

    fun updateQuestion(updated: DraftQuestion) {
        _uiState.value = _uiState.value.copy(
            questions = _uiState.value.questions.map { if (it.id == updated.id) updated else it },
            errorMessage = null
        )
    }

    fun deleteQuestion(questionId: String) {
        _uiState.value = _uiState.value.copy(
            questions = _uiState.value.questions.filterNot { it.id == questionId }
        )
    }

    fun save() {
        val state = _uiState.value

        if (state.quizTitle.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter a quiz title.")
            return
        }
        if (state.questions.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Add at least one question.")
            return
        }
        if (state.questions.any { !it.isValid() }) {
            _uiState.value = state.copy(errorMessage = "Fill in every question and all four options.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            try {
                val quiz = Quiz(
                    id = state.quizId ?: UUID.randomUUID().toString(), // Keep existing ID or create new
                    title = state.quizTitle.trim(),
                    sourceLabel = "Manual",
                    questions = state.questions.map { it.toQuestion() },
                    timeLimitSeconds = null // Keep null here, setup screen handles time
                )
                repository.saveQuiz(quiz)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    savedSuccessfully = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save: ${e.message}"
                )
            }
        }
    }
}
