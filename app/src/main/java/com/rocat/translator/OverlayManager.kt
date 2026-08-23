package com.rocat.translator

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val config = ConfigManager(context)
    private val handler = Handler(Looper.getMainLooper())

    // ISSUE-005 FIX: Thread-safe list to prevent ConcurrentModificationException
    private val activeViews = CopyOnWriteArrayList<View>()
    private val loadingViews = ConcurrentHashMap<String, View>()

    @Volatile private var batchId = 0L
    private var batchView: View? = null
    private val customTypeface: Typeface? by lazy {
        runCatching { Typeface.createFromAsset(context.assets, "fonts/comic_font.ttf") }.getOrNull()
    }

    data class Bubble(val text: String, val bounds: Rect)

    // FASE 6 FIX: Adjustable line-spacing so translated lines don't collide.
    private companion object {
        const val LINE_SPACING_ADD = 4f
        const val LINE_SPACING_MULTIPLIER = 1.2f
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun drawTranslationBubble(translatedText: String, boundingBox: Rect) {
        drawTranslationBatch(listOf(Bubble(translatedText, boundingBox)))
    }

    fun drawTranslationBatch(bubbles: List<Bubble>) {
        handler.post {
            val valid = bubbles.filter { !it.bounds.isEmpty }
            if (valid.isEmpty()) return@post
            clearOverlaysInternal()
            val id = ++batchId
            val view = object : View(context) {
                private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                override fun onDraw(canvas: Canvas) {
                    val bg = Color.parseColor(config.bubbleBgColor)
                    paint.typeface = customTypeface
                    valid.forEach { bubble ->
                        val original = Rect(bubble.bounds)
                        val r = Rect(original)
                        // Placement controls text alignment inside the original box;
                        // the canvas/bubble anchor remains tied to the OCR bounds.
                        paint.color = Color.parseColor(config.bubbleTextColor)
                        val padding = dpToPx(10)
                        // Gunakan ukuran kotak OCR seutuhnya agar teks tidak kekecilan
                        val maxWidth = original.width().coerceAtLeast(1)
                        val maxHeight = original.height().coerceAtLeast(1)
                        val density = resources.displayMetrics.scaledDensity
                        val textSize = if (config.autoTextFitEnabled) findFittingTextSize(bubble.text, maxWidth, maxHeight, density) else config.overlayTextSize.toFloat() * density
                        paint.textSize = textSize
                        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = paint.color
                            this.textSize = paint.textSize
                            typeface = paint.typeface
                        }
                        var layout = StaticLayout.Builder.obtain(bubble.text, 0, bubble.text.length, textPaint, maxWidth)
                            .setAlignment(
                                when (config.placementMode) {
                                    "left" -> Layout.Alignment.ALIGN_NORMAL
                                    "right" -> Layout.Alignment.ALIGN_OPPOSITE
                                    else -> Layout.Alignment.ALIGN_CENTER
                                },
                            )
                            .setIncludePad(false)
                            .setLineSpacing(LINE_SPACING_ADD, LINE_SPACING_MULTIPLIER)
                            .build()
                        val contentWidth = if (config.autoTextFitEnabled) {
                            (0 until layout.lineCount).maxOfOrNull { line ->
                                kotlin.math.ceil(layout.getLineWidth(line).toDouble()).toInt()
                            }?.coerceIn(1, maxWidth) ?: 1
                        } else {
                            layout.width
                        }
                        if (config.autoTextFitEnabled && contentWidth != layout.width) {
                            layout = StaticLayout.Builder.obtain(bubble.text, 0, bubble.text.length, textPaint, contentWidth)
                                .setAlignment(
                                    when (config.placementMode) {
                                        "left" -> Layout.Alignment.ALIGN_NORMAL
                                        "right" -> Layout.Alignment.ALIGN_OPPOSITE
                                        else -> Layout.Alignment.ALIGN_CENTER
                                    },
                                )
                                .setIncludePad(false)
                                .setLineSpacing(LINE_SPACING_ADD, LINE_SPACING_MULTIPLIER)
                                .build()
                        }
                        val width = contentWidth + padding * 2
                        val height = layout.height + padding * 2
                        r.left = original.centerX() - width / 2
                        r.right = r.left + width
                        r.top = original.centerY() - height / 2
                        r.bottom = r.top + height

                        // Tentukan radius: pill shape (height/2) untuk auto, atau dari setting untuk manual
                        val cornerRadius = if (config.autoTextFitEnabled) {
                            height / 2f
                        } else {
                            dpToPx(config.bubbleCornerRadius).toFloat()
                        }

                        paint.color = Color.argb(config.overlayOpacity, Color.red(bg), Color.green(bg), Color.blue(bg))
                        canvas.drawRoundRect(
                            r.left.toFloat(),
                            r.top.toFloat(),
                            r.right.toFloat(),
                            r.bottom.toFloat(),
                            cornerRadius,
                            cornerRadius,
                            paint,
                        )
                        if (config.bubbleBorderEnabled) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = dpToPx(1).toFloat().coerceAtLeast(1f)
                            paint.color = Color.argb(
                                220,
                                Color.red(Color.parseColor(config.bubbleTextColor)),
                                Color.green(Color.parseColor(config.bubbleTextColor)),
                                Color.blue(Color.parseColor(config.bubbleTextColor)),
                            )
                            canvas.drawRoundRect(
                                r.left.toFloat(),
                                r.top.toFloat(),
                                r.right.toFloat(),
                                r.bottom.toFloat(),
                                cornerRadius,
                                cornerRadius,
                                paint,
                            )
                            paint.style = Paint.Style.FILL
                        }
                        canvas.save()
                        canvas.translate((r.left + padding).toFloat(), (r.top + padding).toFloat())
                        layout.draw(canvas)
                        canvas.restore()
                    }
                }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            if (id != batchId) return@post
            runCatching {
                windowManager.addView(view, params)
                batchView = view
                activeViews.add(view)
            }

            val autoClear = config.autoClearSeconds
            if (autoClear > 0) {
                handler.postDelayed({
                    try {
                        if (batchId == id) {
                            windowManager.removeView(view)
                            activeViews.remove(view)
                            batchView = null
                        }
                    } catch (_: IllegalArgumentException) {}
                }, autoClear * 1000L)
            }
        }
    }

    private fun findFittingTextSize(text: String, maxWidth: Int, maxHeight: Int, density: Float): Float {
        var low = 1f
        var high = 48f * density
        repeat(12) {
            val mid = (low + high) / 2f
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = mid
                typeface = customTypeface
            }
            val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, maxWidth).setIncludePad(false).setLineSpacing(LINE_SPACING_ADD, LINE_SPACING_MULTIPLIER).build()
            if (layout.height <= maxHeight) low = mid else high = mid
        }
        return low
    }

    fun drawLoadingBubble(boundingBox: Rect, key: String) {
        handler.post {
            if (boundingBox.isEmpty) return@post
            val view = TextView(context).apply {
                text = "…"
                setTextColor(Color.WHITE)
                textSize = 18f
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.argb(150, 80, 80, 80))
                    cornerRadius = dpToPx(config.bubbleCornerRadius).toFloat()
                }
            }
            val params = WindowManager.LayoutParams(
                boundingBox.width().coerceAtLeast(1),
                boundingBox.height().coerceAtLeast(1),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = boundingBox.left.coerceAtLeast(0)
                y = boundingBox.top.coerceAtLeast(0)
            }
            runCatching {
                windowManager.addView(view, params)
                activeViews.add(view)
                loadingViews[key] = view
            }
        }
    }

    fun replaceLoading(key: String, translatedText: String, boundingBox: Rect) {
        handler.post {
            loadingViews.remove(key)?.let { view ->
                runCatching { windowManager.removeView(view) }
                activeViews.remove(view)
            }
            drawTranslationBubble(translatedText, boundingBox)
        }
    }

    fun removeLoading(key: String) {
        handler.post {
            loadingViews.remove(key)?.let { view ->
                runCatching { windowManager.removeView(view) }
                activeViews.remove(view)
            }
        }
    }

    fun clearOverlays() {
        handler.post {
            clearOverlaysInternal()
        }
    }

    private fun clearOverlaysInternal() {
        batchId++
        for (view in activeViews) {
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {}
        }
        activeViews.clear()
        loadingViews.clear()
        batchView = null
    }
}
