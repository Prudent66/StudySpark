package com.studyspark.app.domain.model

/**
 * Represents a full quiz that a student can take.
 *
 * @param id Unique identifier for the quiz.
 * @param title The name of the quiz.
 * @param sourceLabel Where the quiz came from (e.g., "Chapter 3 PDF" or "Manual").
 * @param questions All available questions in the quiz.
 * @param timeLimitSeconds Optional time limit. Null means untimed.
 */
data class Quiz(
    val id: String,
    val title: String,
    val sourceLabel: String? = null,
    val questions: List<Question>,
    val timeLimitSeconds: Int? = null
) {
    init {
        require(title.isNotBlank()) { "Quiz title cannot be empty." }
        require(questions.isNotEmpty()) { "A quiz must have at least one question." }
        require(timeLimitSeconds == null || timeLimitSeconds > 0) { "Time limit must be positive." }
    }
}
