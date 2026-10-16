package com.studyspark.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved quiz.
 * Note: This is separate from the domain model to keep the domain Android-free.
 */
@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceLabel: String?,
    val timeLimitSeconds: Int?,
    val createdAt: Long,
    val updatedAt: Long
)
