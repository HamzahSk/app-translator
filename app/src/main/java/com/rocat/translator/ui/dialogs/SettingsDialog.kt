package com.rocat.translator.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.TransitionManager
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
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.rocat.translator.ConfigManager
import com.rocat.translator.I18nManager
import com.rocat.translator.R

/**
 * Modal editor for overlay appearance and OCR grouping preferences.
 *
 * Values are persisted immediately through [ConfigManager], allowing the active translation
 * service to consume updated settings without coupling the dialog to service lifecycle state.
 */
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
            delayLabel.text = String.format(i18n.get("inactivity_delay"), v)
            config.inactivityDelayMs = (v * 1000).toLong()
        }
        delay.value = (config.inactivityDelayMs / 1000f).coerceIn(.5f, 10f)
        setDelay(delay.value)
        delay.addOnChangeListener { _, v, _ -> setDelay(v) }
        bindSlider(
            R.id.sliderSettingsOpacity,
            R.id.tvSettingsOpacity,
            (config.overlayOpacity * 100 / 255 / 5 * 5).toFloat(),
            i18n.get("bubble_opacity"),
        ) { config.overlayOpacity = (it * 255 / 100).toInt() }
        bindSlider(
            R.id.sliderSettingsCorner,
            R.id.tvSettingsCorner,
            config.bubbleCornerRadius.toFloat(),
            i18n.get("corner_radius"),
        ) { config.bubbleCornerRadius = it.toInt() }
        val textSizeSlider = findViewById<Slider>(R.id.sliderSettingsTextSize)
        bindSlider(
            R.id.sliderSettingsTextSize,
            R.id.tvSettingsTextSize,
            config.overlayTextSize.toFloat(),
            i18n.get("text_size"),
        ) { config.overlayTextSize = it.toInt() }
        textSizeSlider.isEnabled = !config.autoTextFitEnabled
        findViewById<MaterialSwitch>(R.id.settingsAutoTextFit).apply {
            text = i18n.get("auto_text_fit")
            isChecked = config.autoTextFitEnabled
            setOnCheckedChangeListener { _, value ->
                config.autoTextFitEnabled = value
                textSizeSlider.isEnabled = !value
            }
        }
        val textColorLayout = findViewById<TextInputLayout>(R.id.layoutSettingsTextColor)
        val textColorInput = findViewById<TextInputEditText>(R.id.inputSettingsTextColor).apply {
            setText(config.bubbleTextColor)
            doAfterTextChanged { value ->
                val color = value?.toString().orEmpty()
                if (runCatching { Color.parseColor(color) }.isSuccess) config.bubbleTextColor = color
            }
        }
        findViewById<MaterialSwitch>(R.id.settingsAutoTextColor).apply {
            text = i18n.get("auto_detect_text_color")
            isChecked = config.autoDetectTextColor
            textColorLayout.isEnabled = !isChecked
            setOnCheckedChangeListener { _, value ->
                config.autoDetectTextColor = value
                textColorLayout.isEnabled = !value
                textColorInput.isEnabled = !value
            }
        }
        findViewById<MaterialSwitch>(R.id.settingsAutoRotate).apply {
            text = i18n.get("auto_rotate_canvas")
            isChecked = config.isAutoRotateEnabled
            setOnCheckedChangeListener { _, value -> config.isAutoRotateEnabled = value }
        }
        val outlineConfig = findViewById<View>(R.id.layoutOutlineConfig)
        outlineConfig.visibility = if (config.isTransparentModeEnabled) View.VISIBLE else View.GONE
        findViewById<MaterialSwitch>(R.id.settingsTransparentMode).apply {
            text = i18n.get("transparent_mode")
            isChecked = config.isTransparentModeEnabled
            setOnCheckedChangeListener { _, value ->
                config.isTransparentModeEnabled = value
                TransitionManager.beginDelayedTransition(this@SettingsDialog.findViewById(R.id.settingsDialogRoot))
                outlineConfig.visibility = if (value) View.VISIBLE else View.GONE
            }
        }
        bindSlider(
            R.id.sliderSettingsOutlineThickness,
            R.id.tvSettingsOutlineThickness,
            config.outlineThickness,
            i18n.get("outline_thickness"),
        ) { config.outlineThickness = it }
        findViewById<TextInputEditText>(R.id.inputSettingsOutlineColor).apply {
            setText(config.outlineColor)
            doAfterTextChanged { value ->
                val color = value?.toString().orEmpty()
                if (runCatching { Color.parseColor(color) }.isSuccess) config.outlineColor = color
            }
        }
        findViewById<MaterialSwitch>(R.id.settingsEraserMode).apply {
            text = i18n.get("smart_eraser")
            isChecked = config.isEraserModeEnabled
            setOnCheckedChangeListener { _, value -> config.isEraserModeEnabled = value }
        }
        bindSlider(R.id.sliderSettingsMergeVertical, R.id.tvSettingsMergeVertical, config.mergeVerticalGapMultiplier, i18n.get("vertical_gap")) { config.mergeVerticalGapMultiplier = it }
        bindSlider(R.id.sliderSettingsMergeHorizontal, R.id.tvSettingsMergeHorizontal, config.mergeHorizontalGapRatio, i18n.get("horizontal_gap")) { config.mergeHorizontalGapRatio = it }
        bindSlider(R.id.sliderSettingsMergeSize, R.id.tvSettingsMergeSize, config.mergeSizeTolerance, i18n.get("size_tolerance")) { config.mergeSizeTolerance = it }
        findViewById<AutoCompleteTextView>(R.id.spinnerAppLanguage).apply {
            val labels = listOf(i18n.get("system_default"), i18n.get("english"), i18n.get("indonesian"))
            setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, labels))
            setText(
                labels[
                    when (config.appLanguage) {
                        "en" -> 1
                        "in" -> 2
                        else -> 0
                    },
                ],
                false,
            )
            setOnItemClickListener { _, _, position, _ ->
                config.appLanguage = listOf("system", "en", "in")[position]
                Toast.makeText(context, i18n.get("language_updated"), Toast.LENGTH_SHORT).show()
            }
        }
        bindSlider(
            R.id.sliderSettingsAutoClear,
            R.id.tvSettingsAutoClear,
            config.autoClearSeconds.toFloat(),
            i18n.get("auto_clear"),
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
