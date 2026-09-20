package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class VisionResult(
    val title: String,
    val isStudy: Boolean,
    val explanation: String,
    val keyConcept: String,
    val finalAnswer: String,
    val spokenSummary: String,
    val rawResponse: String
)

object GeminiVisionAnalyzer {
    private const val TAG = "GeminiVision"
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeImage(
        context: Context,
        bitmap: Bitmap,
        mode: String = "study" // "study" or "general"
    ): Result<VisionResult> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("api_key", "")?.takeIf { it.isNotBlank() }
                ?: BuildConfig.BUILD_TYPE // fallback check

            val finalKey = if (apiKey.isNotBlank() && apiKey != "YOUR_API_KEY") apiKey else {
                prefs.getString("api_key", "") ?: ""
            }

            if (finalKey.isBlank()) {
                return@withContext Result.failure(Exception("Gemini API Key is missing. Please set your API key in Settings."))
            }

            // Compress bitmap to JPEG Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            // Scale bitmap if too large to ensure fast low-latency response
            val maxDim = 1280
            val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val bossName = prefs.getString("boss_name", "Boss") ?: "Boss"
            val assistantName = prefs.getString("assistant_name", "Maya") ?: "Maya"

            val systemPrompt = if (mode == "study") {
                """
                You are $assistantName, an elite AI tutor, academic mentor, and professor assisting $bossName.
                The image contains educational material (e.g. math problem, physics derivation, chemistry equation, biology diagram, coding algorithm, history/GK question, or study notes).

                Please provide a deep, clear, and encouraging study explanation in natural Hindi-English (Hinglish):
                1. 📚 TOPIC & SUBJECT: Identify the subject, chapter, and core scientific/mathematical concept.
                2. ❓ QUESTION DETECTED: Transcribe the question clearly.
                3. 💡 STEP-BY-STEP SOLUTION & INTUITION: Break down each step logically. Explain 'Why' we use this formula or method so $bossName understands the concept from first principles.
                4. 🔑 KEY FORMULAS / RULES: Highlight the fundamental formulas, laws, or theorems.
                5. 🎯 FINAL ANSWER: Highlight the exact final answer clearly.
                6. 🚀 PRO-TIP / TRICK: Provide a memory trick, exam tip, or shortcut to solve similar questions quickly.
                """.trimIndent()
            } else {
                """
                You are $assistantName, an intelligent visual assistant helping $bossName.
                Carefully analyze the image:
                1. 🔍 WHAT IS THIS: Identify objects, scene, text, products, or environment clearly.
                2. 📝 KEY DETAILS: Transcribe any visible text, dates, numbers, or unique attributes.
                3. 💡 INSIGHTS & ANSWER: Answer any visible question or provide helpful insights, translation, or suggestions in natural Hindi-English (Hinglish).
                """.trimIndent()
            }

            val requestBodyJson = buildJsonObject {
                putJsonArray("contents") {
                    add(buildJsonObject {
                        putJsonArray("parts") {
                            add(buildJsonObject {
                                put("text", systemPrompt)
                            })
                            add(buildJsonObject {
                                putJsonObject("inline_data") {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                }
                            })
                        }
                    })
                }
                putJsonObject("generationConfig") {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 2048)
                }
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$finalKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Gemini Vision error: $errBody")
                return@withContext Result.failure(Exception("Gemini Vision request failed (Code ${response.code}): $errBody"))
            }

            val respBody = response.body?.string() ?: ""
            val jsonElement = json.parseToJsonElement(respBody)

            val textCandidate = jsonElement.jsonObject["candidates"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content ?: "No response received."

            // Parse response into structured pieces
            val parsedResult = parseVisionText(textCandidate, mode)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in analyzeImage", e)
            Result.failure(e)
        }
    }

    private fun parseVisionText(text: String, mode: String): VisionResult {
        val lines = text.lines()
        val title = lines.firstOrNull { it.isNotBlank() }?.replace(Regex("[#*`_]"), "")?.trim() ?: "Scan Analysis"
        
        // Formulate a concise spoken audio summary for voice narration
        val cleanForSpeech = text.replace(Regex("[#*`_>-]"), "")
            .lines()
            .filter { it.isNotBlank() && !it.contains("http", ignoreCase = true) }
            .take(6)
            .joinToString(". ")

        return VisionResult(
            title = title,
            isStudy = mode == "study",
            explanation = text,
            keyConcept = "",
            finalAnswer = "",
            spokenSummary = cleanForSpeech,
            rawResponse = text
        )
    }
}
