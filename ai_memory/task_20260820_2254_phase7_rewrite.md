# Task Log: Phase 7 Rewrite Start Button & ML Kit Re-Optimization

## Status
Selesai; `./gradlew :app:compileDebugKotlin` dan `spotlessCheck` BUILD SUCCESSFUL.

## Ringkasan Perubahan
- `MainActivity.kt`: Logika tombol Start ditulis ulang total dengan state machine `StartState { IDLE, PREPARING, RUNNING, BLOCKED }`. Listener tombol terdaftar tepat satu kali; routing berjalan via state (perbaikan bug klik dua kali yang disebabkan `onResume` menimpa listener). `createScreenCaptureIntent()` dipanggil instan di UI thread; evaluasi permission berjalan via coroutine IO. Cek `getRunningServices` dipindah ke IO via `syncServiceState()`. Tidak ada runBlocking/Thread.sleep. `refreshPermissionStatuses()` tidak lagi menimpa state saat PREPARING/RUNNING.
- `TranslationEngine.kt`: Pre-load ML Kit dipindah ke `preloadScope` independen `Dispatchers.Default` (CPU-bound). `getRecognizer`/`getTranslator` dibuat lock-free: instansiasi client terjadi di luar `synchronized`, sehingga dummy call preload tidak pernah mengunci/memblokir capture sungguhan. Ditambah `showScanningIndicator()` yang memaksa bubble dirender (withContext(Main) + delay(50)) sebelum OCR berat berjalan, dan dibersihkan di `finally`. `close()` membatalkan kedua scope.
- `OverlayManager.kt`: Hanya formatting spotless (line wrap); fitur Phase 6 (Line Spacing & Center Expansion Box) tetap utuh.

## Verifikasi
- `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL (hanya warning deprecated `scaledDensity`).
- `./gradlew spotlessCheck` BUILD SUCCESSFUL (memperbaiki 2 baris >140 karakter yang sudah ada).

## Next Steps
- Uji manual di perangkat: klik satu kali tombol Start, kecepatan start/stop, dan munculnya loading bubble saat capture pertama.