package com.studyspark.app.domain.scoring

import com.studyspark.app.domain.model.Question
import com.studyspark.app.domain.model.QuestionResult
import com.studyspark.app.domain.model.QuizResult

object QuizScorer {

    fun score(
        quizId: String,
        questions: List<Question>,
        answers: Map<String, Int?>,
        textAnswers: Map<String, String> = emptyMap() // NEW
    ): QuizResult {
        val questionResults = questions.map { question ->
            val selected = answers[question.id]
            val textAns = textAnswers[question.id]
            
            val isCorrect = if (question.questionType == "Essay") {
                // Give credit for essays if they typed a reasonable answer (e.g., > 5 chars)
                !textAns.isNullOrBlank() && textAns.length > 5 
            } else {
                selected != null && selected == question.correctOptionIndex
            }

            QuestionResult(
                question = question,
                selectedOptionIndex = selected,
                textAnswer = textAns,
                isCorrect = isCorrect
            )
        }

        val correct = questionResults.count { it.isCorrect }
        val unanswered = questionResults.count { 
            if (it.question.questionType == "Essay") it.textAnswer.isNullOrBlank()
            else it.selectedOptionIndex == null 
        }
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
