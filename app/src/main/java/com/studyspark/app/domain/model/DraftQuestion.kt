package com.studyspark.app.domain.model

import java.util.UUID

data class DraftQuestion(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctOptionIndex: Int = 0,
    val explanation: String = "",
    val questionType: String = "Objectives" // NEW
) {
    fun isValid(): Boolean = if (questionType == "Essay") {
        text.isNotBlank()
    } else {
        text.isNotBlank() && options.all { it.isNotBlank() }
    }

    fun toQuestion(): Question = Question(
        id = id,
        text = text.trim(),
        options = if (questionType == "Essay") emptyList() else options.map { it.trim() },
        correctOptionIndex = correctOptionIndex,
        explanation = explanation.trim().ifBlank { null },
        questionType = questionType
    )

    companion object {
        fun fromQuestion(question: Question): DraftQuestion = DraftQuestion(
            id = question.id,
            text = question.text,
            options = if (question.questionType == "Essay") listOf("", "", "", "") else question.options,
            correctOptionIndex = question.correctOptionIndex,
            explanation = question.explanation ?: "",
            questionType = question.questionType
        )
    }
}
