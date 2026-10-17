package com.studyspark.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizDao {

    // NEW: Returns quizzes with their questions attached
    @Transaction
    @Query("SELECT * FROM quizzes ORDER BY updatedAt DESC")
    fun observeAllQuizzesWithQuestions(): Flow<List<QuizWithQuestions>>

    @Query("SELECT * FROM quizzes WHERE id = :quizId LIMIT 1")
    suspend fun getQuiz(quizId: String): QuizEntity?

    @Query("SELECT * FROM questions WHERE quizId = :quizId ORDER BY orderIndex ASC")
    suspend fun getQuestionsForQuiz(quizId: String): List<QuestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuiz(quiz: QuizEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE quizId = :quizId")
    suspend fun deleteQuestionsForQuiz(quizId: String)

    @Query("DELETE FROM quizzes WHERE id = :quizId")
    suspend fun deleteQuiz(quizId: String)

    @Transaction
    suspend fun saveQuizWithQuestions(quiz: QuizEntity, questions: List<QuestionEntity>) {
        upsertQuiz(quiz)
        deleteQuestionsForQuiz(quiz.id)
        upsertQuestions(questions)
    }
}
