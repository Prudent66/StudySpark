package com.studyspark.app.domain.scoring

import com.studyspark.app.domain.model.Question
import org.junit.Assert.assertEquals
import org.junit.Test

class QuizScorerTest {

    private fun makeQuestion(
        id: String,
        correctIndex: Int = 0
    ) = Question(
        id = id,
        text = "Question $id",
        options = listOf("A", "B", "C", "D"),
        correctOptionIndex = correctIndex
    )

    @Test
    fun `all answers correct returns 100 percent`() {
        val questions = listOf(makeQuestion("q1", 0), makeQuestion("q2", 1))
        val answers = mapOf("q1" to 0, "q2" to 1)

        val result = QuizScorer.score("quiz1", questions, answers)

        assertEquals(2, result.correctCount)
        assertEquals(0, result.incorrectCount)
        assertEquals(0, result.unansweredCount)
        assertEquals(100f, result.scorePercentage, 0.01f)
    }

    @Test
    fun `all answers wrong returns 0 percent`() {
        val questions = listOf(makeQuestion("q1", 0), makeQuestion("q2", 1))
        val answers = mapOf("q1" to 3, "q2" to 3)

        val result = QuizScorer.score("quiz1", questions, answers)

        assertEquals(0, result.correctCount)
        assertEquals(2, result.incorrectCount)
        assertEquals(0, result.unansweredCount)
        assertEquals(0f, result.scorePercentage, 0.01f)
    }

    @Test
    fun `unanswered questions are counted separately`() {
        val questions = listOf(makeQuestion("q1", 0), makeQuestion("q2", 1), makeQuestion("q3", 2))
        val answers = mapOf("q1" to 0, "q2" to null, "q3" to null)

        val result = QuizScorer.score("quiz1", questions, answers)

        assertEquals(1, result.correctCount)
        assertEquals(0, result.incorrectCount)
        assertEquals(2, result.unansweredCount)
        assertEquals(33.33f, result.scorePercentage, 0.1f)
    }

    @Test
    fun `mixed answers are scored correctly`() {
        val questions = listOf(
            makeQuestion("q1", 0),
            makeQuestion("q2", 1),
            makeQuestion("q3", 2),
            makeQuestion("q4", 3)
        )
        // 2 correct, 1 wrong, 1 unanswered
        val answers = mapOf("q1" to 0, "q2" to 3, "q3" to 2, "q4" to null)

        val result = QuizScorer.score("quiz1", questions, answers)

        assertEquals(2, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.unansweredCount)
        assertEquals(50f, result.scorePercentage, 0.01f)
    }

    @Test
    fun `correctOptionIndex outside range is rejected`() {
        try {
            makeQuestion("q1", 99)
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `question with fewer than 4 options is rejected`() {
        try {
            Question(
                id = "q1",
                text = "Bad question",
                options = listOf("A", "B"),
                correctOptionIndex = 0
            )
            assert(false) { "Expected IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }
}
