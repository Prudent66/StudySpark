package com.studyspark.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.studyspark.app.BuildConfig
import com.studyspark.app.domain.model.DraftQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiRepository {

    // Split keys, trim spaces, and filter out empty ones
    private val apiKeys = BuildConfig.GEMINI_API_KEY
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    init {
        if (apiKeys.isEmpty()) throw IllegalStateException("No Gemini API keys found. Check GitHub Secrets.")
    }

    private fun getModel(key: String) = GenerativeModel(
        modelName = "gemini-3.8-flash",
        apiKey = key,
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
        numberOfQuestions: Int, // a, b, c
        numberOfParts: Int,     // i, ii, iii
        questionType: String
    ): List<DraftQuestion> = withContext(Dispatchers.IO) {
        
        val prompt = if (questionType == "Essay") {
            val partsInstruction = if (numberOfParts > 0) {
                "Each main question MUST have exactly $numberOfParts parts labeled with Roman numerals: (i), (ii), (iii), etc."
            } else {
                "Do NOT include any Roman numeral parts. Just provide the main question text."
            }

            """
            You are an expert university lecturer. Based on the following text, generate exactly $numberOfQuestions main questions.
            
            VERY IMPORTANT FORMATTING RULES:
            1. Label main questions with sequential letters: (a), (b), (c), etc.
            2. $partsInstruction
            3. Use newline characters (\n) to separate all parts.
            4. Do NOT include numbers like "1." or "2." anywhere in the text.
            
            Example format (if parts > 0):
            "(a) Main question text...\n(i) First part...\n(ii) Second part...\n(b) Next main question...\n(i) First part..."
            Example format (if parts = 0):
            "(a) Main question text...\n(b) Next main question..."
            
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

        var lastException: Exception? = null

        // Try each API key in order
        for ((index, key) in apiKeys.withIndex()) {
            try {
                val model = getModel(key)
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
                return@withContext questions // Success!

            } catch (e: Exception) {
                val msg = e.message ?: ""
                // If it's a rate limit or invalid key, try the next one
                if (msg.contains("quota", ignoreCase = true) || 
                    msg.contains("429", ignoreCase = true) || 
                    msg.contains("API key not valid", ignoreCase = true)) {
                    lastException = Exception("Key ${index + 1} failed: $msg")
                } else {
                    throw Exception("AI generation failed: $msg") // Fatal error, stop
                }
            }
        }
        
        throw Exception("All API keys failed. Last error: ${lastException?.message}")
    }
}
