package com.ervareza.screentranslator.online

/**
 * Supported AI translation providers.
 */
enum class AiProvider {
    OPENAI,
    GEMINI,
    ;

    companion object {
        fun fromId(id: String): AiProvider = when (id) {
            "gemini" -> GEMINI
            else -> OPENAI
        }
    }
}
