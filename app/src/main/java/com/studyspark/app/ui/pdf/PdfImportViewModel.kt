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
    val errorMessage: String? = null
)

class PdfImportViewModel(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfImportUiState())
    val uiState: StateFlow<PdfImportUiState> = _uiState.asStateFlow()
    
    private val geminiRepository = GeminiRepository()

    fun extractTextFromPdf(context: Context, uri: Uri, fileName: String) {
        _uiState.value = _uiState.value.copy(isLoadingPdf = true, fileName = fileName, errorMessage = null)

        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    // Create a temporary file to safely copy the PDF into
                    val tempFile = File.createTempFile("import_", ".pdf", context.cacheDir)
                    
                    try {
                        // 1. Copy the URI data to our temp file
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        } ?: throw Exception("Could not open file stream")

                        // 2. Read the temp file with PDFBox
                        PDDocument.load(tempFile).use { document ->
                            val stripper = PDFTextStripper()
                            stripper.getText(document)
                        }
                    } finally {
                        // 3. Always delete the temp file to save space
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

    fun generateQuestionsWithAi(numberOfQuestions: Int = 5) {
        val text = _uiState.value.extractedText
        if (text.isBlank()) return

        _uiState.value = _uiState.value.copy(isGeneratingAi = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val questions = geminiRepository.generateQuestionsFromText(text, numberOfQuestions)
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
