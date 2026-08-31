PROMPT UNTUK AI AGENT: FIX BUGS PHASE 27 - DEBUG & MONITORING

---

KONTEKS

Kita telah mengimplementasikan Fase 27 (Debug & Monitoring) tetapi ditemukan 3 bug kritis:

1. Clear Processes tidak benar-benar menghentikan proses background (hanya mengubah status di UI)
2. Broadcast ACTION_CLEAR_PROCESSES tidak sampai ke ScreenCaptureService
3. Clear Cache hanya membersihkan sebagian cache (ML Kit models & overlay cache tidak terhapus)

---

TUGAS: PERBAIKI SEMUA BUG DI ATAS

1. PERBAIKI KOMUNIKASI DEBUG ACTIVITY → SCREEN CAPTURE SERVICE

File: app/src/main/java/com/rocat/translator/DebugActivity.kt

Masalah: Broadcast tidak sampai karena RECEIVER_NOT_EXPORTED

Solusi: Gunakan startService() dengan Intent explicit, bukan broadcast.

Yang harus diubah:

```kotlin
// HAPUS ini:
sendBroadcast(android.content.Intent(ScreenCaptureService.ACTION_CLEAR_PROCESSES).setPackage(packageName))

// GANTI dengan:
val intent = Intent(this, ScreenCaptureService::class.java).apply {
    action = ScreenCaptureService.ACTION_CLEAR_PROCESSES
}
startService(intent)
```

---

2. PERBAIKI CLEAR PROCESSES DI SCREEN CAPTURE SERVICE

File: app/src/main/java/com/rocat/translator/ScreenCaptureService.kt

Masalah: clearAllProcesses() tidak membatalkan captureJob dengan benar dan tidak membersihkan ProcessMonitor.

Solusi: Perbaiki method clearAllProcesses():

```kotlin
private fun clearAllProcesses() {
    // 1. Batalkan capture coroutine
    captureJob?.cancel()
    captureJob = null
    
    // 2. Batalkan semua proses di TranslationEngine
    translationEngine.cancelAllProcesses()
    
    // 3. Bersihkan ProcessMonitor (tambahkan ini)
    ProcessMonitor.cancelActive("Cancelled from Debug screen")
    
    // 4. Paksa garbage collection untuk membersihkan memory
    System.gc()
}
```

Tambahkan juga di onStartCommand() agar menerima ACTION_CLEAR_PROCESSES:

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_CLEAR_PROCESSES) {
        clearAllProcesses()
        return START_NOT_STICKY
    }
    // ... kode existing lainnya
}
```

---

3. PERBAIKI CLEAR CACHE DI DEBUG ACTIVITY

File: app/src/main/java/com/rocat/translator/DebugActivity.kt

Masalah: Hanya membersihkan cacheDir dan codeCacheDir, tidak membersihkan model ML Kit & cache overlay.

Solusi: Tambahkan pembersihan model ML Kit dan cache tambahan:

```kotlin
private fun clearApplicationCache() {
    val button = findViewById<MaterialButton>(R.id.btnClearCache)
    button.isEnabled = false
    
    cacheExecutor.execute {
        var totalFreed = 0L
        
        // 1. Bersihkan cache internal
        totalFreed += listOf(cacheDir, codeCacheDir).distinct().sumOf(::deleteContents)
        
        // 2. Bersihkan ML Kit models (tambahkan ini)
        try {
            val modelManager = com.google.mlkit.common.model.RemoteModelManager.getInstance()
            // Hapus semua model OCR yang terdownload
            val ocrModels = listOf(
                com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions.Builder().build().getModel()
                // Tambahkan model lain jika perlu
            )
            // Atau gunakan cara yang lebih sederhana: hapus folder model
            val modelDir = File(filesDir, "mlkit_models")
            if (modelDir.exists()) {
                totalFreed += deleteContents(modelDir)
            }
        } catch (e: Exception) {
            Log.e("DebugActivity", "Failed to clear ML Kit cache", e)
        }
        
        // 3. Hapus overlay cache jika ada
        try {
            val overlayDir = File(cacheDir, "overlay_cache")
            if (overlayDir.exists()) {
                totalFreed += deleteContents(overlayDir)
            }
        } catch (e: Exception) {
            Log.e("DebugActivity", "Failed to clear overlay cache", e)
        }
        
        // 4. Paksa garbage collection
        runOnUiThread {
            System.gc()
            button.isEnabled = true
            Toast.makeText(
                this, 
                getString(R.string.debug_cache_cleared, formatBytes(totalFreed)), 
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
```

---

4. TAMBAHKAN VERIFIKASI DI DEBUG ACTIVITY

File: app/src/main/java/com/rocat/translator/DebugActivity.kt

Masalah: Tidak ada verifikasi apakah proses benar-benar berhenti.

Solusi: Tambahkan pengecekan setelah Clear Processes:

```kotlin
findViewById<MaterialButton>(R.id.btnClearProcesses).setOnClickListener {
    // Kirim intent ke service
    val intent = Intent(this, ScreenCaptureService::class.java).apply {
        action = ScreenCaptureService.ACTION_CLEAR_PROCESSES
    }
    startService(intent)
    
    // Verifikasi: cek apakah TranslationEngine benar-benar berhenti
    handler.postDelayed({
        // Ambil TranslationEngine dari service (perlu method getter)
        // Atau cek ProcessMonitor apakah semua proses sudah inactive
        val remaining = ProcessMonitor.snapshot().filter { it.active }
        if (remaining.isEmpty()) {
            Toast.makeText(this, "All processes cleared successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Warning: ${remaining.size} processes still active", Toast.LENGTH_SHORT).show()
        }
    }, 500) // Delay 500ms untuk memberi waktu service merespon
    
    renderProcesses()
}
```

---

5. TAMBAHKAN METHOD GETTER DI SCREEN CAPTURE SERVICE

File: app/src/main/java/com/rocat/translator/ScreenCaptureService.kt

Masalah: DebugActivity tidak bisa mengakses TranslationEngine untuk verifikasi.

Solusi: Tambahkan static method atau gunakan ProcessMonitor sebagai indikator.

Alternatif: Gunakan ProcessMonitor sebagai single source of truth.

---

6. UPDATE LOG FASE 27

File: ai_memory/task_20260830_2338_phase27_debug_monitor.md

Tambahkan informasi bug fixes di bagian "Ringkasan Perubahan":

```markdown
## Bug Fixes (Phase 27.1)
- Fixed Clear Processes: now properly cancels captureJob and all TranslationEngine processes
- Fixed communication: changed from broadcast to startService() for reliable delivery
- Fixed Clear Cache: now clears ML Kit models and overlay cache in addition to app cache
- Added verification after Clear Processes to confirm all jobs are stopped
```

---

KRITERIA VERIFIKASI

Setelah perbaikan, pastikan:

1. ✅ Tombol "Clear Processes" benar-benar menghentikan semua proses background (cek dengan ProcessMonitor.snapshot())
2. ✅ Tidak ada proses yang tersisa setelah Clear Processes (status harus COMPLETED atau CANCELLED, bukan RUNNING)
3. ✅ Clear Cache menghapus semua cache termasuk model ML Kit (cek ukuran cache sebelum & sesudah)
4. ✅ Aplikasi tidak crash setelah Clear Processes atau Clear Cache
5. ✅ Tidak ada memory leak (gunakan Android Profiler untuk verifikasi)

---

FORMAT OUTPUT YANG DIHARAPKAN

1. Berikan diff patch untuk setiap file yang diubah
2. Tulis penjelasan singkat untuk setiap perubahan
3. Update log fase 27 dengan informasi bug fixes
4. Update ai_memory/00_INDEX.md dengan entry baru untuk bug fixes

---

CATATAN TAMBAHAN

· Jangan ubah struktur ProcessMonitor yang sudah ada
· Pastikan semua perubahan kompatibel dengan Android API 24+
· Gunakan runOnUiThread untuk semua operasi UI
· Tambahkan error handling untuk semua operasi I/O

---

DEADLINE: Selesaikan semua perbaikan dalam 1 sesi eksekusi AI Agent.