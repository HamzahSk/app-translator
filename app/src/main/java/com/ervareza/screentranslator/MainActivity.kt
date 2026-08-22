package com.ervareza.screentranslator

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var config: ConfigManager
    private lateinit var i18n: I18nManager
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>

    private val recognizers: Map<String, TextRecognizer> by lazy {
        mapOf(
            "ja" to TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()),
            "ko" to TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()),
            "zh" to TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()),
            "hi" to TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build()),
            "en" to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
        )
    }

    private val langNames = mapOf(
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "hi" to "Devanagari",
        "en" to "Latin/English",
    )

    private val statusViews = mutableMapOf<String, TextView>()
    private lateinit var btnOverlay: MaterialButton
    private lateinit var btnAccessibility: MaterialButton
    private lateinit var btnNotification: MaterialButton
    private lateinit var fabStart: ExtendedFloatingActionButton

    // PHASE 7 FIX: Single source of truth for the Start button. The click
    // listener is registered exactly once and routes on this state, so one tap
    // always responds immediately and the "Preparing..." state is never stuck.
    private enum class StartState { IDLE, PREPARING, RUNNING, BLOCKED }

    @Volatile private var startState = StartState.IDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        config = ConfigManager(this)
        i18n = I18nManager(this)
        AppCompatDelegate.setDefaultNightMode(config.appTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register launchers before lifecycle starts
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra("resultCode", result.resultCode)
                    putExtra("data", result.data)
                }
                startForegroundService(serviceIntent)
                startState = StartState.RUNNING
                renderStartButton()
                Snackbar.make(fabStart, i18n.get("service_started"), Snackbar.LENGTH_LONG).show()
            } else {
                startState = StartState.IDLE
                renderStartButton()
                Snackbar.make(fabStart, i18n.get("screen_capture_denied"), Snackbar.LENGTH_LONG).show()
            }
        }

        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { _ ->
            refreshPermissionStatuses()
        }

        btnOverlay = findViewById(R.id.btnOverlayPermission)
        btnAccessibility = findViewById(R.id.btnAccessibilityPermission)
        btnNotification = findViewById(R.id.btnNotificationPermission)
        fabStart = findViewById(R.id.fabStartService)

        setupThemeToggle()
        setupSourceLanguageSpinner()
        setupTargetLanguageSpinner()
        setupOnlineMode()
        setupSettingsButton()
        setupAIModelsManager()
        setupPermissionsAndStart()
    }

    private val serviceStopReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.ervareza.screentranslator.SERVICE_STOPPED") {
                refreshPermissionStatuses()
                syncServiceState()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatuses()
        syncServiceState()

        val filter = android.content.IntentFilter("com.ervareza.screentranslator.SERVICE_STOPPED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(serviceStopReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(serviceStopReceiver, filter)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(serviceStopReceiver)
        } catch (e: Exception) {}
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    // ==================== THEME ====================
    private fun setupThemeToggle() {
        val toggleGroup = findViewById<MaterialButtonToggleGroup>(R.id.themeToggleGroup)
        when (config.appTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> toggleGroup.check(R.id.btnThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> toggleGroup.check(R.id.btnThemeDark)
            else -> toggleGroup.check(R.id.btnThemeSystem)
        }
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.btnThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.btnThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                if (config.appTheme != mode) {
                    config.appTheme = mode
                    AppCompatDelegate.setDefaultNightMode(mode)
                }
            }
        }
    }

    // ==================== LANGUAGE ====================
    private fun setupSourceLanguageSpinner() {
        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerSourceLanguage)
        val sourceCodes = listOf("auto", "ja", "ko", "zh", "hi", "en")
        val sourceDisplayNames = listOf(
            i18n.get("auto_detect"),
            i18n.get("japanese"),
            i18n.get("korean"),
            i18n.get("chinese"),
            i18n.get("devanagari"),
            i18n.get("latin_english"),
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sourceDisplayNames)
        spinner.setAdapter(adapter)
        val idx = sourceCodes.indexOf(config.sourceLanguage)
        spinner.setText(if (idx >= 0) sourceDisplayNames[idx] else sourceDisplayNames[0], false)
        spinner.setOnItemClickListener { _, _, position, _ -> config.sourceLanguage = sourceCodes[position] }
    }

    private fun setupTargetLanguageSpinner() {
        val spinner = findViewById<AutoCompleteTextView>(R.id.spinnerTargetLanguage)
        val codes = com.google.mlkit.nl.translate.TranslateLanguage.getAllLanguages()
        val names = codes.map { java.util.Locale(it).displayLanguage }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        spinner.setAdapter(adapter)
        val idx = codes.indexOf(config.targetLanguage)
        spinner.setText(if (idx >= 0) names[idx] else i18n.get("indonesian"), false)
        spinner.setOnItemClickListener { _, _, position, _ -> config.targetLanguage = codes[position] }
    }

    // ==================== ONLINE MODE ====================
    private fun setupOnlineMode() {
        val modeGroup = findViewById<MaterialButtonToggleGroup>(R.id.modeToggleGroup)
        val onlineConfig = findViewById<LinearLayout>(R.id.layoutOnlineConfig)
        val modelsCard = findViewById<View>(R.id.layoutModelsCard)
        fun applyMode(mode: String) {
            val offline = mode == "offline"
            onlineConfig.visibility = if (offline) View.GONE else View.VISIBLE
            modelsCard.visibility = if (offline) View.VISIBLE else View.GONE
            findViewById<TextView>(R.id.tvOnlineHint).text = if (offline) {
                i18n.get("offline_uses")
            } else {
                i18n.get("online_uses")
            }
        }
        when (config.translationMode) {
            "online" -> modeGroup.check(R.id.btnModeOnline)
            else -> modeGroup.check(R.id.btnModeOffline)
        }
        applyMode(config.translationMode)
        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                config.translationMode = if (checkedId == R.id.btnModeOnline) "online" else "offline"
                applyMode(config.translationMode)
            }
        }

        val providerSpinner = findViewById<AutoCompleteTextView>(R.id.spinnerApiProvider)
        val apiKeyLayout = findViewById<View>(R.id.layoutApiKey)
        val baseUrlLayout = findViewById<View>(R.id.layoutApiBaseUrl)
        fun applyProviderVisibility() {
            val visible = config.apiProvider != "default"
            apiKeyLayout.visibility = if (visible) View.VISIBLE else View.GONE
            baseUrlLayout.visibility = if (visible) View.VISIBLE else View.GONE
        }
        val providers = listOf(
            i18n.get("provider_openai"),
            i18n.get("provider_gemini"),
            i18n.get("provider_default"),
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, providers)
        providerSpinner.setAdapter(adapter)
        val providerIdx = when (config.apiProvider) { "gemini" -> 1; "default" -> 2; else -> 0 }
        providerSpinner.setText(providers[providerIdx], false)
        providerSpinner.setOnItemClickListener { _, _, position, _ ->
            config.apiProvider = when (position) { 1 -> "gemini"; 2 -> "default"; else -> "openai" }
            updateOnlineHint()
            applyProviderVisibility()
        }

        bindOnlineTextField(R.id.editApiKey) { config.apiKey = it }
        bindOnlineTextField(R.id.editApiBaseUrl) { config.apiBaseUrl = it }
        bindOnlineTextField(R.id.editApiModel) { config.apiModel = it }

        updateOnlineHint()
        applyProviderVisibility()
    }

    private fun bindOnlineTextField(viewId: Int, save: (String) -> Unit) {
        val edit = findViewById<TextInputEditText>(viewId)
        edit.setText(
            when (viewId) {
                R.id.editApiKey -> config.apiKey
                R.id.editApiBaseUrl -> config.apiBaseUrl
                else -> config.apiModel
            },
        )
        edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = save(s?.toString()?.trim().orEmpty())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateOnlineHint() {
        val hint = findViewById<TextView>(R.id.tvOnlineHint)
        hint.text = if (config.apiProvider == "gemini") {
            i18n.get("gemini_hint")
        } else {
            i18n.get("openai_hint")
        }
    }

    private fun setupSettingsButton() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val settingsItem = toolbar.menu.add(i18n.get("menu_settings"))
        settingsItem.setIcon(android.R.drawable.ic_menu_preferences)
        settingsItem.setShowAsAction(android.view.MenuItem.SHOW_AS_ACTION_ALWAYS)
        settingsItem.setOnMenuItemClickListener {
            SettingsDialog(this, config).show()
            true
        }
    }

    // ==================== AI MODELS ====================
    private fun setupAIModelsManager() {
        val container = findViewById<LinearLayout>(R.id.layoutModelsContainer)
        val btnDownloadAll = findViewById<MaterialButton>(R.id.btnDownloadAll)

        for (code in langNames.keys) {
            val name = i18n.get("lang_$code", langNames[code]!!)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 12)
            }
            val tvName = TextView(this).apply {
                text = name
                textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvStatus = TextView(this).apply {
                text = i18n.get("checking")
                textSize = 13f
                gravity = Gravity.END
            }
            row.addView(tvName)
            row.addView(tvStatus)
            statusViews[code] = tvStatus
            container.addView(row)
        }

        btnDownloadAll.setOnClickListener { downloadAllMissingModels() }
        checkModelStatuses()
    }

    private val translateModelCodes = mapOf(
        "ja" to TranslateLanguage.JAPANESE,
        "ko" to TranslateLanguage.KOREAN,
        "zh" to TranslateLanguage.CHINESE,
        "hi" to TranslateLanguage.HINDI,
        "en" to TranslateLanguage.ENGLISH,
    )

    private fun checkModelStatuses() {
        val modelManager = RemoteModelManager.getInstance()
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).addOnSuccessListener { models ->
            val downloadedTags = models.map { it.language }

            for ((code, langTag) in translateModelCodes) {
                val isInstalled = downloadedTags.contains(langTag)
                config.setModelInstalled(code, isInstalled)
                updateModelStatusUI(code, isInstalled)
            }
        }.addOnFailureListener {
            for (code in translateModelCodes.keys) {
                config.setModelInstalled(code, false)
                statusViews[code]?.text = i18n.get("error")
            }
        }
    }

    private fun updateModelStatusUI(code: String, installed: Boolean) {
        statusViews[code]?.text = if (installed) i18n.get("installed") else i18n.get("not_installed")
        statusViews[code]?.setTextColor(
            if (installed) {
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            } else {
                ContextCompat.getColor(this, android.R.color.holo_red_light)
            },
        )
    }

    private fun downloadAllMissingModels() {
        val modelManager = RemoteModelManager.getInstance()
        val conditions = DownloadConditions.Builder().build()

        Snackbar.make(fabStart, i18n.get("downloading_models"), Snackbar.LENGTH_LONG).show()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "ModelDownloadChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Model Downloads", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        for ((code, langTag) in translateModelCodes) {
            if (!config.isModelInstalled(code)) {
                val langDisplay = i18n.get("lang_$code", langNames[code]!!)
                statusViews[code]?.text = i18n.get("downloading")
                statusViews[code]?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))

                val notificationId = 200 + code.hashCode()
                val notificationBuilder = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentTitle(i18n.get("downloading_model", "Downloading %s Translation Model").format(langDisplay))
                    .setProgress(0, 0, true)
                    .setOngoing(true)

                notificationManager.notify(notificationId, notificationBuilder.build())

                val model = TranslateRemoteModel.Builder(langTag).build()
                modelManager.download(model, conditions)
                    .addOnSuccessListener {
                        config.setModelInstalled(code, true)
                        updateModelStatusUI(code, true)

                        notificationBuilder.setContentText(i18n.get("download_complete"))
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
                    .addOnFailureListener {
                        statusViews[code]?.text = i18n.get("failed")
                        statusViews[code]?.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

                        notificationBuilder.setContentText(i18n.get("download_failed"))
                            .setProgress(0, 0, false)
                            .setOngoing(false)
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
            }
        }
    }

    // ==================== PERMISSIONS ====================
    private fun isAccessibilityServiceEnabled(): Boolean {
        // Use Settings.Secure for reliable detection
        val expectedId = "$packageName/${InactivityAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabledServices.split(':').any { it.equals(expectedId, ignoreCase = true) }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun refreshPermissionStatuses() {
        val overlayOk = Settings.canDrawOverlays(this)
        btnOverlay.text = if (overlayOk) i18n.get("overlay_granted") else i18n.get("overlay_grant")
        btnOverlay.setIconResource(if (overlayOk) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background)
        btnOverlay.isEnabled = !overlayOk

        val accessOk = isAccessibilityServiceEnabled()
        btnAccessibility.text = if (accessOk) i18n.get("accessibility_enabled") else i18n.get("accessibility_enable")
        btnAccessibility.setIconResource(
            if (accessOk) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background,
        )
        btnAccessibility.isEnabled = !accessOk

        val notifOk = isNotificationPermissionGranted()
        btnNotification.text = if (notifOk) i18n.get("notification_granted") else i18n.get("notification_grant")
        btnNotification.setIconResource(
            if (notifOk) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background,
        )
        btnNotification.isEnabled = !notifOk

        // PHASE 7 FIX: Permission changes only affect the button when we are not
        // busy preparing or already running.
        if (startState != StartState.RUNNING && startState != StartState.PREPARING) {
            startState = if (overlayOk && accessOk) StartState.IDLE else StartState.BLOCKED
        }
        renderStartButton()
    }

    private fun renderStartButton() {
        when (startState) {
            StartState.BLOCKED -> {
                fabStart.isEnabled = false
                fabStart.text = i18n.get("grant_permissions_first")
                fabStart.setIconResource(android.R.drawable.ic_dialog_alert)
            }
            StartState.IDLE -> {
                fabStart.isEnabled = true
                fabStart.text = i18n.get("start_service")
                fabStart.setIconResource(android.R.drawable.ic_media_play)
            }
            StartState.PREPARING -> {
                fabStart.isEnabled = false
                fabStart.text = i18n.get("preparing")
                fabStart.setIconResource(android.R.drawable.ic_media_play)
            }
            StartState.RUNNING -> {
                fabStart.isEnabled = true
                fabStart.text = i18n.get("service_running")
                fabStart.setIconResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun setupPermissionsAndStart() {
        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }
        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btnNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // PHASE 7 FIX: Registered exactly once; state routing lives in the enum.
        fabStart.setOnClickListener {
            when (startState) {
                StartState.RUNNING -> stopService()
                StartState.IDLE -> startServiceFlow()
                StartState.PREPARING, StartState.BLOCKED -> Unit
            }
        }
        refreshPermissionStatuses()
    }

    private fun startServiceFlow() {
        if (startState != StartState.IDLE) return
        startState = StartState.PREPARING
        renderStartButton()

        // createScreenCaptureIntent() is cheap; build it instantly so the
        // permission dialog can open as soon as the async checks complete.
        val captureIntent = try {
            (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
        } catch (e: Exception) {
            null
        }
        if (captureIntent == null) {
            startState = StartState.IDLE
            renderStartButton()
            Snackbar.make(fabStart, i18n.get("failed_request_capture"), Snackbar.LENGTH_SHORT).show()
            return
        }

        // PHASE 7 FIX: Only the permission evaluation runs off-thread; the UI is
        // never locked and the button reacts on the first tap.
        lifecycleScope.launch {
            val allReady = withContext(Dispatchers.IO) {
                Settings.canDrawOverlays(this@MainActivity) && isAccessibilityServiceEnabled()
            }
            if (!allReady) {
                startState = StartState.BLOCKED
                renderStartButton()
                Snackbar.make(fabStart, i18n.get("grant_permissions_first_snack"), Snackbar.LENGTH_SHORT).show()
                return@launch
            }
            screenCaptureLauncher.launch(captureIntent)
        }
    }

    private fun stopService() {
        val stopBroadcast = Intent("com.ervareza.screentranslator.SERVICE_STOPPED")
        stopBroadcast.setPackage(packageName)
        sendBroadcast(stopBroadcast)
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = "ACTION_STOP"
        }
        startService(stopIntent)
        startState = StartState.IDLE
        renderStartButton()
    }

    private fun syncServiceState() {
        lifecycleScope.launch {
            // getRunningServices is deprecated and slow; always run it off-thread.
            val running = withContext(Dispatchers.IO) { isServiceRunning(ScreenCaptureService::class.java) }
            if (running) {
                startState = StartState.RUNNING
            } else if (startState == StartState.RUNNING) {
                val ready = Settings.canDrawOverlays(this@MainActivity) && isAccessibilityServiceEnabled()
                startState = if (ready) StartState.IDLE else StartState.BLOCKED
            }
            renderStartButton()
        }
    }
}
