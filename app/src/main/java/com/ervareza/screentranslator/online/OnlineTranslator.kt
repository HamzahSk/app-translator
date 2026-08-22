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
    private var serviceFingerprint: String? = null
    private val defaultScraper = DefaultScraperTranslator()

    private fun getService(): AiApiService {
        val provider = AiProvider.fromId(config.apiProvider)
        val baseUrl = config.apiBaseUrl.trim()
        val fingerprint = "${provider.name}|${config.apiKey}|$baseUrl"
        val cached = service
        if (cached != null && serviceFingerprint == fingerprint) return cached
        val created = AiApiClient.create(
            provider = provider,
            apiKey = config.apiKey,
            customBaseUrl = baseUrl.ifBlank { null },
        )
        service = created
        serviceFingerprint = fingerprint
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
                AiProvider.DEFAULT -> defaultScraper.translate(text)
            }
        } catch (e: Exception) {
            Log.e("OnlineTranslator", "Translation request failed", e)
            null
        }
    }

    suspend fun translateBatch(texts: List<String>, targetLang: String, delimiter: String): List<String>? {
        if (texts.isEmpty()) return emptyList()
        val joined = texts.joinToString(delimiter)
        val result = translate(
            "Translate each segment independently. Preserve the delimiter exactly and return the same number of segments.\n" +
                "Delimiter: $delimiter\n\n$joined",
            targetLang,
        ) ?: return null
        return try {
            result.split(delimiter).map { it.trim() }.takeIf { it.size == texts.size }
        } catch (e: RuntimeException) {
            Log.e("OnlineTranslator", "Failed to parse batch response", e)
            null
        }
    }

    private suspend fun translateWithOpenAi(model: String, prompt: String): String? {
        val request = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = "You are a precise manga translation engine."),
                OpenAiMessage(role = "user", content = prompt),
            ),
        )
        val response = getService().chatCompletion(request)
        return response.choices.firstOrNull()?.message?.content?.trim()?.takeIf { it.isNotBlank() }
    }

    private suspend fun translateWithGemini(model: String, prompt: String): String? {
        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
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
        AiProvider.DEFAULT -> "local-script"
    }

    private fun langName(code: String): String {
        return runCatching { Locale(code).displayLanguage }.getOrElse { code }
    }
}
