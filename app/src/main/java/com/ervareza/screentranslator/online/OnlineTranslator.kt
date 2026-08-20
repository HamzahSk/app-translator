package com.ervareza.screentranslator.online

import android.content.Context
import android.util.Log
import com.ervareza.screentranslator.ConfigManager
import java.util.Locale

/**
 * Translates text blocks through a custom AI API (OpenAI or Gemini compatible).
 * All requests are suspend functions, safe to call from a CoroutineScope.
 */
class OnlineTranslator(context: Context) {

    private val config = ConfigManager(context)
    private var service: AiApiService? = null

    private fun getService(): AiApiService {
        val cached = service
        if (cached != null) return cached
        val created = AiApiClient.create(
            provider = AiProvider.fromId(config.apiProvider),
            apiKey = config.apiKey,
            customBaseUrl = config.apiBaseUrl.ifBlank { null }
        )
        service = created
        return created
    }

    suspend fun translate(text: String, targetLang: String): String? {
        val provider = AiProvider.fromId(config.apiProvider)
        val model = config.apiModel.ifBlank { defaultModelFor(provider) }
        val targetLangName = langName(targetLang)

        val prompt = "You are a manga/comic translation engine. " +
            "Translate the following text into $targetLangName. " +
            "Return ONLY the translation without any explanation, notes, or quotation marks.\n\n" +
            "Source text: $text"

        return try {
            when (provider) {
                AiProvider.OPENAI -> translateWithOpenAi(model, prompt)
                AiProvider.GEMINI -> translateWithGemini(model, prompt)
            }
        } catch (e: Exception) {
            Log.e("OnlineTranslator", "Translation request failed", e)
            null
        }
    }

    private suspend fun translateWithOpenAi(model: String, prompt: String): String? {
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = "You are a precise manga translation engine."),
                OpenAiMessage(role = "user", content = prompt)
            )
        )
        val response = getService().chatCompletion(request)
        return response.choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun translateWithGemini(model: String, prompt: String): String? {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )
        val response = getService().generateContent(model, request)
        return response.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.joinToString("") { it.text }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun defaultModelFor(provider: AiProvider): String = when (provider) {
        AiProvider.OPENAI -> "gpt-4o-mini"
        AiProvider.GEMINI -> "gemini-1.5-flash"
    }

    private fun langName(code: String): String {
        return runCatching { Locale(code).displayLanguage }.getOrElse { code }
    }
}