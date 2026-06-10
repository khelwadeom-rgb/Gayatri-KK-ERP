package com.example.data.api

import com.example.gayatrikrushikendra.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inline_data: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mime_type: String,
    val data: String // Base64
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    suspend fun getAgricultureAdvice(
        cropType: String,
        soilType: String,
        irrigationType: String,
        queryPrompt: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing."
        }

        val formattedPrompt = """
            Crop: $cropType
            Soil Type: $soilType
            Irrigation: $irrigationType
            
            Action Request: $queryPrompt
        """.trimIndent()

        val systemInstructionText = """
            You are the "Gayatri Krushi Kendra Smart AI Advisor." 
            Provide expert agricultural advice for Indian farmers.
            Mention specific products like Urea, DAP, NPK, Syngenta Amistar, Bayer Regent, etc.
            Respond in a helpful, scientific tone.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = formattedPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response generated."
        } catch (e: Exception) {
            "API Failure: ${e.localizedMessage}"
        }
    }

    suspend fun analyzeCropImage(
        base64Image: String,
        mimeType: String,
        language: String = "English"
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing."
        }

        val systemInstructionText = """
            You are the "GKK AI Crop Doctor." 
            Analyze the image of a crop/leaf.
            1. Identify crop.
            2. Detect diseases/pests.
            3. Suggest treatment & products (Indian brands).
            
            Respond in $language.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(
                    Part(text = "Please analyze this crop image for diseases and suggest treatments."),
                    Part(inline_data = InlineData(mimeType, base64Image))
                ))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemInstructionText)))
        )

        try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No analysis possible."
        } catch (e: Exception) {
            "Analysis Failure: ${e.localizedMessage}"
        }
    }
}
