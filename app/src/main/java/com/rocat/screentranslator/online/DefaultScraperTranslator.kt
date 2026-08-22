package com.ervareza.screentranslator.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

class DefaultScraperTranslator(private val client: OkHttpClient = OkHttpClient()) {
    private val host = "https://android.chat.openai.com"
    private val jsonType = "application/json".toMediaType()
    private var cookie = ""
    private val deviceId = UUID.randomUUID().toString()
    private var parentMessageId = UUID.randomUUID().toString()

    suspend fun translate(prompt: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            ensureSession()
            val messageId = UUID.randomUUID().toString()
            val body = JSONObject().apply {
                put("action", "next")
                put(
                    "messages",
                    org.json.JSONArray().put(
                        JSONObject().apply {
                            put("id", messageId)
                            put("author", JSONObject().put("role", "user"))
                            put("content", JSONObject().put("content_type", "text").put("parts", org.json.JSONArray().put(prompt)))
                            put("status", "finished_successfully").put("recipient", "all")
                        },
                    ),
                )
                put("model", "auto").put("history_and_training_disabled", false).put("force_use_sse", true)
                put("stream", true).put("timezone", "Asia/Makassar").put("timezone_offset_min", -480)
            }
            val request = Request.Builder().url("$host/backend-anon/f/conversation").post(body.toString().toRequestBody(jsonType))
                .headers(commonHeaders("/backend-anon/f/conversation")).addHeader("Accept", "text/event-stream")
                .addHeader("Cookie", cookie).build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Conversation failed: ${response.code}" }
                var text = ""
                response.body?.charStream()?.forEachLine { line ->
                    if (!line.startsWith("data: ") || line == "data: [DONE]") return@forEachLine
                    runCatching {
                        val data = JSONObject(line.substring(6))

                        // Cek apakah ada object "message" dan role-nya "assistant"
                        val msg = data.optJSONObject("message")
                        if (msg != null && msg.optJSONObject("author")?.optString("role") == "assistant") {
                            val extractedText = msg.optJSONObject("content")?.optJSONArray("parts")?.optString(0)

                            if (!extractedText.isNullOrEmpty()) {
                                // Timpa teks lama karena stream dari web sudah berupa teks akumulatif
                                text = extractedText
                                parentMessageId = msg.optString("id", parentMessageId)
                            }
                        }
                    }
                }
                cleanSpecialTags(text).takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun ensureSession() {
        if (cookie.isNotBlank()) return
        val request = Request.Builder().url("$host/backend-anon/sentinel/chat-requirements").post("{}".toRequestBody(jsonType))
            .headers(commonHeaders("/backend-anon/sentinel/chat-requirements")).build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Session failed: ${response.code}" }
            cookie = response.headers.values("Set-Cookie").map { it.substringBefore(';') }.joinToString("; ")
            val token = response.body?.string()?.let { runCatching { JSONObject(it).optString("token") }.getOrNull() }.orEmpty()
            if (cookie.none { false } && token.isNotBlank()) cookie = "oai-sc=0$token"
        }
    }

    private fun commonHeaders(path: String) = okhttp3.Headers.Builder().add("User-Agent", "ChatGPT/1.2026.181 (Android 16; Neo/1.0; build 2222222)")
        .add("OAI-Package-Name", "com.openai.chatgpt").add("OAI-Client-Type", "android").add("OAI-Device-Id", deviceId)
        .add("X-OpenAI-Target-Path", path).add("ChatGPT-Account-Id", "default").add("Content-Type", "application/json").build()

    private fun cleanSpecialTags(value: String): String = value.replace(Regex("\\uE200entity\\uE202([^\\uE201]+)\\uE201")) { match ->
        runCatching { JSONObject("{\"v\":${match.groupValues[1]}}") }.getOrNull()?.optJSONArray("v")?.optString(1).orEmpty()
    }.replace(Regex("\\uE200[^\\uE201]*\\uE201"), "").trim()
}
