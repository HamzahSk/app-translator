package com.rocat.translator.online

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a lazily-created Retrofit client. The client is created only when
 * online mode is actually used, so offline users pay no initialization cost.
 */
object AiApiClient {

    private const val DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1/"
    private const val DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    fun create(provider: AiProvider, apiKey: String, customBaseUrl: String? = null): AiApiService {
        val baseUrl = when {
            !customBaseUrl.isNullOrBlank() -> ensureTrailingSlash(customBaseUrl.trim())
            provider == AiProvider.GEMINI -> DEFAULT_GEMINI_BASE_URL
            else -> DEFAULT_OPENAI_BASE_URL
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                when (provider) {
                    AiProvider.OPENAI -> {
                        // Bearer token header (OpenAI & any OpenAI-compatible endpoint)
                        if (apiKey.isNotBlank()) {
                            builder.header("Authorization", "Bearer $apiKey")
                        }
                    }
                    AiProvider.GEMINI -> {
                        // Gemini expects the key as a query parameter
                        if (apiKey.isNotBlank()) {
                            val url = original.url.newBuilder()
                                .addQueryParameter("key", apiKey)
                                .build()
                            builder.url(url)
                        }
                    }
                    AiProvider.DEFAULT -> Unit
                }
                chain.proceed(builder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AiApiService::class.java)
    }

    private fun ensureTrailingSlash(url: String): String = if (url.endsWith("/")) url else "$url/"
}
