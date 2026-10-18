package com.studyspark.app.ui.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.data.repository.GeminiRepository
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.DraftQuestion
import com.studyspark.app.domain.model.Quiz
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class PdfImportUiState(
    val fileName: String? = null,
    val extractedText: String = "",
    val isLoadingPdf: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val generatedQuestions: List<DraftQuestion> = emptyList(),
    val errorMessage: String? = null,
    // Configuration state
    val questionType: String = "Objectives", 
    val numberOfQuestions: Int = 5
)

class PdfImportViewModel(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfImportUiState())
    val uiState: StateFlow<PdfImportUiState> = _uiState.asStateFlow()
    
    private val geminiRepository = GeminiRepository()

    fun updateQuestionType(type: String) {
        _uiState.value = _uiState.value.copy(questionType = type)
    }

    fun updateNumberOfQuestions(count: Int) {
        _uiState.value = _uiState.value.copy(numberOfQuestions = count)
    }

    fun extractTextFromPdf(context: Context, uri: Uri, fileName: String) {
        _uiState.value = _uiState.value.copy(isLoadingPdf = true, fileName = fileName, errorMessage = null)

        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val tempFile = File.createTempFile("import_", ".pdf", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: throw Exception("Could not open file stream")

                        PDDocument.load(tempFile).use { document ->
                            val stripper = PDFTextStripper()
                            stripper.getText(document)
                        }
                    } finally {
                        tempFile.delete()
                    }
                }

                if (text.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPdf = false,
                        errorMessage = "No selectable text found in this PDF. It might be a scanned image."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingPdf = false,
                        extractedText = text.trim()
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPdf = false,
                    errorMessage = "Failed to read PDF: ${e.message}"
                )
            }
        }
    }

    fun generateQuestionsWithAi() {
        val state = _uiState.value
        if (state.extractedText.isBlank()) return

        _uiState.value = state.copy(isGeneratingAi = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val questions = geminiRepository.generateQuestionsFromText(
                    extractedText = state.extractedText,
                    numberOfQuestions = state.numberOfQuestions,
                    questionType = state.questionType // THIS WAS MISSING
                )
                _uiState.value = _uiState.value.copy(
                    isGeneratingAi = false,
                    generatedQuestions = questions
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingAi = false,
                    errorMessage = e.message ?: "Failed to generate questions."
                )
            }
        }
    }

    fun saveAndContinueToEditor(onSuccess: (String) -> Unit) {
        val questions = _uiState.value.generatedQuestions
        if (questions.isEmpty()) return

        viewModelScope.launch {
            try {
                val quizId = UUID.randomUUID().toString()
                val quiz = Quiz(
                    id = quizId,
                    title = _uiState.value.fileName?.replace(".pdf", "") ?: "AI Generated Quiz",
                    sourceLabel = "AI Generated",
                    questions = questions.map { it.toQuestion() }
                )
                quizRepository.saveQuiz(quiz)
                onSuccess(quizId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to save draft: ${e.message}")
            }
        }
    }

    fun clearState() {
        _uiState.value = PdfImportUiState()
    }
}
