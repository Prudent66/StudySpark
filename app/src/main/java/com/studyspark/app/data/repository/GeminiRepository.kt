package com.studyspark.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.studyspark.app.BuildConfig
import com.studyspark.app.domain.model.DraftQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiRepository {

    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 32
            topP = 1f
            maxOutputTokens = 4096
            responseMimeType = "application/json"
        }
    )

    suspend fun generateQuestionsFromText(
        extractedText: String,
        numberOfQuestions: Int = 5
    ): List<DraftQuestion> = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert educator. Based on the following text, generate exactly $numberOfQuestions multiple-choice questions.
            Return the response ONLY as a JSON array of objects. Do not include markdown formatting.
            Each object must have these exact keys: "question", "options" (array of 4 strings), "correctIndex" (0-3), and "explanation".
            
            Text:
            $extractedText
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text ?: throw Exception("Empty response")

            val jsonString = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val questions = mutableListOf<DraftQuestion>()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsArray = obj.getJSONArray("options")
                val options = List(4) { optionsArray.getString(it) }
                
                questions.add(
                    DraftQuestion(
                        text = obj.getString("question"),
                        options = options,
                        correctOptionIndex = obj.getInt("correctIndex"),
                        explanation = obj.optString("explanation", "")
                    )
                )
            }
            questions
        } catch (e: Exception) {
            throw Exception("AI generation failed: ${e.message}")
        }
    }
}
