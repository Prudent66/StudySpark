package com.studyspark.app.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.studyspark.app.BuildConfig
import com.studyspark.app.domain.model.DraftQuestion
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class GeminiRepository {

    private val apiKeys = BuildConfig.GEMINI_API_KEY
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    init {
        if (apiKeys.isEmpty()) throw IllegalStateException("No API keys found.")
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateQuestionsFromText(
        extractedText: String,
        numberOfQuestions: Int, 
        numberOfParts: Int,
        questionType: String
    ): List<DraftQuestion> = withContext(Dispatchers.IO) {
        
        val prompt = buildPrompt(extractedText, numberOfQuestions, numberOfParts, questionType)
        var lastException: Exception? = null

        for ((index, keyConfig) in apiKeys.withIndex()) {
            val parts = keyConfig.split(":", limit = 2)
            if (parts.size != 2) continue
            
            val provider = parts[0].lowercase()
            val apiKey = parts[1]

            try {
                val questions = withTimeoutOrNull(20_000L) {
                    when (provider) {
                        "gemini" -> callGemini(apiKey, prompt, questionType)
                        "groq" -> callOpenAiCompatible("https://api.groq.com/openai/v1", apiKey, "llama-3.3-70b-versatile", prompt, questionType)
                        "cerebras" -> callOpenAiCompatible("https://api.cerebras.ai/v1", apiKey, "llama3.3-70b", prompt, questionType)
                        "sambanova" -> callOpenAiCompatible("https://api.sambanova.ai/v1", apiKey, "Meta-Llama-3.3-70B-Instruct", prompt, questionType)
                        "openrouter" -> callOpenAiCompatible("https://openrouter.ai/api/v1", apiKey, "meta-llama/llama-3.3-70b-instruct:free", prompt, questionType)
                        "nvidia" -> callOpenAiCompatible("https://integrate.api.nvidia.com/v1", apiKey, "nvidia/nemotron-3.5-lightning-30b-a3b", prompt, questionType)
                        "together" -> callOpenAiCompatible("https://api.together.ai/v1", apiKey, "Meta-Llama-3.3-70B-Instruct-Turbo", prompt, questionType)
                        else -> null
                    }
                }

                if (questions != null) return@withContext questions
                else lastException = Exception("$provider key ${index + 1} timed out.")
            } catch (e: Exception) {
                lastException = Exception("$provider key ${index + 1} failed: ${e.message}")
            }
        }
        
        throw Exception("All ${apiKeys.size} API keys failed. Last error: ${lastException?.message}")
    }

    private suspend fun callGemini(apiKey: String, prompt: String, questionType: String): List<DraftQuestion> {
        val model = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.3f; maxOutputTokens = 8192; responseMimeType = "application/json"
            }
        )
        val response = model.generateContent(prompt)
        return parseJsonResponse(response.text ?: throw Exception("Empty response"), questionType)
    }

    private fun callOpenAiCompatible(baseUrl: String, apiKey: String, modelName: String, prompt: String, questionType: String): List<DraftQuestion> {
        val json = JsonObject().apply {
            addProperty("model", modelName)
            add("messages", Gson().toJsonTree(listOf(mapOf("role" to "user", "content" to prompt))))
            addProperty("temperature", 0.3)
        }

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

        val responseBody = response.body?.string() ?: throw Exception("Empty response")
        val jsonObject = Gson().fromJson(responseBody, JsonObject::class.java)
        val content = jsonObject.getAsJsonArray("choices").get(0).asJsonObject
            .getAsJsonObject("message").get("content").asString

        return parseJsonResponse(content, questionType)
    }

    private fun parseJsonResponse(rawResponse: String, questionType: String): List<DraftQuestion> {
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
        return questions
    }

    private fun buildPrompt(text: String, numQ: Int, numParts: Int, type: String): String {
        return if (type == "Essay") {
            """
            You are an expert university lecturer. Based on the text, generate exactly $numQ main questions.
            RULES:
            1. Label main questions as (a), (b), (c), etc.
            2. Vary parts naturally. Each main question has between 1 and $numParts parts.
            3. Label parts with Roman numerals: (i), (ii), etc.
            4. Use \n to separate parts. Do NOT include numbers like "1." anywhere.
            Return ONLY a JSON array. Keys: "question", "options" (empty array), "correctIndex" (0), "explanation".
            Text: $text
            """.trimIndent()
        } else {
            """
            You are an expert educator. Based on the text, generate exactly $numQ multiple-choice questions.
            Return ONLY a JSON array. Keys: "question", "options" (array of 4), "correctIndex" (0-3), "explanation".
            Text: $text
            """.trimIndent()
        }
    }
}
