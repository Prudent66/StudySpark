package com.studyspark.app.ui.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.data.repository.GeminiRepository
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.DraftQuestion
import com.studyspark.app.domain.model.Quiz
import com.studyspark.app.utils.PdfGenerator
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

data class PdfImportUiState(
    val fileName: String? = null,
    val extractedText: String = "",
    val isLoadingPdf: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val isGeneratingPdf: Boolean = false,
    val generatedQuestions: List<DraftQuestion> = emptyList(),
    val errorMessage: String? = null,
    val questionType: String = "Objectives", 
    val numberOfQuestions: Int = 5,
    
    // PDF Header State
    val showHeaderDialog: Boolean = false,
    val schoolName: String = "StudySpark University",
    val examTitle: String = "End of Semester Examination",
    val courseCode: String = "Course Code: CS101",
    val instructions: String = "Answer all questions. Each question carries equal marks.",
    
    // Resulting PDF File
    val generatedPdfFile: File? = null
) {
    val maxQuestionsAllowed: Int get() = if (questionType == "Essay") 10 else 100
}

class PdfImportViewModel(
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfImportUiState())
    val uiState: StateFlow<PdfImportUiState> = _uiState.asStateFlow()
    
    private val geminiRepository = GeminiRepository()

    fun updateQuestionType(type: String) {
        val currentCount = _uiState.value.numberOfQuestions
        val max = if (type == "Essay") 10 else 100
        _uiState.value = _uiState.value.copy(
            questionType = type,
            numberOfQuestions = currentCount.coerceAtMost(max)
        )
    }

    fun updateNumberOfQuestions(count: Int) {
        _uiState.value = _uiState.value.copy(
            numberOfQuestions = count.coerceIn(1, _uiState.value.maxQuestionsAllowed)
        )
    }

    // Dialog State Updates
    fun showHeaderDialog(show: Boolean) { _uiState.value = _uiState.value.copy(showHeaderDialog = show) }
    fun updateSchoolName(name: String) { _uiState.value = _uiState.value.copy(schoolName = name) }
    fun updateExamTitle(title: String) { _uiState.value = _uiState.value.copy(examTitle = title) }
    fun updateCourseCode(code: String) { _uiState.value = _uiState.value.copy(courseCode = code) }
    fun updateInstructions(inst: String) { _uiState.value = _uiState.value.copy(instructions = inst) }

    fun extractTextFromPdf(context: Context, uri: Uri, fileName: String) {
        _uiState.value = _uiState.value.copy(isLoadingPdf = true, fileName = fileName, errorMessage = null)

        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    val tempFile = File.createTempFile("import_", ".pdf", context.cacheDir)
                    try {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
                        } ?: throw Exception("Could not open file stream")

                        PDDocument.load(tempFile).use { document ->
                            val stripper = PDFTextStripper()
                            stripper.getText(document)
                        }
                    } finally { tempFile.delete() }
                }

                if (text.isBlank()) {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "No selectable text found.")
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, extractedText = text.trim())
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "Failed to read PDF: ${e.message}")
            }
        }
    }

    fun generateQuestionsWithAi() {
        val state = _uiState.value
        if (state.extractedText.isBlank()) return

        _uiState.value = state.copy(isGeneratingAi = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val questions = withTimeoutOrNull(90_000L) { // Increased timeout for up to 100 questions
                    geminiRepository.generateQuestionsFromText(
                        extractedText = state.extractedText,
                        numberOfQuestions = state.numberOfQuestions,
                        questionType = state.questionType 
                    )
                }

                if (questions != null) {
                    _uiState.value = _uiState.value.copy(isGeneratingAi = false, generatedQuestions = questions)
                } else {
                    _uiState.value = _uiState.value.copy(isGeneratingAi = false, errorMessage = "Request timed out. Please try fewer questions.")
                }
            } catch (e: CancellationException) {
                _uiState.value = _uiState.value.copy(isGeneratingAi = false)
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGeneratingAi = false, errorMessage = e.message ?: "Failed to generate.")
            }
        }
    }

    fun generatePdf(context: Context) {
        val state = _uiState.value
        if (state.generatedQuestions.isEmpty()) return

        _uiState.value = state.copy(isGeneratingPdf = true, showHeaderDialog = false)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdfFile = PdfGenerator.generateQuizPdf(
                    context = context,
                    questions = state.generatedQuestions,
                    schoolName = state.schoolName,
                    examTitle = state.examTitle,
                    courseCode = state.courseCode,
                    instructions = state.instructions
                )
                _uiState.value = _uiState.value.copy(isGeneratingPdf = false, generatedPdfFile = pdfFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isGeneratingPdf = false, errorMessage = "PDF generation failed: ${e.message}")
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

    fun clearState() { _uiState.value = PdfImportUiState() }
}
