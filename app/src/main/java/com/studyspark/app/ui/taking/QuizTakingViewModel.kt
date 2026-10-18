package com.studyspark.app.ui.taking

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyspark.app.data.repository.QuizRepository
import com.studyspark.app.domain.model.Question
import com.studyspark.app.domain.model.Quiz
import com.studyspark.app.domain.model.QuizResult
import com.studyspark.app.domain.scoring.QuizScorer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QuizTakingUiState(
    val quiz: Quiz? = null,
    val activeQuestions: List<Question> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, Int?> = emptyMap(),
    val essayAnswers: Map<String, String> = emptyMap(), // NEW
    val timeLeftSeconds: Int? = null,
    val isTimerRunning: Boolean = false,
    val showFinishDialog: Boolean = false,
    val isSubmitted: Boolean = false,
    val result: QuizResult? = null,
    val errorMessage: String? = null,
    val hasLeftApp: Boolean = false,
    val showWarningDialog: Boolean = true
) {
    val currentQuestion: Question?
        get() = activeQuestions.getOrNull(currentIndex)
        
    val answeredCount: Int
        get() = activeQuestions.count { 
            if (it.questionType == "Essay") !essayAnswers[it.id].isNullOrBlank()
            else answers[it.id] != null
        }
        
    val progress: Float
        get() = if (activeQuestions.isEmpty()) 0f
                else answeredCount.toFloat() / activeQuestions.size
}

class QuizTakingViewModel(
    private val repository: QuizRepository,
    private val quizId: String,
    private val requestedQuestionCount: Int,
    private val timeLimitSeconds: Int?,
    private val randomize: Boolean,
    private val prefs: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizTakingUiState(timeLeftSeconds = timeLimitSeconds))
    val uiState: StateFlow<QuizTakingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private val abandonedKey = "abandoned_$quizId"

    init {
        if (prefs.getBoolean(abandonedKey, false)) {
            forceSubmitAbandonedQuiz()
        } else {
            loadAndSetupQuiz()
        }
    }

    private fun forceSubmitAbandonedQuiz() {
        viewModelScope.launch {
            val fullQuiz = repository.getQuiz(quizId)
            if (fullQuiz != null) {
                var workingQuestions = if (randomize) fullQuiz.questions.shuffled() else fullQuiz.questions
                if (requestedQuestionCount < workingQuestions.size) {
                    workingQuestions = workingQuestions.take(requestedQuestionCount)
                }
                val result = QuizScorer.score(quizId, workingQuestions, emptyMap(), emptyMap())
                _uiState.value = _uiState.value.copy(
                    quiz = fullQuiz,
                    activeQuestions = workingQuestions,
                    isSubmitted = true,
                    result = result
                )
            }
        }
    }

    private fun loadAndSetupQuiz() {
        prefs.edit().putBoolean(abandonedKey, false).apply()
        viewModelScope.launch {
            val fullQuiz = repository.getQuiz(quizId)
            if (fullQuiz == null) {
                _uiState.value = _uiState.value.copy(errorMessage = "Quiz not found.")
                return@launch
            }
            var workingQuestions = if (randomize) fullQuiz.questions.shuffled() else fullQuiz.questions
            if (requestedQuestionCount < workingQuestions.size) {
                workingQuestions = workingQuestions.take(requestedQuestionCount)
            }
            _uiState.value = _uiState.value.copy(
                quiz = fullQuiz,
                activeQuestions = workingQuestions,
                isTimerRunning = timeLimitSeconds != null
            )
            if (timeLimitSeconds != null) startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_uiState.value.showWarningDialog) continue 
                val current = _uiState.value.timeLeftSeconds ?: break
                if (current <= 1) {
                    _uiState.value = _uiState.value.copy(timeLeftSeconds = 0, isTimerRunning = false)
                    submitQuiz()
                    break
                } else {
                    _uiState.value = _uiState.value.copy(timeLeftSeconds = current - 1)
                }
            }
        }
    }

    fun acknowledgeWarning() { _uiState.value = _uiState.value.copy(showWarningDialog = false) }

    fun selectAnswer(questionId: String, optionIndex: Int) {
        if (_uiState.value.isSubmitted) return
        val updatedAnswers = _uiState.value.answers.toMutableMap()
        updatedAnswers[questionId] = optionIndex
        _uiState.value = _uiState.value.copy(answers = updatedAnswers)
    }

    fun selectEssayAnswer(questionId: String, text: String) {
        if (_uiState.value.isSubmitted) return
        val updatedAnswers = _uiState.value.essayAnswers.toMutableMap()
        updatedAnswers[questionId] = text
        _uiState.value = _uiState.value.copy(essayAnswers = updatedAnswers)
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.activeQuestions.size - 1) _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
    }

    fun goToQuestion(index: Int) {
        if (index in _uiState.value.activeQuestions.indices) _uiState.value = _uiState.value.copy(currentIndex = index)
    }

    fun requestFinish() { _uiState.value = _uiState.value.copy(showFinishDialog = true) }
    fun dismissFinishDialog() { _uiState.value = _uiState.value.copy(showFinishDialog = false) }

    fun onAppBackgrounded() {
        if (!_uiState.value.isSubmitted && !_uiState.value.showWarningDialog) {
            prefs.edit().putBoolean(abandonedKey, true).apply()
            _uiState.value = _uiState.value.copy(hasLeftApp = true)
            submitQuiz() 
        }
    }

    fun submitQuiz() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        timerJob?.cancel()

        val result = QuizScorer.score(
            quizId = quizId, 
            questions = state.activeQuestions, 
            answers = state.answers,
            textAnswers = state.essayAnswers // Pass essays
        )

        _uiState.value = state.copy(
            isSubmitted = true,
            showFinishDialog = false,
            showWarningDialog = false,
            isTimerRunning = false,
            result = result
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
