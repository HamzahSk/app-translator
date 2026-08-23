package com.rocat.translator

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class InactivityAccessibilityService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())

    // Use nullable instead of lateinit to prevent crash when onAccessibilityEvent
    // fires before onServiceConnected
    private var config: ConfigManager? = null
    private var serviceReady = false

    private val triggerTranslationRunnable = Runnable {
        Log.d("Translator", "Inactivity detected. Triggering translation...")
        try {
            // Explicit broadcast with package name so it reaches RECEIVER_NOT_EXPORTED
            val intent = Intent("com.rocat.translator.TRIGGER_CAPTURE")
            intent.setPackage("com.rocat.translator")
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("Translator", "Failed to send broadcast", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            config = ConfigManager(this)
            serviceReady = true
            Log.d("Translator", "Accessibility Service Connected")
            resetTimer()
        } catch (e: Exception) {
            Log.e("Translator", "Error in onServiceConnected", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (TranslationControlState.paused) return
        // Guard: do nothing if service is not fully initialized yet
        if (!serviceReady || config == null || event == null) return
    
        // Ignore events from our own app so we don't clear overlays when we add them
        if (event.packageName == "com.rocat.translator") return
    
        // TAMBAHKAN TYPE_WINDOW_CONTENT_CHANGED DI BAWAH INI
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            // Clear current overlays immediately upon movement or app switch
            val clearIntent = Intent("com.rocat.translator.CLEAR_OVERLAY")
            clearIntent.setPackage("com.rocat.translator")
            sendBroadcast(clearIntent)
    
            resetTimer()
        }
    }
    
    private fun resetTimer() {
        handler.removeCallbacks(triggerTranslationRunnable)
        val delay = config?.inactivityDelayMs ?: 3000L
        handler.postDelayed(triggerTranslationRunnable, delay)
    }

    override fun onInterrupt() {
        handler.removeCallbacks(triggerTranslationRunnable)
        serviceReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(triggerTranslationRunnable)
        serviceReady = false
    }
}
