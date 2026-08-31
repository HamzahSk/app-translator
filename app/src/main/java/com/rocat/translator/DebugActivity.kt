package com.rocat.translator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.concurrent.Executors

class DebugActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val cacheExecutor = Executors.newSingleThreadExecutor()
    private lateinit var processContainer: LinearLayout
    private lateinit var emptyState: TextView

    private val refreshProcesses = object : Runnable {
        override fun run() {
            renderProcesses()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.debugToolbar).setNavigationOnClickListener { finish() }
        processContainer = findViewById(R.id.processContainer)
        emptyState = findViewById(R.id.emptyProcessState)

        findViewById<MaterialButton>(R.id.btnClearProcesses).setOnClickListener {
            sendBroadcast(android.content.Intent(ScreenCaptureService.ACTION_CLEAR_PROCESSES).setPackage(packageName))
            ProcessMonitor.cancelActive(getString(R.string.debug_cancelled_manually))
            renderProcesses()
            Toast.makeText(this, R.string.debug_processes_cleared, Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnClearCache).setOnClickListener { clearApplicationCache() }
    }

    override fun onStart() {
        super.onStart()
        handler.post(refreshProcesses)
    }

    override fun onStop() {
        handler.removeCallbacks(refreshProcesses)
        super.onStop()
    }

    override fun onDestroy() {
        cacheExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun renderProcesses() {
        val processes = ProcessMonitor.snapshot()
        processContainer.removeAllViews()
        emptyState.visibility = if (processes.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        processes.forEach { process ->
            val elapsed = ((SystemClock.elapsedRealtime() - process.startedAt) / 1000f).coerceAtLeast(0f)
            val row = layoutInflater.inflate(R.layout.item_debug_process, processContainer, false)
            row.findViewById<TextView>(R.id.processTitle).text = getString(R.string.debug_process_title, process.category, process.state)
            row.findViewById<TextView>(R.id.processDetail).text = process.detail
            row.findViewById<TextView>(R.id.processDuration).text = getString(R.string.debug_process_duration, elapsed)
            processContainer.addView(row)
        }
    }

    private fun clearApplicationCache() {
        val button = findViewById<MaterialButton>(R.id.btnClearCache)
        button.isEnabled = false
        cacheExecutor.execute {
            val freedBytes = listOf(cacheDir, codeCacheDir).distinct().sumOf(::deleteContents)
            runOnUiThread {
                button.isEnabled = true
                Toast.makeText(this, getString(R.string.debug_cache_cleared, formatBytes(freedBytes)), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteContents(directory: File): Long {
        var freed = 0L
        directory.listFiles()?.forEach { file ->
            val size = if (file.isDirectory) deleteContents(file) else file.length()
            if (file.delete()) freed += size
        }
        return freed
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024f * 1024f))
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
        else -> "$bytes B"
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 500L
    }
}
