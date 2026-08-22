package com.ervareza.screentranslator

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
                    paint.textSize = config.overlayTextSize.toFloat() * resources.displayMetrics.scaledDensity
                    valid.forEach { bubble ->
                        val original = Rect(bubble.bounds)
                        val r = Rect(original)
                        // Placement controls text alignment inside the original box;
                        // the canvas/bubble anchor remains tied to the OCR bounds.
                        paint.color = Color.parseColor(config.bubbleTextColor)
                        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = paint.color
                            textSize = paint.textSize
                            typeface = paint.typeface
                        }
                        val layout = StaticLayout.Builder.obtain(bubble.text, 0, bubble.text.length, textPaint, r.width().coerceAtLeast(1))
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
                        val padding = dpToPx(10)
                        val width = layout.width + padding * 2
                        val height = layout.height + padding * 2
                        r.left = original.centerX() - width / 2
                        r.right = r.left + width
                        r.top = original.centerY() - height / 2
                        r.bottom = r.top + height
                        paint.color = Color.argb(config.overlayOpacity, Color.red(bg), Color.green(bg), Color.blue(bg))
                        canvas.drawRoundRect(r.left.toFloat(), r.top.toFloat(), r.right.toFloat(), r.bottom.toFloat(), dpToPx(config.bubbleCornerRadius).toFloat(), dpToPx(config.bubbleCornerRadius).toFloat(), paint)
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
                boundingBox.width().coerceAtLeast(dpToPx(100)),
                boundingBox.height().coerceAtLeast(dpToPx(32)),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = boundingBox.left
                y = boundingBox.top
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
