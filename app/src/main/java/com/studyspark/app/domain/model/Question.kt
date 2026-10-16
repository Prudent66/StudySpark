package com.studyspark.app.domain.model

/**
 * Represents a single multiple-choice question.
 *
 * @param id Unique identifier for the question.
 * @param text The question prompt.
 * @param options Exactly four answer options (A, B, C, D).
 * @param correctOptionIndex The index (0-3) of the correct answer.
 * @param explanation Optional explanation grounded in the source handout.
 */
data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String? = null
) {
    init {
        require(options.size == 4) { "A question must have exactly 4 options." }
        require(correctOptionIndex in 0..3) { "Correct option index must be between 0 and 3." }
        require(text.isNotBlank()) { "Question text cannot be empty." }
        require(options.all { it.isNotBlank() }) { "No answer option can be empty." }
    }
}
