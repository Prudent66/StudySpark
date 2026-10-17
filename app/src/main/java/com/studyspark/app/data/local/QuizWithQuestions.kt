package com.studyspark.app.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room relation class to load a QuizEntity along with its QuestionEntities.
 */
data class QuizWithQuestions(
    @Embedded val quiz: QuizEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "quizId"
    )
    val questions: List<QuestionEntity>
)
