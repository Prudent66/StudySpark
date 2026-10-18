package com.studyspark.app.domain.model

data class Question(
    val id: String,
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val explanation: String? = null,
    val questionType: String = "Objectives" // NEW
) {
    init {
        require(text.isNotBlank()) { "Question text cannot be empty." }
        if (questionType == "Objectives") {
            require(options.size == 4) { "An objective question must have exactly 4 options." }
            require(correctOptionIndex in 0..3) { "Correct option index must be between 0 and 3." }
        }
    }
}
