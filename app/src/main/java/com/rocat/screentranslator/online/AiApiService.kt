package com.ervareza.screentranslator.online

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit service describing the two supported API formats.
 */
interface AiApiService {

    /**
     * OpenAI Chat Completions.
     * Base URL: https://api.openai.com/v1/
     */
    @POST("chat/completions")
    suspend fun chatCompletion(@Body request: OpenAiChatRequest): OpenAiChatResponse

    /**
     * Google Gemini generateContent.
     * Base URL: https://generativelanguage.googleapis.com/
     * The API key is appended as a query parameter by [AiApiClient].
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(@Path("model") model: String, @Body request: GeminiRequest): GeminiResponse
}
