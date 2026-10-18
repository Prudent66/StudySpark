package com.studyspark.app.domain.model

data class QuizResult(
    val quizId: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val scorePercentage: Float,
    val questionResults: List<QuestionResult>
)

data class QuestionResult(
    val question: Question,
    val selectedOptionIndex: Int?,   // For MCQs
    val textAnswer: String? = null,  // NEW: For Essays
    val isCorrect: Boolean
)
