package com.studyspark.app.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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
import kotlinx.coroutines.tasks.await
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
    val numberOfQuestions: Int = 3, 
    val numberOfParts: Int = 2,     
    val numberOfSubParts: Int = 2,  
    
    val showHeaderDialog: Boolean = false,
    val schoolName: String = "StudySpark University",
    val department: String = "Department of Computer Science",
    val courseTitle: String = "Course Title: Introduction to Computing",
    val timeAllowed: String = "Time Allowed: 2 Hours",
    val examDate: String = "Date: October 2026",
    val examTitle: String = "End of Semester Examination",
    val instructions: String = "Answer all questions. Each question carries equal marks.",
    
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

    fun updateNumberOfQuestions(count: Int) { _uiState.value = _uiState.value.copy(numberOfQuestions = count.coerceIn(1, _uiState.value.maxQuestionsAllowed)) }
    fun updateNumberOfParts(count: Int) { _uiState.value = _uiState.value.copy(numberOfParts = count.coerceIn(1, 5)) }
    fun updateNumberOfSubParts(count: Int) { _uiState.value = _uiState.value.copy(numberOfSubParts = count.coerceIn(1, 5)) }

    fun showHeaderDialog(show: Boolean) { _uiState.value = _uiState.value.copy(showHeaderDialog = show) }
    fun updateSchoolName(name: String) { _uiState.value = _uiState.value.copy(schoolName = name) }
    fun updateDepartment(dept: String) { _uiState.value = _uiState.value.copy(department = dept) }
    fun updateCourseTitle(title: String) { _uiState.value = _uiState.value.copy(courseTitle = title) }
    fun updateTimeAllowed(time: String) { _uiState.value = _uiState.value.copy(timeAllowed = time) }
    fun updateExamDate(date: String) { _uiState.value = _uiState.value.copy(examDate = date) }
    fun updateExamTitle(title: String) { _uiState.value = _uiState.value.copy(examTitle = title) }
    fun updateInstructions(inst: String) { _uiState.value = _uiState.value.copy(instructions = inst) }

    // --- PDF EXTRACTION ---
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

                        var extracted = ""
                        PDDocument.load(tempFile).use { document ->
                            val stripper = PDFTextStripper()
                            extracted = stripper.getText(document)
                        }

                        if (isLikelyImagePdf(extracted)) {
                            extractTextFromPdfOcr(tempFile)
                        } else {
                            extracted
                        }
                    } finally { tempFile.delete() }
                }

                if (text.isBlank()) {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "No text found even after OCR.")
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, extractedText = text.trim())
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "Failed to read PDF: ${e.message}")
            }
        }
    }

    private fun isLikelyImagePdf(text: String): Boolean {
        val words = text.split(Regex("\\s+"))
        if (words.size < 20) return true
        val mostFrequent = words.groupingBy { it.lowercase() }.eachCount().maxByOrNull { it.value }
        return mostFrequent != null && mostFrequent.value > words.size * 0.3
    }

    private suspend fun extractTextFromPdfOcr(pdfFile: File): String {
        val builder = StringBuilder()
        val descriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(descriptor)

        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                builder.append(performOcr(bitmap)).append("\n")
            }
        } finally {
            renderer.close()
            descriptor.close()
        }
        return builder.toString()
    }

    // --- CAMERA EXTRACTION (NEW) ---
    fun extractTextFromImage(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(isLoadingPdf = true, fileName = "Camera_Photo", errorMessage = null)
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    performOcr(bitmap)
                }
                if (text.isBlank()) {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "No text found in the image.")
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingPdf = false, extractedText = text.trim())
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingPdf = false, errorMessage = "Failed to read image: ${e.message}")
            }
        }
    }

    // --- REUSABLE OCR FUNCTION ---
    private suspend fun performOcr(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizer.process(image).await().text
    }

    // --- AI GENERATION ---
    fun generateQuestionsWithAi() {
        val state = _uiState.value
        if (state.extractedText.isBlank()) return

        _uiState.value = state.copy(isGeneratingAi = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val questions = withTimeoutOrNull(90_000L) { 
                    geminiRepository.generateQuestionsFromText(
                        extractedText = state.extractedText,
                        numberOfQuestions = state.numberOfQuestions,
                        numberOfParts = state.numberOfParts,
                        numberOfSubParts = state.numberOfSubParts,
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
                    context, state.generatedQuestions, state.schoolName, state.department,
                    state.courseTitle, state.timeAllowed, state.examDate, state.examTitle, state.instructions
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
