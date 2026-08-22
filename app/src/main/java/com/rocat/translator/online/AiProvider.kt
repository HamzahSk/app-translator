package com.rocat.translator.online

/**
 * Supported AI translation providers.
 */
enum class AiProvider {
    OPENAI,
    GEMINI,
    DEFAULT,
    ;

    companion object {
        fun fromId(id: String): AiProvider = when (id) {
            "gemini" -> GEMINI
            "default" -> DEFAULT
            else -> OPENAI
        }
    }
}
