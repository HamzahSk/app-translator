package com.rocat.translator

import android.content.Context
import android.content.SharedPreferences

class ConfigManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ScreenTranslatorPrefs", Context.MODE_PRIVATE)

    var floatingBallSizeDp: Int
        get() = prefs.getInt("floatingBallSizeDp", 35)
        set(value) = prefs.edit().putInt("floatingBallSizeDp", value.coerceIn(28, 72)).apply()

    // ---------- General ----------
    var inactivityDelayMs: Long
        get() = prefs.getLong("inactivityDelayMs", 1500L)
        set(value) = prefs.edit().putLong("inactivityDelayMs", value).apply()

    var appTheme: Int
        get() = prefs.getInt("appTheme", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putInt("appTheme", value).apply()

    // ---------- Language ----------
    var targetLanguage: String
        get() = prefs.getString("targetLanguage", "id") ?: "id"
        set(value) = prefs.edit().putString("targetLanguage", value).apply()

    var sourceLanguage: String
        get() = prefs.getString("sourceLanguage", "auto") ?: "auto"
        set(value) = prefs.edit().putString("sourceLanguage", value).apply()

    var appLanguage: String
        get() = prefs.getString("appLanguage", "system") ?: "system"
        set(value) = prefs.edit().putString("appLanguage", value).apply()

    // ---------- Overlay Customization ----------

    // "direct" = over original text, "left" = bubble to the left, "right" = bubble to the right
    var placementMode: String
        get() = prefs.getString("placementMode", "direct") ?: "direct"
        set(value) = prefs.edit().putString("placementMode", value).apply()

    // 0-255 alpha for bubble background
    var overlayOpacity: Int
        get() = prefs.getInt("overlayOpacity", 230)
        set(value) = prefs.edit().putInt("overlayOpacity", value).apply()

    // Bubble corner radius in dp
    var bubbleCornerRadius: Int
        get() = prefs.getInt("bubbleCornerRadius", 12)
        set(value) = prefs.edit().putInt("bubbleCornerRadius", value).apply()

    // Translated text size in sp
    var overlayTextSize: Int
        get() = prefs.getInt("overlayTextSize", 12)
        set(value) = prefs.edit().putInt("overlayTextSize", value).apply()

    var autoTextFitEnabled: Boolean
        get() = prefs.getBoolean("autoTextFitEnabled", false)
        set(value) = prefs.edit().putBoolean("autoTextFitEnabled", value).apply()

    var isAutoRotateEnabled: Boolean
        get() = prefs.getBoolean("isAutoRotateEnabled", false)
        set(value) = prefs.edit().putBoolean("isAutoRotateEnabled", value).apply()
    var isTransparentModeEnabled: Boolean
        get() = prefs.getBoolean("isTransparentModeEnabled", false)
        set(value) = prefs.edit().putBoolean("isTransparentModeEnabled", value).apply()
    var outlineThickness: Float
        get() = prefs.getFloat("outlineThickness", 2f)
        set(value) = prefs.edit().putFloat("outlineThickness", value.coerceIn(1f, 10f)).apply()
    var outlineColor: String
        get() = prefs.getString("outlineColor", "#80FFFFFF") ?: "#80FFFFFF"
        set(value) = prefs.edit().putString("outlineColor", value).apply()
    var isEraserModeEnabled: Boolean
        get() = prefs.getBoolean("isEraserModeEnabled", false)
        set(value) = prefs.edit().putBoolean("isEraserModeEnabled", value).apply()
    var mergeVerticalGapMultiplier: Float
        get() = prefs.getFloat("mergeVerticalGapMultiplier", 1.25f)
        set(value) = prefs.edit().putFloat("mergeVerticalGapMultiplier", value.coerceIn(0.5f, 3f)).apply()
    var mergeHorizontalGapRatio: Float
        get() = prefs.getFloat("mergeHorizontalGapRatio", 0.12f)
        set(value) = prefs.edit().putFloat("mergeHorizontalGapRatio", value.coerceIn(0f, 0.8f)).apply()
    var mergeSizeTolerance: Float
        get() = prefs.getFloat("mergeSizeTolerance", 0.30f)
        set(value) = prefs.edit().putFloat("mergeSizeTolerance", value.coerceIn(0f, 1f)).apply()

    // Bubble background color as ARGB hex string (without alpha)
    var bubbleBgColor: String
        get() = prefs.getString("bubbleBgColor", "#FFFFFF") ?: "#FFFFFF"
        set(value) = prefs.edit().putString("bubbleBgColor", value).apply()

    // Bubble text color as ARGB hex string
    var bubbleTextColor: String
        get() = prefs.getString("bubbleTextColor", "#000000") ?: "#000000"
        set(value) = prefs.edit().putString("bubbleTextColor", value).apply()

    var autoDetectTextColor: Boolean
        get() = prefs.getBoolean("autoDetectTextColor", false)
        set(value) = prefs.edit().putBoolean("autoDetectTextColor", value).apply()

    // Show border on bubble
    var bubbleBorderEnabled: Boolean
        get() = prefs.getBoolean("bubbleBorderEnabled", true)
        set(value) = prefs.edit().putBoolean("bubbleBorderEnabled", value).apply()

    // Auto-clear translation overlays after X seconds (0 = manual only)
    var autoClearSeconds: Int
        get() = prefs.getInt("autoClearSeconds", 0)
        set(value) = prefs.edit().putInt("autoClearSeconds", value).apply()

    // ---------- Model Tracking ----------
    fun isModelInstalled(langCode: String): Boolean {
        return prefs.getBoolean("installed_model_$langCode", false)
    }

    fun setModelInstalled(langCode: String, installed: Boolean) {
        prefs.edit().putBoolean("installed_model_$langCode", installed).apply()
    }

    // ---------- Translation Mode (Online) ----------

    // "offline" = on-device ML Kit, "online" = custom AI API
    var translationMode: String
        get() = prefs.getString("translationMode", "offline") ?: "offline"
        set(value) = prefs.edit().putString("translationMode", value).apply()

    // "openai" or "gemini"
    var apiProvider: String
        get() = prefs.getString("apiProvider", "openai") ?: "openai"
        set(value) = prefs.edit().putString("apiProvider", value).apply()

    // Custom base URL. Empty = provider default.
    var apiBaseUrl: String
        get() = prefs.getString("apiBaseUrl", "") ?: ""
        set(value) = prefs.edit().putString("apiBaseUrl", value).apply()

    var apiKey: String
        get() = prefs.getString("apiKey", "") ?: ""
        set(value) = prefs.edit().putString("apiKey", value).apply()

    var apiModel: String
        get() = prefs.getString("apiModel", "gpt-4o-mini") ?: "gpt-4o-mini"
        set(value) = prefs.edit().putString("apiModel", value).apply()
}
