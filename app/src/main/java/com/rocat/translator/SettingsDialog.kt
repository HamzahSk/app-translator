package com.rocat.translator

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.AttrRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class SettingsDialog(context: Context, private val config: ConfigManager) : Dialog(context) {
    private val i18n = I18nManager(context)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(i18n.get("settings_title"))
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_settings, null))
        applyThemeBackground()
        findViewById<ImageButton>(R.id.btnSettingsClose).apply {
            contentDescription = i18n.get("close")
            setOnClickListener { dismiss() }
        }
        findViewById<TextView>(R.id.tvSettingsTitle).text = i18n.get("overlay_customization")
        window?.setLayout((context.resources.displayMetrics.widthPixels * 0.94).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        val delay = findViewById<Slider>(R.id.sliderSettingsDelay)
        val delayLabel = findViewById<TextView>(R.id.tvSettingsDelay)
        fun setDelay(value: Float) {
            val v = (value * 2).toInt() / 2f
            delayLabel.text = String.format(i18n.get("inactivity_delay", "Inactivity Delay: %.1fs"), v)
            config.inactivityDelayMs = (v * 1000).toLong()
        }
        delay.value = (config.inactivityDelayMs / 1000f).coerceIn(.5f, 10f)
        setDelay(delay.value)
        delay.addOnChangeListener { _, v, _ -> setDelay(v) }
        bindSlider(
            R.id.sliderSettingsOpacity,
            R.id.tvSettingsOpacity,
            (config.overlayOpacity * 100 / 255 / 5 * 5).toFloat(),
            i18n.get("bubble_opacity", "Bubble Opacity: %d%%"),
        ) { config.overlayOpacity = (it * 255 / 100).toInt() }
        bindSlider(
            R.id.sliderSettingsCorner,
            R.id.tvSettingsCorner,
            config.bubbleCornerRadius.toFloat(),
            i18n.get("corner_radius", "Corner Radius: %ddp"),
        ) { config.bubbleCornerRadius = it.toInt() }
        bindSlider(
            R.id.sliderSettingsTextSize,
            R.id.tvSettingsTextSize,
            config.overlayTextSize.toFloat(),
            i18n.get("text_size", "Text Size: %dsp"),
        ) { config.overlayTextSize = it.toInt() }
        bindSlider(R.id.sliderSettingsGrouping, R.id.tvSettingsGrouping, config.paragraphGroupingMargin, "Paragraph Grouping / Margin: %.1fx") { config.paragraphGroupingMargin = it }
        findViewById<AutoCompleteTextView>(R.id.spinnerAppLanguage).apply {
            val labels = listOf("System Default", "English", "Indonesian")
            setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, labels))
            setText(
                labels[
                    when (config.appLanguage) {
                        "en" -> 1
                        "id" -> 2
                        else -> 0
                    },
                ],
                false,
            )
            setOnItemClickListener { _, _, position, _ ->
                config.appLanguage = listOf("system", "en", "id")[position]
                Toast.makeText(context, "Language updated. Restart to apply.", Toast.LENGTH_SHORT).show()
            }
        }
        bindSlider(
            R.id.sliderSettingsAutoClear,
            R.id.tvSettingsAutoClear,
            config.autoClearSeconds.toFloat(),
            i18n.get("auto_clear", "Auto-Clear: %s"),
        ) { config.autoClearSeconds = it.toInt() }
        bindSlider(R.id.sliderSettingsBall, null, config.floatingBallSizeDp.toFloat(), null) {
            config.floatingBallSizeDp = it.toInt()
        }
        findViewById<TextView>(R.id.tvSettingsBallSize).text = i18n.get("floating_ball_size")
        val placement = findViewById<MaterialButtonToggleGroup>(R.id.settingsPlacement)
        findViewById<MaterialButton>(R.id.settingsDirect).text = i18n.get("placement_direct")
        findViewById<MaterialButton>(R.id.settingsLeft).text = i18n.get("placement_left")
        findViewById<MaterialButton>(R.id.settingsRight).text = i18n.get("placement_right")
        placement.check(
            when (config.placementMode) {
                "left" -> R.id.settingsLeft
                "right" -> R.id.settingsRight
                else -> R.id.settingsDirect
            },
        )
        placement.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                config.placementMode = when (id) {
                    R.id.settingsLeft -> "left"
                    R.id.settingsRight -> "right"
                    else -> "direct"
                }
            }
        }
        val border = findViewById<MaterialSwitch>(R.id.settingsBorder)
        border.text = i18n.get("show_bubble_border")
        border.isChecked = config.bubbleBorderEnabled
        border.setOnCheckedChangeListener { _, v -> config.bubbleBorderEnabled = v }
    }

    private fun applyThemeBackground() {
        val content = findViewById<View>(R.id.settingsDialogRoot)
        content.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * context.resources.displayMetrics.density
            setColor(resolveThemeColor(com.google.android.material.R.attr.colorSurface, android.R.attr.colorBackground))
        }
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun resolveThemeColor(@AttrRes primary: Int, @AttrRes fallback: Int): Int {
        val value = TypedValue()
        if (context.theme.resolveAttribute(primary, value, true) || context.theme.resolveAttribute(fallback, value, true)) {
            return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
        }
        return Color.WHITE
    }

    private fun bindSlider(sliderId: Int, labelId: Int?, initial: Float, format: String?, save: (Float) -> Unit) {
        val slider = findViewById<Slider>(sliderId)
        val label = labelId?.let { findViewById<TextView>(it) }
        fun apply(v: Float) {
            save(v)
            if (label != null && format != null) {
                label.text = if (format.contains("%s")) {
                    format.replace("%s", if (v == 0f) i18n.get("off") else "${v.toInt()}s")
                } else {
                if (format.matches(".*%\\.?\\d*f.*".toRegex())) {
                    String.format(format, v)
                } else {
                    String.format(format, v.toInt())
                }
                }
            }
        }
        slider.value = initial.coerceIn(slider.valueFrom, slider.valueTo)
        apply(slider.value)
        slider.addOnChangeListener { _, v, _ -> apply(v) }
    }
}
