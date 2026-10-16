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
    val quizTitle: String = "",
    val questions: List<DraftQuestion> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = quizTitle.isNotBlank() &&
                questions.isNotEmpty() &&
                questions.all { it.isValid() } &&
                !isSaving
}

class ManualEditorViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEditorUiState())
    val uiState: StateFlow<ManualEditorUiState> = _uiState.asStateFlow()

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
        val invalid = state.questions.firstOrNull { !it.isValid() }
        if (invalid != null) {
            _uiState.value = state.copy(
                errorMessage = "Fill in every question and all four options before saving."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            try {
                val quiz = Quiz(
                    id = UUID.randomUUID().toString(),
                    title = state.quizTitle.trim(),
                    sourceLabel = "Manual",
                    questions = state.questions.map { it.toQuestion() },
                    timeLimitSeconds = null
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
