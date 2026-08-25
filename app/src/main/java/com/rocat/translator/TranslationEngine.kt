package com.rocat.translator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
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
import com.rocat.translator.online.OnlineTranslator
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

/**
 * PHASE 8 FIX: Smart merged block produced by [TranslationEngine.mergeBlocks].
 *
 * ML Kit tends to fragment a single speech bubble into multiple [Text.TextBlock]
 * entries. When drawn naively, each fragment shows its own overlay and they stack
 * on top of each other. To fix that we pre-merge neighbouring fragments into a
 * single bubble that:
 *  - has a combined bounding box ([rect])
 *  - holds the concatenated original text ([text], joined with `\n`)
 *  - remembers the average line height ([lineHeight]) of its source block(s), so
 *    downstream code (and the merge heuristic itself) can compare font sizes and
 *    refuse to fuse a tiny dialogue line with a giant SFX burst.
 */
data class MergedBlock(
    val text: String,
    val rect: Rect,
    val lineHeight: Float,
    val rotation: Float = 0f,
    val sampledColor: Int = android.graphics.Color.WHITE,
    val detectedTextColor: Int = android.graphics.Color.BLACK,
)

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

    @Volatile private var networkAvailable = true
    private val bitmapLock = Any()
    private var activeBitmap: Bitmap? = null

    private val statusBarHeight: Int by lazy {
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) context.resources.getDimensionPixelSize(resourceId) else 0
    }

    // Lazy: LanguageIdentification client is created on first use, inside an IO coroutine.
    private var languageIdentifier: LanguageIdentifier? = null

    @Volatile private var activeRecognizer: TextRecognizer? = null

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

    private suspend fun preloadOfflineModel(force: Boolean = false) {
        if (!force && config.translationMode != "offline") return
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
                recognize(source, dummyImage, "PreloadOCR", trackAsActive = false)
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
    private fun createRecognizer(code: String): TextRecognizer = when (code) {
        "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
        "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        "hi" -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
        else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private suspend fun recognize(code: String, image: InputImage, tag: String, trackAsActive: Boolean = true): Text? {
        val recognizer = createRecognizer(code)
        if (trackAsActive) activeRecognizer = recognizer
        return try {
            mlKitCall(tag) { recognizer.process(image).await() }
        } finally {
            if (trackAsActive && activeRecognizer === recognizer) activeRecognizer = null
            runCatching { recognizer.close() }
        }
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
        synchronized(bitmapLock) {
            activeJob?.cancel()
            activeBitmap?.takeUnless { it === bitmap }?.let { old -> runCatching { old.recycle() } }
            activeBitmap = bitmap
        }
        activeJob = scope.launch(Dispatchers.IO) {
            try {
                processImageInternal(bitmap)
            } catch (ce: CancellationException) {
                overlayManager.clearOverlays()
                throw ce
            } catch (e: Exception) {
                Log.e("Translator", "Processing failed", e)
            } finally {
                synchronized(bitmapLock) {
                    if (activeBitmap === bitmap) activeBitmap = null
                }
                runCatching { if (!bitmap.isRecycled) bitmap.recycle() }
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
                runFallbackChain(image, installedCodes, 0, bitmap)
            } else {
                try {
                    val text = recognize(config.sourceLanguage, image, "OCR")
                    overlayManager.removeLoading(scanningIndicatorKey)
                    if (text != null && text.text.isNotBlank()) identifyAndTranslate(text, bitmap)
                } catch (e: Exception) {
                    Log.e("Translator", "OCR Failed", e)
                }
            }
        } finally {
            overlayManager.removeLoading(scanningIndicatorKey)
        }
    }

    private suspend fun runFallbackChain(image: InputImage, codes: List<String>, index: Int, bitmap: Bitmap) {
        if (index >= codes.size) return
        try {
            val text = recognize(codes[index], image, "OCR-${codes[index]}")
            if (text != null && text.text.isNotBlank()) {
                overlayManager.removeLoading(scanningIndicatorKey)
                identifyAndTranslate(text, bitmap)
            } else {
                runFallbackChain(image, codes, index + 1, bitmap)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            runFallbackChain(image, codes, index + 1, bitmap)
        }
    }

    private suspend fun identifyAndTranslate(visionText: Text, bitmap: Bitmap) {
        val fullText = visionText.text
        try {
            var languageCode = mlKitCall("LanguageIdentify") { getLanguageIdentifier().identifyLanguage(fullText).await() }

            // FIX: Kalau ML Kit bingung ("und"), jangan dibatalkan!
            // Gunakan bahasa dari pengaturan sebagai cadangan.
            if (languageCode == null || languageCode == "und") {
                languageCode = if (config.sourceLanguage != "auto") config.sourceLanguage else "en"
                Log.d("Translator", "Deteksi bahasa gagal (und), menggunakan fallback: $languageCode")
            } else {
                Log.d("Translator", "Detected language: $languageCode")
            }

            // Lanjut gas translate
            if (config.translationMode != "offline" && networkAvailable) {
                onlineTranslate(visionText, languageCode, bitmap)
            } else {
                translateBlocks(visionText, languageCode, bitmap)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Translator", "Language identification failed", e)
        }
    }

    private suspend fun translateBlocks(visionText: Text, sourceLangCode: String, bitmap: Bitmap) {
        val targetLangCode = config.targetLanguage

        // ISSUE-003 FIX: Reuse a single translator for the entire batch
        val translator = getTranslator(sourceLangCode, targetLangCode)
        // PHASE 8 FIX: Use merged blocks so neighbouring fragments of the same
        // speech bubble share one translation request and one bubble overlay.
        val mergedBlocks = mergeBlocks(visionText.textBlocks, bitmap)
        mergedBlocks.forEachIndexed { index, block ->
            adjustedBoundingBox(block.rect)?.let { overlayManager.drawLoadingBubble(it, "offline_$index") }
        }

        try {
            translator.downloadModelIfNeeded().await()
            val translatedBubbles = mutableListOf<OverlayManager.Bubble>()
            for ((index, block) in mergedBlocks.withIndex()) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val rect = adjustedBoundingBox(block.rect) ?: continue
                try {
                    val translatedText = mlKitCall("Translate") { translator.translate(block.text).await() }
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    if (translatedText != null) {
                        translatedBubbles += OverlayManager.Bubble(translatedText, rect, block.rotation, block.sampledColor, block.detectedTextColor)
                    }
                    overlayManager.removeLoading("offline_$index")
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("Translator", "Block translation failed", e)
                }
            }
            if (activeJob == null || TranslationControlState.paused) {
                return
            }

            if (translatedBubbles.isNotEmpty()) overlayManager.drawTranslationBatch(translatedBubbles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Translator", "Model download failed", e)
        } finally {
            mergedBlocks.indices.forEach { overlayManager.removeLoading("offline_$it") }
        }
    }

    // ISSUE-012 FIX: Online translation mode (OpenAI / Gemini compatible APIs).
    private suspend fun onlineTranslate(visionText: Text, sourceLangCode: String, bitmap: Bitmap) {
        // PHASE 8 FIX: Merge neighbouring fragments into single bubbles before
        // sending them to the API so we get one translated result per bubble
        // and one overlay per bubble instead of overlapping duplicates.
        val blocks = mergeBlocks(visionText.textBlocks, bitmap)
            .filter { it.text.isNotBlank() && adjustedBoundingBox(it.rect) != null }
        if (blocks.isEmpty()) return
        blocks.forEachIndexed { index, block ->
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            overlayManager.drawLoadingBubble(adjustedBoundingBox(block.rect)!!, index.toString())
        }
        try {
            val translated = onlineTranslator.translateBatch(blocks.map { it.text }, config.targetLanguage)
            if (translated == null) {
                if (!networkAvailable) translateBlocks(visionText, sourceLangCode, bitmap)
                return
            }
            val translatedBubbles = mutableListOf<OverlayManager.Bubble>()
            blocks.forEachIndexed { index, block ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val text = translated.getOrNull(index) ?: return@forEachIndexed
                adjustedBoundingBox(block.rect)?.let { box ->
                    translatedBubbles += OverlayManager.Bubble(text, box, block.rotation, block.sampledColor, block.detectedTextColor)
                }
            }
            if (activeJob == null || TranslationControlState.paused) {
                return
            }

            if (translatedBubbles.isNotEmpty()) overlayManager.drawTranslationBatch(translatedBubbles)
        } catch (e: CancellationException) {
            overlayManager.clearOverlays()
            throw e
        } catch (e: NullPointerException) {
            Log.e("Translator", "Online batch contained null data", e)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("Translator", "Online batch segment count mismatch", e)
        } catch (e: Exception) {
            Log.e("Translator", "Online batch translation failed", e)
            if (!networkAvailable) {
                translateBlocks(visionText, sourceLangCode, bitmap)
            }
        } finally {
            blocks.indices.forEach { overlayManager.removeLoading(it.toString()) }
        }
    }

    // ISSUE-010 FIX: Clean up all resources
    fun close() {
        scope.cancel()
        preloadScope.cancel()
        runCatching { languageIdentifier?.close() }
        languageIdentifier = null
        runCatching { activeRecognizer?.close() }
        activeRecognizer = null
        translatorCache.values.forEach { runCatching { it.close() } }
        translatorCache.clear()
        synchronized(bitmapLock) {
            activeBitmap?.let { runCatching { if (!it.isRecycled) it.recycle() } }
            activeBitmap = null
        }
        overlayManager.clearOverlays()
    }

    fun clearOverlays() {
        activeJob?.cancel()
        activeJob = null
        runCatching { activeRecognizer?.close() }
        activeRecognizer = null
        overlayManager.clearOverlays()
    }

    fun onConnectivityChanged(available: Boolean) {
        networkAvailable = available
        if (!available && config.translationMode != "offline") {
            preloadScope.launch(Dispatchers.IO) { preloadOfflineModel(force = true) }
        }
    }

    fun hardPause() {
        activeJob?.cancel()
        activeJob = null
        runCatching { activeRecognizer?.close() }
        activeRecognizer = null
        overlayManager.clearOverlays()
    }

    // PHASE 8 FIX: Smart OCR block merging.
    //
    // ML Kit often returns one speech bubble as several [Text.TextBlock] entries
    // (each line is sometimes its own block). Drawing each one separately
    // produces overlapping stacked overlays. We greedily walk the blocks in
    // top-to-bottom order and fuse them into a [MergedBlock] when all three
    // conditions hold:
    //
    //  1. Vertical proximity — the gap between the previous block's bottom and
    //     the next block's top is less than [MERGE_VERTICAL_GAP_MULTIPLIER]
    //     times the average line height. This prevents fusing bubbles that are
    //     several lines apart.
    //  2. Same column — the horizontal ranges overlap or sit very close
    //     together ([MERGE_HORIZONTAL_GAP_RATIO] of the wider block). Blocks
    //     that sit in clearly different columns stay separate.
    //  3. Similar font size — the per-line height of both blocks differs by at
    //     most [MERGE_SIZE_TOLERANCE]. A tiny dialogue line next to a giant SFX
    //     burst therefore stays separated even when their boxes touch.
    //
    // If merged, the text is joined with `\n` and the bounding box becomes the
    // union of both rectangles.
    private fun mergeBlocks(blocks: List<Text.TextBlock>, bitmap: Bitmap): List<MergedBlock> {
        val allLines = blocks.flatMap { it.lines }
        val fragments = allLines.mapNotNull { line ->
            val rect = line.boundingBox ?: return@mapNotNull null
            if (line.text.isBlank() || rect.isEmpty) return@mapNotNull null
            val rotation = if (config.isAutoRotateEnabled) {
                line.cornerPoints?.takeIf {
                    it.size >= 2
                }?.let { p -> Math.toDegrees(kotlin.math.atan2((p[1].y - p[0].y).toDouble(), (p[1].x - p[0].x).toDouble())).toFloat() } ?: 0f
            } else {
                0f
            }
            val backgroundColor = getDominantBackgroundColor(bitmap, rect)
            MergedBlock(
                line.text.trim(),
                Rect(rect),
                rect.height().toFloat(),
                rotation,
                backgroundColor,
                getBodyTextColor(bitmap, rect, backgroundColor),
            )
        }
        if (fragments.isEmpty()) return emptyList()

        // Sort top-to-bottom; for blocks sharing a row, left-to-right. This keeps
        // a single bubble's fragments in the right read order before fusing.
        val sorted = fragments.sortedWith(
            compareBy<MergedBlock> { it.rect.top }
                .thenBy { it.rect.left },
        )

        val merged = mutableListOf<MergedBlock>()

        for (next in sorted) {
            var mergedIntoExisting = false

            // Cek dari grup terbaru ke terlama agar lebih akurat secara spasial
            for (i in merged.indices.reversed()) {
                val current = merged[i]
                val avgLineHeight = maxOf(current.lineHeight, next.lineHeight)

                // Jarak vertikal dihitung dari ujung bawah grup (union) ke ujung atas baris baru
                val verticalGap = (next.rect.top - current.rect.bottom).coerceAtLeast(0)
                val closeVertically = verticalGap <= config.mergeVerticalGapMultiplier * avgLineHeight

                val overlapHorizontally = next.rect.left <= current.rect.right &&
                    next.rect.right >= current.rect.left

                val widthA = current.rect.width()
                val widthB = next.rect.width()
                val maxWidth = maxOf(widthA, widthB).coerceAtLeast(1)

                val horizontalGap = when {
                    overlapHorizontally -> 0
                    next.rect.left > current.rect.right -> next.rect.left - current.rect.right
                    else -> current.rect.left - next.rect.right
                }

                val closeHorizontally = horizontalGap < config.mergeHorizontalGapRatio * maxWidth
                val sizeTolerance = maxOf(current.lineHeight, next.lineHeight) * config.mergeSizeTolerance
                val similarSize = kotlin.math.abs(current.lineHeight - next.lineHeight) <= sizeTolerance

                // Jika memenuhi syarat, gabungkan ke dalam grup ini
                if (closeVertically && closeHorizontally && similarSize) {
                    val union = Rect(current.rect).apply { union(next.rect) }
                    merged[i] = MergedBlock(
                        text = current.text + "\n" + next.text,
                        rect = union,
                        lineHeight = avgLineHeight,
                        rotation = (current.rotation + next.rotation) / 2f,
                        sampledColor = current.sampledColor,
                        detectedTextColor = current.detectedTextColor,
                    )
                    mergedIntoExisting = true
                    break
                }
            }

            // Jika tidak cocok dengan grup mana pun, buat grup/balon baru
            if (!mergedIntoExisting) {
                merged.add(next)
            }
        }

        return merged
    }

    private fun getDominantBackgroundColor(bitmap: Bitmap, rect: Rect): Int {
        val left = rect.left.coerceIn(0, bitmap.width - 1)
        val right = (rect.right - 1).coerceIn(0, bitmap.width - 1)
        val top = rect.top.coerceIn(0, bitmap.height - 1)
        val bottom = (rect.bottom - 1).coerceIn(0, bitmap.height - 1)
        if (right < left || bottom < top) return android.graphics.Color.WHITE
        val counts = HashMap<Int, Int>()
        fun sample(x: Int, y: Int) {
            val color = bitmap.getPixel(x, y)
            counts[color] = (counts[color] ?: 0) + 1
        }
        for (x in left..right) {
            sample(x, top)
            sample(x, bottom)
        }
        for (y in (top + 1) until bottom) {
            sample(left, y)
            sample(right, y)
        }
        return counts.maxByOrNull { it.value }?.key ?: bitmap.getPixel(left, top)
    }

    /**
     * Finds the glyph fill instead of its outline. Colors are quantized to absorb
     * antialiasing, background-like pixels are rejected, and candidates touching
     * the background frequently are penalized because those pixels are normally
     * the outer stroke or shadow. The fill sits inside that stroke and therefore
     * has substantially fewer background neighbours.
     */
    private fun getBodyTextColor(bitmap: Bitmap, rect: Rect, backgroundColor: Int): Int {
        val left = rect.left.coerceIn(0, bitmap.width - 1)
        val right = (rect.right - 1).coerceIn(left, bitmap.width - 1)
        val top = rect.top.coerceIn(0, bitmap.height - 1)
        val bottom = (rect.bottom - 1).coerceIn(top, bitmap.height - 1)
        val insetX = ((right - left + 1) * 0.08f).toInt()
        val insetY = ((bottom - top + 1) * 0.08f).toInt()
        val counts = HashMap<Int, Int>()
        val backgroundTouches = HashMap<Int, Int>()

        fun quantize(color: Int): Int = android.graphics.Color.rgb(
            android.graphics.Color.red(color) and 0xE0,
            android.graphics.Color.green(color) and 0xE0,
            android.graphics.Color.blue(color) and 0xE0,
        )
        fun distance(a: Int, b: Int): Double {
            val dr = android.graphics.Color.red(a) - android.graphics.Color.red(b)
            val dg = android.graphics.Color.green(a) - android.graphics.Color.green(b)
            val db = android.graphics.Color.blue(a) - android.graphics.Color.blue(b)
            return kotlin.math.sqrt((dr * dr + dg * dg + db * db).toDouble())
        }
        val bg = quantize(backgroundColor)
        for (y in (top + insetY)..(bottom - insetY).coerceAtLeast(top + insetY)) {
            for (x in (left + insetX)..(right - insetX).coerceAtLeast(left + insetX)) {
                val color = quantize(bitmap.getPixel(x, y))
                if (distance(color, bg) < 56.0) continue
                counts[color] = (counts[color] ?: 0) + 1
                val touchesBackground = listOf(x - 1 to y, x + 1 to y, x to y - 1, x to y + 1).any { (nx, ny) ->
                    nx !in 0 until bitmap.width || ny !in 0 until bitmap.height || distance(quantize(bitmap.getPixel(nx, ny)), bg) < 48.0
                }
                if (touchesBackground) backgroundTouches[color] = (backgroundTouches[color] ?: 0) + 1
            }
        }
        val winner = counts.maxByOrNull { (color, count) ->
            val exposedRatio = (backgroundTouches[color] ?: 0).toDouble() / count.coerceAtLeast(1)
            count * distance(color, bg) * (1.0 - exposedRatio.coerceIn(0.0, 0.9))
        }?.key ?: return android.graphics.Color.BLACK
        return winner or 0xFF000000.toInt()
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

        // PHASE 8 FIX: Smart merge tuning. Tuned empirically for manga/comic
        // dialogue vs SFX, but conservative enough to keep clearly distinct
        // bubbles separate.
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
