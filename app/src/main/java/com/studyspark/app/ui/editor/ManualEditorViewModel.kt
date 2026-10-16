package com.studyspark.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.domain.model.Question
import com.studyspark.app.domain.model.Quiz
import com.studyspark.app.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ManualEditorUiState(
    val quizTitle: String = "",
    val questions: List<Question> = emptyList(),
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean
        get() = quizTitle.isNotBlank() && questions.isNotEmpty() && !isSaving
}

class ManualEditorViewModel(
    private val repository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualEditorUiState())
    val uiState: StateFlow<ManualEditorUiState> = _uiState.asStateFlow()

    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(quizTitle = newTitle, errorMessage = null)
    }

    fun addQuestion(question: Question) {
        _uiState.value = _uiState.value.copy(
            questions = _uiState.value.questions + question,
            errorMessage = null
        )
    }

    fun updateQuestion(updated: Question) {
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
        if (!state.canSave) {
            _uiState.value = state.copy(errorMessage = "Add a title and at least one question.")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            try {
                val quiz = Quiz(
                    id = UUID.randomUUID().toString(),
                    title = state.quizTitle.trim(),
                    sourceLabel = "Manual",
                    questions = state.questions,
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

    fun createBlankQuestion(): Question = Question(
        id = UUID.randomUUID().toString(),
        text = "",
        options = listOf("", "", "", ""),
        correctOptionIndex = 0,
        explanation = null
    )
}
