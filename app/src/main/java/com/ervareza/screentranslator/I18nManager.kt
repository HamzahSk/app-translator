package com.ervareza.screentranslator

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Lightweight, translator-friendly JSON string catalog loader. */
class I18nManager(private val context: Context, language: String = "en") {
    private val strings: Map<String, String> = runCatching {
        val path = "i18n/$language/strings.json"
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        Gson().fromJson<Map<String, String>>(json, object : TypeToken<Map<String, String>>() {}.type)
    }.getOrElse { emptyMap() }

    fun get(key: String, fallback: String = key): String = strings[key] ?: fallback
}
