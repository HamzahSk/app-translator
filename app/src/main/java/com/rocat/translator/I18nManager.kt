package com.rocat.translator

import android.content.Context
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.util.Locale

/** Lightweight XML string catalog loader with an English fallback. */
class I18nManager(
    private val context: Context,
    language: String = Locale.getDefault().language,
    private val rootDirectory: File = context.filesDir // Menggunakan filesDir sebagai root directory default
) {
    private val strings: Map<String, String> = buildMap {
        putAll(loadCatalog("en"))
        val normalizedLanguage = language.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
        if (normalizedLanguage != "en") putAll(loadCatalog(normalizedLanguage))
    }

    fun get(key: String, fallback: String = key): String = strings[key] ?: fallback

    private fun loadCatalog(language: String): Map<String, String> = runCatching {
        // Membaca dari rootDirectory/i18n/{language}/strings.xml alih-alih dari assets
        val file = File(rootDirectory, "i18n/$language/strings.xml")
        file.inputStream().use { input ->
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(input, Charsets.UTF_8.name())
            }
            buildMap {
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "string") {
                        val name = parser.getAttributeValue(null, "name")
                        if (!name.isNullOrBlank()) put(name, parser.nextText())
                    }
                    event = parser.next()
                }
            }
        }
    }.getOrElse { emptyMap() }
}
