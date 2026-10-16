package com.studyspark.app.domain.model

import java.util.UUID

/**
 * A mutable, unvalidated version of a Question used during editing.
 * It allows empty fields so users can type freely.
 * Converted to a valid [Question] only when saving.
 */
data class DraftQuestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val explanation: String = ""
) {
    /**
     * Returns true if this draft satisfies all the rules required to become a Question.
     */
    fun isValid(): Boolean =
        text.isNotBlank() && options.all { it.isNotBlank() }

    /**
     * Converts to a real Question. Only call this if [isValid] returns true.
     */
    fun toQuestion(): Question = Question(
        id = id,
        text = text.trim(),
        options = options.map { it.trim() },
        correctOptionIndex = correctOptionIndex,
        explanation = explanation.trim().ifBlank { null }
    )

    companion object {
        fun fromQuestion(question: Question): DraftQuestion = DraftQuestion(
            id = question.id,
            text = question.text,
            options = question.options,
            correctOptionIndex = question.correctOptionIndex,
            explanation = question.explanation ?: ""
        )
    }
}
