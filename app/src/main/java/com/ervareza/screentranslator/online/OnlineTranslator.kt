package com.ervareza.screentranslator.online

import android.content.Context
import android.util.Log
import com.ervareza.screentranslator.ConfigManager
import org.json.JSONArray
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

    suspend fun translateBatch(texts: List<String>, targetLang: String): List<String>? {
        if (texts.isEmpty()) return emptyList()
        val sourceArray = JSONArray().apply { texts.forEach { put(it) } }
        val result = translate(
            "Translate each string independently into the target language. " +
                "You MUST return ONLY a valid JSON array of strings, in the same order and count as the input. " +
                "Do not use markdown, code fences, explanations, or any text outside the JSON array.\n\n" +
                "Input JSON array:\n$sourceArray",
            targetLang,
        ) ?: return null
        return try {
            val start = result.indexOf('[')
            val end = result.lastIndexOf(']')
            if (start < 0 || end <= start) return null
            val array = JSONArray(result.substring(start, end + 1))
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    add(if (array.isNull(index)) "" else array.optString(index))
                }
            }
        } catch (e: Exception) {
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
