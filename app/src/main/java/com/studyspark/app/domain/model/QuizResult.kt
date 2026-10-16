package com.studyspark.app.domain.model

/**
 * Represents the outcome of a completed quiz attempt.
 */
data class QuizResult(
    val quizId: String,
    val totalQuestions: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val scorePercentage: Float,
    val questionResults: List<QuestionResult>
)

/**
 * The per-question outcome of a quiz attempt.
 */
data class QuestionResult(
    val question: Question,
    val selectedOptionIndex: Int?,   // Null = unanswered
    val isCorrect: Boolean
)
