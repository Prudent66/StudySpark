package com.studyspark.app.data.repository

import com.studyspark.app.data.local.QuestionEntity
import com.studyspark.app.data.local.QuizDao
import com.studyspark.app.data.local.QuizEntity
import com.studyspark.app.domain.model.Question
import com.studyspark.app.domain.model.Quiz
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class QuizRepository(private val quizDao: QuizDao) {

    fun observeAllQuizzes(): Flow<List<Quiz>> =
        quizDao.observeAllQuizzesWithQuestions().map { list ->
            list.map { it.quiz.toDomain(it.questions.map { q -> q.toDomain() }) }
        }

    suspend fun getQuiz(quizId: String): Quiz? {
        val quizEntity = quizDao.getQuiz(quizId) ?: return null
        val questionEntities = quizDao.getQuestionsForQuiz(quizId)
        return quizEntity.toDomain(questionEntities.map { it.toDomain() })
    }

    suspend fun saveQuiz(quiz: Quiz) {
        val quizEntity = quiz.toEntity()
        val questionEntities = quiz.questions.mapIndexed { index, q ->
            q.toEntity(quizId = quiz.id, orderIndex = index)
        }
        quizDao.saveQuizWithQuestions(quizEntity, questionEntities)
    }

    suspend fun deleteQuiz(quizId: String) {
        quizDao.deleteQuiz(quizId)
    }
}

private fun QuizEntity.toDomain(questions: List<Question>): Quiz = Quiz(
    id = id,
    title = title,
    sourceLabel = sourceLabel,
    questions = questions,
    timeLimitSeconds = timeLimitSeconds
)

private fun QuestionEntity.toDomain(): Question = Question(
    id = id,
    text = text,
    options = if (questionType == "Essay") emptyList() else listOf(optionA, optionB, optionC, optionD),
    correctOptionIndex = correctOptionIndex,
    explanation = explanation,
    questionType = questionType
)

private fun Quiz.toEntity(): QuizEntity {
    val now = System.currentTimeMillis()
    return QuizEntity(
        id = id,
        title = title,
        sourceLabel = sourceLabel,
        timeLimitSeconds = timeLimitSeconds,
        createdAt = now,
        updatedAt = now
    )
}

private fun Question.toEntity(quizId: String, orderIndex: Int): QuestionEntity = QuestionEntity(
    id = id,
    quizId = quizId,
    text = text,
    optionA = options.getOrNull(0) ?: "",
    optionB = options.getOrNull(1) ?: "",
    optionC = options.getOrNull(2) ?: "",
    optionD = options.getOrNull(3) ?: "",
    correctOptionIndex = correctOptionIndex,
    explanation = explanation,
    orderIndex = orderIndex,
    questionType = questionType
)
