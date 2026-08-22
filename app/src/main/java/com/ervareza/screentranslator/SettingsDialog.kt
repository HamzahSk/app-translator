package com.ervareza.screentranslator

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.ImageButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class SettingsDialog(context: Context, private val config: ConfigManager) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle("Settings")
        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_settings, null))
        findViewById<ImageButton>(R.id.btnSettingsClose).setOnClickListener { dismiss() }
        window?.setLayout((context.resources.displayMetrics.widthPixels * 0.94).toInt(), -2)
        val delay = findViewById<Slider>(R.id.sliderSettingsDelay)
        val delayLabel = findViewById<TextView>(R.id.tvSettingsDelay)
        fun setDelay(value: Float) {
            val v = (value * 2).toInt() / 2f
            delayLabel.text = "Inactivity Delay: ${"%.1f".format(v)}s"
            config.inactivityDelayMs = (v * 1000).toLong()
        }
        delay.value = (config.inactivityDelayMs / 1000f).coerceIn(.5f, 10f)
        setDelay(delay.value)
        delay.addOnChangeListener { _, v, _ -> setDelay(v) }
        bindSlider(
            R.id.sliderSettingsOpacity,
            R.id.tvSettingsOpacity,
            (config.overlayOpacity * 100 / 255 / 5 * 5).toFloat(),
            "Bubble Opacity: %d%%",
        ) { config.overlayOpacity = (it * 255 / 100).toInt() }
        bindSlider(
            R.id.sliderSettingsCorner,
            R.id.tvSettingsCorner,
            config.bubbleCornerRadius.toFloat(),
            "Corner Radius: %ddp",
        ) { config.bubbleCornerRadius = it.toInt() }
        bindSlider(
            R.id.sliderSettingsTextSize,
            R.id.tvSettingsTextSize,
            config.overlayTextSize.toFloat(),
            "Text Size: %dsp",
        ) { config.overlayTextSize = it.toInt() }
        bindSlider(
            R.id.sliderSettingsAutoClear,
            R.id.tvSettingsAutoClear,
            config.autoClearSeconds.toFloat(),
            "Auto-Clear: %s",
        ) { config.autoClearSeconds = it.toInt() }
        bindSlider(R.id.sliderSettingsBall, null, config.floatingBallSizeDp.toFloat(), null) {
            config.floatingBallSizeDp = it.toInt()
        }
        val placement = findViewById<MaterialButtonToggleGroup>(R.id.settingsPlacement)
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
        border.isChecked = config.bubbleBorderEnabled
        border.setOnCheckedChangeListener { _, v -> config.bubbleBorderEnabled = v }
    }
    private fun bindSlider(sliderId: Int, labelId: Int?, initial: Float, format: String?, save: (Float) -> Unit) {
        val slider = findViewById<Slider>(sliderId)
        val label = labelId?.let { findViewById<TextView>(it) }
        fun apply(v: Float) {
            save(v)
            if (label != null && format != null) {
                label.text = if (format.contains("%s")) {
                    format.replace("%s", if (v == 0f) "Off" else "${v.toInt()}s")
                } else {
                    String.format(format, v.toInt())
                }
            }
        }
        slider.value = initial.coerceIn(slider.valueFrom, slider.valueTo)
        apply(slider.value)
        slider.addOnChangeListener { _, v, _ -> apply(v) }
    }
}
