package com.rocat.translator.online

import android.content.Context
import android.util.Log
import com.rocat.translator.ConfigManager
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

        val prompt = "You are a professional Manga and Manhwa translator. " +
            "Translate the following text into $targetLangName using natural, casual language that fits everyday comic dialogue. " +
            "Preserve the original meaning, emotion, and character voice. " +
            "Do not add explanations, notes, or quotation marks. Return only the translation.\n\n" +
            "Source text: $text"

        return try {
            when (provider) {
                AiProvider.OPENAI -> translateWithOpenAi(model, prompt)
                AiProvider.GEMINI -> translateWithGemini(model, prompt)
                AiProvider.DEFAULT -> defaultScraper.translate(prompt)
            }
        } catch (e: Exception) {
            Log.e("OnlineTranslator", "Translation request failed", e)
            null
        }
    }

    suspend fun translateBatch(texts: List<String>, targetLang: String): List<String>? {
        if (texts.isEmpty()) return emptyList()
        val sourceArray = JSONArray().apply { texts.forEach { put(it) } }
        val targetLangName = langName(targetLang)
        val provider = AiProvider.fromId(config.apiProvider)
        val model = config.apiModel.ifBlank { defaultModelFor(provider) }

        val batchPrompt = "You are a professional Manga and Manhwa translator. " +
            "Translate the following JSON array of texts into $targetLangName using natural, casual " +
            "language that fits everyday comic dialogue. " +
            "Preserve the original meaning, emotion, and character voice. " +
            "Do not add explanations, notes, or quotation marks. " +
            "You MUST return ONLY a valid JSON array of strings in the exact same order. Do not use markdown.\n\n" +
            "Source text: $sourceArray"

        val result = try {
            when (provider) {
                AiProvider.OPENAI -> translateWithOpenAi(model, batchPrompt)
                AiProvider.GEMINI -> translateWithGemini(model, batchPrompt)
                AiProvider.DEFAULT -> defaultScraper.translate(batchPrompt)
            }
        } catch (e: Exception) {
            Log.e("OnlineTranslator", "Batch translation request failed", e)
            null
        } ?: return null

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
