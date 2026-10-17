package com.studyspark.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.studyspark.app.BuildConfig
import com.studyspark.app.domain.model.DraftQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiRepository {

    // Initialize the model with our secure API key
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 32
            topP = 1f
            maxOutputTokens = 4096
            responseMimeType = "application/json" // Force JSON output
        }
    )

    suspend fun generateQuestionsFromText(
        extractedText: String,
        numberOfQuestions: Int = 5
    ): List<DraftQuestion> = withContext(Dispatchers.IO) {
        
        // We must be very strict with the prompt to ensure valid JSON
        val prompt = """
            You are an expert educator. Based on the following text, generate exactly $numberOfQuestions multiple-choice questions.
            
            Return the response ONLY as a JSON array of objects. Do not include markdown formatting like ```json or any other text.
            Each object must have these exact keys:
            - "question": The question text (String)
            - "options": An array of exactly 4 strings
            - "correctIndex": An integer from 0 to 3 representing the correct option
            - "explanation": A brief explanation of why the answer is correct (String)
            
            Text to analyze:
            $extractedText
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text ?: throw Exception("Empty response from AI")

            // Clean up the response just in case the AI adds markdown code blocks
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
