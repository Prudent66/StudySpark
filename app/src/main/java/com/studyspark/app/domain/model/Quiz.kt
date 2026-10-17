package com.studyspark.app.domain.model

data class Quiz(
    val id: String,
    val title: String,
    val sourceLabel: String? = null,
    val questions: List<Question>,
    val timeLimitSeconds: Int? = null
) {
    init {
        require(title.isNotBlank()) { "Quiz title cannot be empty." }
        // Note: We removed the `questions.isNotEmpty()` check here.
        // The UI/ViewModel layer ensures a quiz has questions before saving.
        require(timeLimitSeconds == null || timeLimitSeconds > 0) { "Time limit must be positive." }
    }
}
