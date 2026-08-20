package com.ervareza.screentranslator

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ervareza.screentranslator.online.OnlineTranslator
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationEngine(private val context: Context) {

    private val overlayManager = OverlayManager(context)
    private val config = ConfigManager(context)

    // ISSUE-011 FIX: Everything runs off the main thread. Heavy clients are
    // initialized lazily (only on first use) so the service starts instantly.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Lazy: LanguageIdentification client is created on first use, inside an IO coroutine.
    private val languageIdentifier: LanguageIdentifier by lazy { LanguageIdentification.getClient() }

    // ISSUE-004 FIX: Cache recognizers instead of creating new ones per call
    private val recognizerCache = mutableMapOf<String, TextRecognizer>()

    // ISSUE-003 FIX: Cache translators per language pair to avoid per-block creation
    private val translatorCache = mutableMapOf<String, Translator>()

    // Lazy: network client is only built when online mode is actually used.
    private val onlineTranslator by lazy { OnlineTranslator(context) }

    private fun getRecognizer(code: String): TextRecognizer {
        return recognizerCache.getOrPut(code) {
            when (code) {
                "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                "hi" -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }
        }
    }

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "${sourceLang}_${targetLang}"
        return translatorCache.getOrPut(key) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build()
            Translation.getClient(options)
        }
    }

    // ISSUE-011 FIX: Async pipeline. Bitmap conversion + OCR + translation all run
    // on Dispatchers.IO, so the main thread is never blocked.
    fun processImage(bitmap: Bitmap) {
        scope.launch(Dispatchers.IO) {
            try {
                processImageInternal(bitmap)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Log.e("Translator", "Processing failed", e)
            }
        }
    }

    private suspend fun processImageInternal(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)

        if (config.sourceLanguage == "auto") {
            val supportedCodes = listOf("ja", "ko", "zh", "hi", "en")
            val installedCodes = supportedCodes.filter { config.isModelInstalled(it) }

            if (installedCodes.isEmpty()) {
                Log.e("Translator", "Auto-detect failed: No OCR models are installed!")
                return
            }
            runFallbackChain(image, installedCodes, 0)
        } else {
            val recognizer = getRecognizer(config.sourceLanguage)
            try {
                val text = recognizer.process(image).await()
                if (text.text.isNotBlank()) identifyAndTranslate(text)
            } catch (e: Exception) {
                Log.e("Translator", "OCR Failed", e)
            }
        }
    }

    private suspend fun runFallbackChain(image: InputImage, codes: List<String>, index: Int) {
        if (index >= codes.size) return
        try {
            val recognizer = getRecognizer(codes[index])
            val text = recognizer.process(image).await()
            if (text.text.isNotBlank()) {
                identifyAndTranslate(text)
            } else {
                runFallbackChain(image, codes, index + 1)
            }
        } catch (e: Exception) {
            runFallbackChain(image, codes, index + 1)
        }
    }

    private suspend fun identifyAndTranslate(visionText: Text) {
        val fullText = visionText.text
        try {
            val languageCode = languageIdentifier.identifyLanguage(fullText).await()
            if (languageCode != "und") {
                Log.d("Translator", "Detected language: $languageCode")
                if (config.translationMode != "offline") {
                    onlineTranslate(visionText)
                } else {
                    translateBlocks(visionText, languageCode)
                }
            }
        } catch (e: Exception) {
            Log.e("Translator", "Language identification failed", e)
        }
    }

    private suspend fun translateBlocks(visionText: Text, sourceLangCode: String) {
        val targetLangCode = config.targetLanguage

        // ISSUE-003 FIX: Reuse a single translator for the entire batch
        val translator = getTranslator(sourceLangCode, targetLangCode)

        try {
            translator.downloadModelIfNeeded().await()
            for (block in visionText.textBlocks) {
                try {
                    val translatedText = translator.translate(block.text).await()
                    block.boundingBox?.let { rect ->
                        overlayManager.drawTranslationBubble(translatedText, rect)
                    }
                } catch (e: Exception) {
                    Log.e("Translator", "Block translation failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e("Translator", "Model download failed", e)
        }
    }

    // ISSUE-012 FIX: Online translation mode (OpenAI / Gemini compatible APIs).
    private suspend fun onlineTranslate(visionText: Text) {
        for (block in visionText.textBlocks) {
            try {
                val translatedText = onlineTranslator.translate(block.text, config.targetLanguage)
                if (!translatedText.isNullOrBlank()) {
                    block.boundingBox?.let { rect ->
                        overlayManager.drawTranslationBubble(translatedText, rect)
                    }
                }
            } catch (e: Exception) {
                Log.e("Translator", "Online translation failed", e)
            }
        }
    }

    // ISSUE-010 FIX: Clean up all resources
    fun close() {
        scope.cancel()
        runCatching { languageIdentifier.close() }
        recognizerCache.values.forEach { runCatching { it.close() } }
        recognizerCache.clear()
        translatorCache.values.forEach { runCatching { it.close() } }
        translatorCache.clear()
        overlayManager.clearOverlays()
    }

    fun clearOverlays() {
        overlayManager.clearOverlays()
    }
}

// ISSUE-011 FIX: Bridge ML Kit's callback-based APIs into suspend functions.
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { e ->
        if (cont.isActive) cont.resumeWithException(e)
    }
    addOnCanceledListener {
        if (cont.isActive) cont.cancel()
    }
}