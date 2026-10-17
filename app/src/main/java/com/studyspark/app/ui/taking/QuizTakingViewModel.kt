package com.studyspark.app.ui.taking

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
    val timeLeftSeconds: Int? = null,
    val isTimerRunning: Boolean = false,
    val showFinishDialog: Boolean = false,
    val isSubmitted: Boolean = false,
    val result: QuizResult? = null,
    val errorMessage: String? = null
) {
    val currentQuestion: Question?
        get() = activeQuestions.getOrNull(currentIndex)
        
    val answeredCount: Int
        get() = answers.count { it.value != null }
        
    val progress: Float
        get() = if (activeQuestions.isEmpty()) 0f
                else answeredCount.toFloat() / activeQuestions.size
}

class QuizTakingViewModel(
    private val repository: QuizRepository,
    private val quizId: String,
    private val requestedQuestionCount: Int,
    private val timeLimitSeconds: Int?,
    private val randomize: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizTakingUiState(timeLeftSeconds = timeLimitSeconds))
    val uiState: StateFlow<QuizTakingUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadAndSetupQuiz()
    }

    private fun loadAndSetupQuiz() {
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

            if (timeLimitSeconds != null) {
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
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

    fun selectAnswer(questionId: String, optionIndex: Int) {
        if (_uiState.value.isSubmitted) return
        val updatedAnswers = _uiState.value.answers.toMutableMap()
        updatedAnswers[questionId] = optionIndex
        _uiState.value = _uiState.value.copy(answers = updatedAnswers)
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (state.currentIndex < state.activeQuestions.size - 1) {
            _uiState.value = state.copy(currentIndex = state.currentIndex + 1)
        }
    }

    fun previousQuestion() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1)
        }
    }

    fun goToQuestion(index: Int) {
        if (index in _uiState.value.activeQuestions.indices) {
            _uiState.value = _uiState.value.copy(currentIndex = index)
        }
    }

    fun requestFinish() {
        _uiState.value = _uiState.value.copy(showFinishDialog = true)
    }

    fun dismissFinishDialog() {
        _uiState.value = _uiState.value.copy(showFinishDialog = false)
    }

    fun submitQuiz() {
        val state = _uiState.value
        val quiz = state.quiz ?: return
        
        timerJob?.cancel()

        val result = QuizScorer.score(
            quizId = quiz.id,
            questions = state.activeQuestions,
            answers = state.answers
        )

        _uiState.value = state.copy(
            isSubmitted = true,
            showFinishDialog = false,
            isTimerRunning = false,
            result = result
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
