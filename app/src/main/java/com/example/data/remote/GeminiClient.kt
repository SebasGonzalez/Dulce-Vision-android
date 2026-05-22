package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Client to connect directly with Gemini 3.5 Flash REST API to
 * generate custom movie, series or channel recommendations in real-time.
 */
object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun getSmartRecommendations(userPreferencePrompt: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing, using offline local AI recommendations...")
            return@withContext getOfflineMocks(userPreferencePrompt)
        }

        val url = "$BASE_URL$MODEL_NAME:generateContent?key=$apiKey"

        // Build the precise JSON request for Gemini API
        val prompt = """
            Eres el motor de Recomendación de IA de la plataforma de Streaming "DulceVision".
            El usuario te pide películas o canales con este criterio: "$userPreferencePrompt".
            Analiza su petición y devuelve una lista con los nombres de 3 películas, series o canales que encajen.
            IMPORTANTE: Devuelve ÚNICAMENTE un array JSON válido de strings con los títulos sugeridos.
            Ejemplo de salida:
            ["Sintel", "Tears of Steel", "Cosmos Laundromat"]
            No incluyas texto explicativo, solo el JSON raw.
        """.trimIndent()

        val jsonRequest = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()

        partObject.put("text", prompt)
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        jsonRequest.put("contents", contentsArray)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonRequest.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}")
                    return@withContext getOfflineMocks(userPreferencePrompt)
                }

                val bodyString = response.body?.string() ?: ""
                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val rawText = firstPart?.optString("text") ?: ""

                // Log response and extract JSON array
                Log.d(TAG, "Gemini Raw Output: $rawText")
                parseSuggestedTitles(rawText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or JSON error, falling back...", e)
            getOfflineMocks(userPreferencePrompt)
        }
    }

    private fun parseSuggestedTitles(rawText: String): List<String> {
        return try {
            // Clean text from markdown block wrapper ```json ... ```
            var cleanText = rawText.trim()
            if (cleanText.startsWith("```json")) {
                cleanText = cleanText.substringAfter("```json").substringBeforeLast("```")
            } else if (cleanText.startsWith("```")) {
                cleanText = cleanText.substringAfter("```").substringBeforeLast("```")
            }
            cleanText = cleanText.trim()

            val jsonArray = JSONArray(cleanText)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Parsing text as JSON failed. Extracting text manually...", e)
            // Manual fallback parsing in case model returns plain list
            val matches = Regex("\"([^\"]+)\"").findAll(rawText)
            val extracted = matches.map { it.groupValues[1] }.toList()
            extracted.ifEmpty { listOf("Sintel", "Elephant's Dream", "Tears of Steel") }
        }
    }

    private fun getOfflineMocks(prompt: String): List<String> {
        val lowerPrompt = prompt.lowercase()
        return when {
            lowerPrompt.contains("accion") || lowerPrompt.contains("action") || lowerPrompt.contains("pelea") -> {
                listOf("Tears of Steel", "The Charge of the Light Brigade", "Hero Quest Live")
            }
            lowerPrompt.contains("futuro") || lowerPrompt.contains("ciencia") || lowerPrompt.contains("scifi") || lowerPrompt.contains("robot") -> {
                listOf("Tears of Steel", "Sintel", "Futura TV SciFi")
            }
            lowerPrompt.contains("animacion") || lowerPrompt.contains("niño") || lowerPrompt.contains("dibujo") -> {
                listOf("Caminandes: Gran Dillama", "Big Buck Bunny", "Elephant's Dream")
            }
            lowerPrompt.contains("canales") || lowerPrompt.contains("iptv") || lowerPrompt.contains("en vivo") || lowerPrompt.contains("live") -> {
                listOf("Cinema Premium HD", "Sports Live Action", "Discovery Earth HD")
            }
            else -> {
                // Heuristic suggestions
                listOf("Sintel (BGE)", "Big Buck Bunny", "Caminandes 3")
            }
        }
    }
}
