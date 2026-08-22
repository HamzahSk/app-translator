package com.rocat.translator.online

// ---------- OpenAI Chat Completions (POST /v1/chat/completions) ----------
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.3,
)

data class OpenAiMessage(
    val role: String,
    val content: String,
)

data class OpenAiChatResponse(
    val choices: List<OpenAiChoice> = emptyList(),
)

data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null,
)

// ---------- Gemini generateContent (POST /v1beta/models/{model}:generateContent) ----------
data class GeminiRequest(
    val contents: List<GeminiContent>,
)

data class GeminiContent(
    val parts: List<GeminiPart>,
)

data class GeminiPart(
    val text: String,
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
)

data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
)
