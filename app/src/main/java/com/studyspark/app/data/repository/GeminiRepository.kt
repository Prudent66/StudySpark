package com.studyspark.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.studyspark.app.BuildConfig
import com.studyspark.app.domain.model.DraftQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiRepository {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    init {
        if (apiKey.isBlank()) throw IllegalStateException("GEMINI_API_KEY is missing.")
    }

    private val model = GenerativeModel(
        modelName = "gemini-3.8-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.2f
            topK = 32
            topP = 1f
            maxOutputTokens = 8192 
            responseMimeType = "application/json"
        }
    )

    suspend fun generateQuestionsFromText(
        extractedText: String,
        numberOfQuestions: Int,
        numberOfParts: Int, // NEW
        numberOfSubParts: Int, // NEW
        questionType: String
    ): List<DraftQuestion> = withContext(Dispatchers.IO) {
        
        val prompt = if (questionType == "Essay") {
            val subQuestionInstruction = """
            Generate exactly $numberOfQuestions main questions.
            Each main question MUST have exactly $numberOfParts parts labeled as (a), (b), (c), etc.
            For EACH part, include exactly $numberOfSubParts sub-parts labeled as (i), (ii), (iii).
            
            VERY IMPORTANT: Do NOT include the main question number (e.g., "1.", "2.") in the text. Start directly with the question text.
            Use newline characters (\n) to format the text clearly. 
            Example format:
            "Main question text...\n(a) First part...\n(i) Sub-part...\n(ii) Sub-part...\n(b) Second part..."
            """.trimIndent()

            """
            You are an expert university lecturer. Based on the following text, generate the requested questions.
            $subQuestionInstruction
            Return the response ONLY as a JSON array of objects. Do not include markdown formatting.
            Each object must have these exact keys:
            - "question": The full essay question text formatted as requested (String)
            - "options": An empty array []
            - "correctIndex": Use 0
            - "explanation": The model answer or key points (String)
            
            Text:
            $extractedText
            """.trimIndent()
        } else {
            """
            You are an expert educator. Based on the following text, generate exactly $numberOfQuestions multiple-choice questions.
            Return the response ONLY as a JSON array of objects. Do not include markdown formatting.
            Each object must have these exact keys: "question", "options" (array of 4 strings), "correctIndex" (0-3), and "explanation".
            
            Text:
            $extractedText
            """.trimIndent()
        }

        try {
            val response = model.generateContent(prompt)
            val rawResponse = response.text ?: throw Exception("Empty response")
            val jsonString = rawResponse.replace("```json", "").replace("```", "").trim()

            val questions = mutableListOf<DraftQuestion>()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val optionsList = mutableListOf<String>()
                if (questionType != "Essay") {
                    val optionsArray = obj.getJSONArray("options")
                    for (j in 0 until 4) optionsList.add(optionsArray.getString(j))
                }
                questions.add(
                    DraftQuestion(
                        text = obj.getString("question"),
                        options = if (questionType == "Essay") listOf("", "", "", "") else optionsList,
                        correctOptionIndex = obj.optInt("correctIndex", 0),
                        explanation = obj.optString("explanation", ""),
                        questionType = questionType
                    )
                )
            }
            questions
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // NEW: Catch Rate Limit / Quota Error
            if (msg.contains("quota", ignoreCase = true) || msg.contains("429", ignoreCase = true)) {
                throw Exception("API Limit Reached. Please wait a minute before generating again, or upgrade your Google AI Studio billing plan.")
            }
            throw Exception("AI generation failed: $msg")
        }
    }
}
