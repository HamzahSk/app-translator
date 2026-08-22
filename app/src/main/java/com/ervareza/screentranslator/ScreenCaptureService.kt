package com.ervareza.screentranslator

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.MotionEvent
import android.view.View
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScreenCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var translationEngine: TranslationEngine

    // ISSUE-011 FIX: Screen capture + bitmap conversion run on a background thread.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ervareza.screentranslator.CLEAR_OVERLAY") {
                translationEngine.clearOverlays()
            } else {
                captureScreen()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        translationEngine = TranslationEngine(this)
        showControlBall()

        // Register broadcast receiver for the Accessibility Service trigger
        val filter = IntentFilter().apply {
            addAction("com.ervareza.screentranslator.TRIGGER_CAPTURE")
            addAction("com.ervareza.screentranslator.CLEAR_OVERLAY")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(captureReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_STOP") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            // Notify MainActivity to update FAB UI
            val stopBroadcast = Intent("com.ervareza.screentranslator.SERVICE_STOPPED")
            stopBroadcast.setPackage(packageName)
            sendBroadcast(stopBroadcast)

            return START_NOT_STICKY
        }

        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "ACTION_STOP"
        }
        val stopPendingIntent = android.app.PendingIntent.getService(
            this,
            0,
            stopIntent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(this, "ScreenTranslatorChannel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Screen Translator Active")
            .setContentText("Monitoring screen for translations...")
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(1, notification)
        }

        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

            // FIX: Android 14+ requires registering a callback before createVirtualDisplay
            mediaProjection?.registerCallback(
                object : android.media.projection.MediaProjection.Callback() {
                    override fun onStop() {
                        super.onStop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        val stopBroadcast = Intent("com.ervareza.screentranslator.SERVICE_STOPPED")
                        stopBroadcast.setPackage(packageName)
                        sendBroadcast(stopBroadcast)
                    }
                },
                android.os.Handler(android.os.Looper.getMainLooper()),
            )

            setupVirtualDisplay()
        }

        return START_NOT_STICKY
    }

    private fun setupVirtualDisplay() {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()

        val width: Int
        val height: Int
        val density: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = windowManager.currentWindowMetrics
            width = windowMetrics.bounds.width()
            height = windowMetrics.bounds.height()
            density = resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
        }

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenTranslatorCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null,
        )
    }

    private fun captureScreen() {
        if (TranslationControlState.paused) return
        Log.d("Translator", "Capturing screen...")
        serviceScope.launch(Dispatchers.IO) {
            val image = imageReader?.acquireLatestImage() ?: return@launch
            try {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * image.width

                val bitmap = Bitmap.createBitmap(
                    image.width + rowPadding / pixelStride,
                    image.height,
                    Bitmap.Config.ARGB_8888,
                )
                bitmap.copyPixelsFromBuffer(buffer)

                // Pass to Translation Engine (itself coroutine-based)
                translationEngine.processImage(bitmap)
            } finally {
                image.close()
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "ScreenTranslatorChannel",
            "Screen Translator Service",
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(captureReceiver)
        serviceScope.cancel()
        translationEngine.close()
        virtualDisplay?.release()
        mediaProjection?.stop()
        imageReader?.close()
        removeControlBall()
    }

    private var controlBall: View? = null
    private var controlParams: WindowManager.LayoutParams? = null
    private fun showControlBall() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val ballConfig = ConfigManager(this)
        val size = (ballConfig.floatingBallSizeDp * resources.displayMetrics.density).toInt()
        val view = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            override fun onDraw(canvas: Canvas) {
                paint.color = Color.rgb(35, 125, 210); canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint)
                paint.color = Color.WHITE
                if (TranslationControlState.paused) canvas.drawPath(android.graphics.Path().apply { moveTo(width * .42f, height * .3f); lineTo(width * .7f, height / 2f); lineTo(width * .42f, height * .7f); close() }, paint)
                else { canvas.drawRect(width * .35f, height * .3f, width * .45f, height * .7f, paint); canvas.drawRect(width * .55f, height * .3f, width * .65f, height * .7f, paint) }
            }
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())
        val hideRunnable = Runnable { val edge = if ((controlParams?.x ?: 0) < resources.displayMetrics.widthPixels / 2) 0 else resources.displayMetrics.widthPixels - size; view.animate().translationX((edge - (controlParams?.x ?: 0)).toFloat()).alpha(.5f).setDuration(250).start() }
        fun scheduleHide() { hideHandler.removeCallbacks(hideRunnable); hideHandler.postDelayed(hideRunnable, 5000) }
        view.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { view.animate().translationX(0f).alpha(1f).setDuration(150).start(); downX = e.rawX; downY = e.rawY; startX = controlParams?.x ?: 0; startY = controlParams?.y ?: 0; true }
                MotionEvent.ACTION_MOVE -> { controlParams?.let { it.x = startX + (e.rawX - downX).toInt(); it.y = startY + (e.rawY - downY).toInt(); wm.updateViewLayout(view, it) }; true }
                MotionEvent.ACTION_UP -> { if (kotlin.math.abs(e.rawX - downX) < 12 && kotlin.math.abs(e.rawY - downY) < 12) { TranslationControlState.paused = !TranslationControlState.paused; if (TranslationControlState.paused) translationEngine.hardPause(); view.invalidate() }; scheduleHide(); true }
                else -> true
            }
        }
        val params = WindowManager.LayoutParams(size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT).apply { gravity = android.view.Gravity.TOP or android.view.Gravity.START; x = 24; y = 180 }
        runCatching { wm.addView(view, params); controlBall = view; controlParams = params }
        scheduleHide()
    }
    private fun removeControlBall() { controlBall?.let { runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(it) } }; controlBall = null }

    override fun onBind(intent: Intent?): IBinder? = null
}
