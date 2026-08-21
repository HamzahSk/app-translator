package com.ervareza.screentranslator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationEngine(private val context: Context) {

    private val overlayManager = OverlayManager(context)
    private val config = ConfigManager(context)

    // ISSUE-011 FIX: Everything runs off the main thread. Heavy clients are
    // initialized lazily (only on first use) so the service starts instantly.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // PHASE 7 FIX: Independent background job (CPU-bound, Default) for the ML Kit
    // pre-load, fully decoupled from the capture pipeline so a warm-up never
    // blocks/stalls a real OCR/translate request.
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var activeJob: Job? = null

    private val statusBarHeight: Int by lazy {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    // Lazy: LanguageIdentification client is created on first use, inside an IO coroutine.
    private var languageIdentifier: LanguageIdentifier? = null

    // ISSUE-004 FIX: Cache recognizers instead of creating new ones per call
    private val recognizerCache = mutableMapOf<String, TextRecognizer>()

    // ISSUE-003 FIX: Cache translators per language pair to avoid per-block creation
    private val translatorCache = mutableMapOf<String, Translator>()

    private fun getLanguageIdentifier(): LanguageIdentifier = synchronized(this) {
        languageIdentifier ?: LanguageIdentification.getClient().also { languageIdentifier = it }
    }

    // Lazy: network client is only built when online mode is actually used.
    private val onlineTranslator by lazy { OnlineTranslator(context) }

    init {
        preloadScope.launch(Dispatchers.Default) { preloadOfflineModel() }
    }

    private val scanningIndicatorKey = "phase7_scanning"

    // PHASE 7 FIX: Show a visible loading bubble and force Android to render it
    // before the CPU-heavy OCR grabs all cores. withContext(Main) flushes the
    // queued overlay draw onto the UI thread, then delay(50) gives the system
    // a rendering window.
    private suspend fun showScanningIndicator() {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val width = (metrics.widthPixels * 0.42f).toInt().coerceAtLeast(1)
        val height = (48 * density).toInt()
        val left = (metrics.widthPixels - width) / 2
        val top = metrics.heightPixels / 3
        overlayManager.drawLoadingBubble(Rect(left, top, left + width, top + height), scanningIndicatorKey)
        withContext(Dispatchers.Main) { }
        delay(50)
    }

    // FASE 6 FIX: ML Kit can freeze internally and hang the coroutine forever.
    // Wrap every on-device .await() call with a bounded timeout so a stuck model
    // throws TimeoutCancellationException instead of blocking all future captures.
    private suspend fun <T> mlKitCall(tag: String, block: suspend () -> T): T? = try {
        withTimeout(ML_KIT_TIMEOUT_MS) { block() }
    } catch (e: TimeoutCancellationException) {
        Log.e("Translator", "$tag timed out after ${ML_KIT_TIMEOUT_MS}ms", e)
        null
    }

    private suspend fun preloadOfflineModel() {
        if (config.translationMode != "offline") return
        val source = config.sourceLanguage.takeUnless { it == "auto" } ?: "en"
        val target = config.targetLanguage
        runCatching { getTranslator(source, target).downloadModelIfNeeded().await() }
        // PHASE 7 FIX: Warm the OCR + translate models into RAM from inside the
        // independent preloadScope (Dispatchers.Default, CPU-bound). The clients
        // are created lock-free (see getRecognizer/getTranslator), so a real
        // capture can instantiate its own recognizer concurrently without stalls.
        runCatching {
            val dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            try {
                val dummyImage = InputImage.fromBitmap(dummy, 0)
                mlKitCall("PreloadOCR") { getRecognizer(source).process(dummyImage).await() }
                mlKitCall("PreloadTranslate") { getTranslator(source, target).translate("test").await() }
            } finally {
                dummy.recycle()
            }
        }
    }

    // PHASE 7 FIX: Client creation happens OUTSIDE the lock. TextRecognition.
    // getClient() can take time on first call; previously it ran inside a
    // synchronized block, so a pre-load warm-up could block a real capture's
    // getRecognizer() and stall the first screenshot.
    private fun getRecognizer(code: String): TextRecognizer {
        val cached = synchronized(recognizerCache) { recognizerCache[code] }
        if (cached != null) return cached
        val created = createRecognizer(code)
        return synchronized(recognizerCache) {
            recognizerCache[code] ?: created.also { recognizerCache[code] = it }
        }
    }

    private fun createRecognizer(code: String): TextRecognizer = when (code) {
        "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        "hi" -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private fun getTranslator(sourceLang: String, targetLang: String): Translator {
        val key = "${sourceLang}_$targetLang"
        val cached = synchronized(translatorCache) { translatorCache[key] }
        if (cached != null) return cached
        val created = Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang)
                .setTargetLanguage(targetLang)
                .build(),
        )
        return synchronized(translatorCache) {
            translatorCache[key] ?: created.also { translatorCache[key] = it }
        }
    }

    // ISSUE-011 FIX: Async pipeline. Bitmap conversion + OCR + translation all run
    // on Dispatchers.IO, so the main thread is never blocked.
    fun processImage(bitmap: Bitmap) {
        activeJob?.cancel()
        activeJob = scope.launch(Dispatchers.IO) {
            try {
                processImageInternal(bitmap)
            } catch (ce: CancellationException) {
                overlayManager.clearOverlays()
                throw ce
            } catch (e: Exception) {
                Log.e("Translator", "Processing failed", e)
            }
        }
    }

    private suspend fun processImageInternal(bitmap: Bitmap) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
        showScanningIndicator()
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
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
                    val text = mlKitCall("OCR") { recognizer.process(image).await() }
                    overlayManager.removeLoading(scanningIndicatorKey)
                    if (text != null && text.text.isNotBlank()) identifyAndTranslate(text)
                } catch (e: Exception) {
                    Log.e("Translator", "OCR Failed", e)
                }
            }
        } finally {
            overlayManager.removeLoading(scanningIndicatorKey)
        }
    }

    private suspend fun runFallbackChain(image: InputImage, codes: List<String>, index: Int) {
        if (index >= codes.size) return
        try {
            val recognizer = getRecognizer(codes[index])
            val text = mlKitCall("OCR-${codes[index]}") { recognizer.process(image).await() }
            if (text != null && text.text.isNotBlank()) {
                overlayManager.removeLoading(scanningIndicatorKey)
                identifyAndTranslate(text)
            } else {
                runFallbackChain(image, codes, index + 1)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runFallbackChain(image, codes, index + 1)
        }
    }

    // PHASE 8 FIX: ML Kit frequently splits a single conversation bubble into
    // many small TextBlocks; translating them individually makes the rendered
    // bubbles overlap. We first run an offline spatial merge so the pipeline
    // (loading bubble -> translation -> final draw) operates on whole bubbles.
    private suspend fun identifyAndTranslate(visionText: Text) {
        val fullText = visionText.text
        try {
            val languageCode = mlKitCall("LanguageIdentify") { getLanguageIdentifier().identifyLanguage(fullText).await() }
            if (languageCode != null && languageCode != "und") {
                Log.d("Translator", "Detected language: $languageCode")
                val merged = mergeBlocks(visionText.textBlocks)
                if (merged.isEmpty()) return
                if (config.translationMode != "offline") {
                    onlineTranslate(merged)
                } else {
                    translateBlocks(merged, languageCode)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Translator", "Language identification failed", e)
        }
    }

    // ISSUE-012 FIX: Online translation mode (OpenAI / Gemini compatible APIs).
    private suspend fun onlineTranslate(blocks: List<MergedBlock>) {
        val visible = blocks.filter { it.text.isNotBlank() && adjustedBoundingBox(it.boundingBox) != null }
        if (visible.isEmpty()) return
        val delimiter = "\n<<<SCREEN_TRANSLATOR_SEGMENT>>>\n"
        visible.forEachIndexed { index, merged ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            overlayManager.drawLoadingBubble(adjustedBoundingBox(merged.boundingBox)!!, index.toString())
        }
        try {
            val translated = onlineTranslator.translateBatch(visible.map { it.text }, config.targetLanguage, delimiter)
            if (translated == null) {
                visible.indices.forEach { overlayManager.removeLoading(it.toString()) }
                return
            }
            visible.forEachIndexed { index, merged ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val text = translated.getOrNull(index) ?: return@forEachIndexed
                adjustedBoundingBox(merged.boundingBox)?.let { box -> overlayManager.replaceLoading(index.toString(), text, box) }
            }
        } catch (e: CancellationException) {
            overlayManager.clearOverlays()
            throw e
        } catch (e: NullPointerException) {
            Log.e("Translator", "Online batch contained null data", e)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Translator", "Online batch segment count mismatch", e)
        } catch (e: Exception) {
            Log.e("Translator", "Online batch translation failed", e)
        } finally {
            visible.indices.forEach { overlayManager.removeLoading(it.toString()) }
        }
    }

    // PHASE 8 FIX: Iterate on merged bubbles instead of raw ML Kit TextBlocks
    // so the loading bubble, translation, and final overlay cover a single
    // conversation bubble rather than overlapping sub-segments.
    private suspend fun translateBlocks(blocks: List<MergedBlock>, sourceLangCode: String) {
        val targetLangCode = config.targetLanguage

        // ISSUE-003 FIX: Reuse a single translator for the entire batch
        val translator = getTranslator(sourceLangCode, targetLangCode)
        blocks.forEachIndexed { index, merged ->
            adjustedBoundingBox(merged.boundingBox)?.let { overlayManager.drawLoadingBubble(it, "offline_$index") }
        }

        try {
            translator.downloadModelIfNeeded().await()
            val translatedBubbles = mutableListOf<OverlayManager.Bubble>()
            for ((index, merged) in blocks.withIndex()) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val rect = adjustedBoundingBox(merged.boundingBox) ?: continue
                try {
                    val translatedText = mlKitCall("Translate") { translator.translate(merged.text).await() }
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (translatedText != null) {
                        translatedBubbles += OverlayManager.Bubble(translatedText, rect)
                    }
                    overlayManager.removeLoading("offline_$index")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Translator", "Block translation failed", e)
                }
            }
            if (translatedBubbles.isNotEmpty()) overlayManager.drawTranslationBatch(translatedBubbles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Translator", "Model download failed", e)
        } finally {
            blocks.indices.forEach { overlayManager.removeLoading("offline_$it") }
        }
    }

    // PHASE 8 FIX: A spatially grouped TextBlock (one logical bubble).
    // ML Kit frequently shards a single conversation bubble into several
    // tiny TextBlocks that, if translated separately, would render as
    // overlapping overlays. The merge pipeline collects them into a single
    // MergedBlock so the loading bubble, translation, and overlay all cover
    // the full extent of the original bubble.
    private data class MergedBlock(val text: String, val boundingBox: Rect)

    // PHASE 8 FIX: Spatial grouping of ML Kit TextBlocks.
    //
    // Strategy:
    //  1. Discard TextBlocks with no usable rect/text and compute the
    //     average text-block height (a proxy for line height).
    //  2. Sort the survivors top-to-bottom, left-to-right so we walk the
    //     screen in reading order.
    //  3. Greedily merge a candidate block into the previous one when:
    //       - the candidate sits "very close" vertically: gap <
    //         MERGE_GAP_FACTOR * averageHeight (this fuses the multiple
    //         sub-lines ML Kit produces from a single speech bubble), OR
    //       - horizontally overlapping / nearly touching: rects overlap
    //         or are within MERGE_GAP_FACTOR * averageHeight of each other
    //         on the x-axis and roughly share the same vertical band.
    //     The merged bounding box is the union of the two rects; the text
    //     is joined with a space (or newline if the previous line ended
    //     without a sentence terminator and the gap is wide enough to be a
    //     soft break).
    //  4. Result is a smaller, bubble-shaped list of MergedBlocks that the
    //     translate/online pipelines can iterate without overlapping.
    private fun mergeBlocks(blocks: List<Text.TextBlock>): List<MergedBlock> {
        val candidates = blocks.mapNotNull { block ->
            val rect = adjustedBoundingBox(block.boundingBox) ?: return@mapNotNull null
            val text = block.text.trim()
            if (text.isEmpty()) return@mapNotNull null
            text to rect
        }
        if (candidates.isEmpty()) return emptyList()

        val avgHeight = candidates.sumOf { (_, r) -> r.height() }
            .toDouble() / candidates.size
        val gapThreshold = (avgHeight * MERGE_GAP_FACTOR).toInt().coerceAtLeast(1)

        // Reading order: top-to-bottom, left-to-right within a row.
        val sorted = candidates.sortedWith(
            compareBy({ (it.second.top) / gapThreshold }, { it.second.left }),
        )

        val result = mutableListOf<MergedBlock>()
        for ((text, rect) in sorted) {
            val current = result.lastOrNull()
            if (current != null && shouldMerge(current.boundingBox, rect, gapThreshold)) {
                val joinedText = joinText(current.text, text, current.boundingBox, rect)
                result[result.size - 1] = MergedBlock(
                    text = joinedText,
                    boundingBox = Rect(current.boundingBox).apply { union(rect) },
                )
            } else {
                result += MergedBlock(text = text, boundingBox = Rect(rect))
            }
        }
        return result
    }

    // Decide whether `next` belongs to the same bubble as `prev`. We treat
    // two rects as "the same bubble" when:
    //  * `next.top` is just below `prev.bottom` (within the gap threshold)
    //    OR the rects vertically overlap and horizontally overlap/touch
    //    within the gap threshold. The second clause catches the case
    //    where ML Kit splits a single line into a left chunk + a right
    //    chunk with no vertical gap.
    private fun shouldMerge(prev: Rect, next: Rect, gapThreshold: Int): Boolean {
        val verticalGap = next.top - prev.bottom
        if (verticalGap in 0..gapThreshold) return true
        val verticalOverlap = next.top < prev.bottom && next.bottom > prev.top
        if (!verticalOverlap) return false
        val horizontalGap = maxOf(prev.left - next.right, next.left - prev.right, 0)
        return horizontalGap <= gapThreshold
    }

    private fun joinText(prev: String, next: String, prevRect: Rect, nextRect: Rect): String {
        // If the candidate starts on a new visual line (clearly below the
        // previous block), insert a newline so the translation preserves
        // the original line breaks. Otherwise glue with a single space.
        val isNewLine = nextRect.top > prevRect.centerY()
        val separator = if (isNewLine) "\n" else " "
        val needsSpace = !prev.endsWith(' ') && !prev.endsWith('\n') &&
            !next.startsWith(' ') && !next.startsWith('\n')
        return if (needsSpace) prev + separator + next else prev + separator.drop(1) + next
    }


    // ISSUE-010 FIX: Clean up all resources
    fun close() {
        scope.cancel()
        preloadScope.cancel()
        runCatching { languageIdentifier?.close() }
        languageIdentifier = null
        recognizerCache.values.forEach { runCatching { it.close() } }
        recognizerCache.clear()
        translatorCache.values.forEach { runCatching { it.close() } }
        translatorCache.clear()
        overlayManager.clearOverlays()
    }

    fun clearOverlays() {
        activeJob?.cancel()
        activeJob = null
        overlayManager.clearOverlays()
    }

    private fun adjustedBoundingBox(original: Rect?): Rect? {
        if (original == null || original.bottom <= statusBarHeight) return null
        return Rect(original).apply {
            top = (top - statusBarHeight).coerceAtLeast(0)
            bottom = (bottom - statusBarHeight).coerceAtLeast(top)
        }.takeUnless { it.isEmpty }
    }

    private companion object {
        const val ML_KIT_TIMEOUT_MS = 7_000L
        // PHASE 8 FIX: 1.5x the average text-block height is the cut-off for
        // considering two ML Kit blocks "part of the same bubble". Anything
        // further apart is treated as the next line/bubble. Tunable later.
        const val MERGE_GAP_FACTOR = 1.5
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
