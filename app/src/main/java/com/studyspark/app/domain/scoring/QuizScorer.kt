package com.studyspark.app.domain.scoring

import com.studyspark.app.domain.model.Question
import com.studyspark.app.domain.model.QuestionResult
import com.studyspark.app.domain.model.QuizResult

/**
 * Pure scoring logic for quizzes. No Android dependencies — fully testable.
 */
object QuizScorer {

    /**
     * Scores a quiz given the student's answers.
     *
     * @param quizId The ID of the quiz being scored.
     * @param questions The questions that were asked.
     * @param answers Map of questionId -> selected option index. Null = unanswered.
     */
    fun score(
        quizId: String,
        questions: List<Question>,
        answers: Map<String, Int?>
    ): QuizResult {
        val questionResults = questions.map { question ->
            val selected = answers[question.id]
            val isCorrect = selected != null && selected == question.correctOptionIndex
            QuestionResult(
                question = question,
                selectedOptionIndex = selected,
                isCorrect = isCorrect
            )
        }

        val correct = questionResults.count { it.isCorrect }
        val unanswered = questionResults.count { it.selectedOptionIndex == null }
        val incorrect = questionResults.size - correct - unanswered
        val percentage = if (questions.isEmpty()) 0f
            else (correct.toFloat() / questions.size.toFloat()) * 100f

        return QuizResult(
            quizId = quizId,
            totalQuestions = questions.size,
            correctCount = correct,
            incorrectCount = incorrect,
            unansweredCount = unanswered,
            scorePercentage = percentage,
            questionResults = questionResults
        )
    }
}
