# Task Log: Phase 6 Refinement UI/UX, Start Button, Offline Hang & Text Spacing

## Status
Selesai; `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL.

## Ringkasan Perubahan
- `TranslationEngine.kt`: Tambah helper `mlKitCall(tag, block)` yang membungkus semua panggilan ML Kit `.await()` (OCR `recognizer.process`, `identifyLanguage`, dan `translator.translate`) dengan `withTimeout(7000L)`. `TimeoutCancellationException` ditangkap di dalam helper dan mengembalikan `null`, sehingga coroutine tidak menggantung; cleanup `removeLoading` tetap jalan via `finally`. `preloadOfflineModel()` kini memuat model ke RAM lewat dummy call OCR (bitmap 1x1) dan `translate("test")` di awal service.
- `MainActivity.kt`: Alur tombol Start di-refactor. Semua pengecekan berat (`Settings.canDrawOverlays`, `isAccessibilityServiceEnabled`, `isServiceRunning`, `createScreenCaptureIntent`) dipindah ke `Dispatchers.IO` via `StartCheckResult`; UI (spinner, snackbar) diperbarui di Main thread; debounce 500 ms dan state "Preparing..." dipertahankan; state tombol direstore via `refreshPermissionStatuses()` pada jalur gagal.
- `OverlayManager.kt`: `StaticLayout.Builder` memakai `.setLineSpacing(4f, 1.2f)` (konstanta adjustable `LINE_SPACING_ADD`/`LINE_SPACING_MULTIPLIER`). Jika `layout.height > rect.height`, box dipuaskan simetris ke atas/bawah (`r.top -= overflow; r.bottom += overflow`) sehingga teks tetap sentris terhadap bounding box asli.

## Tugas Selanjutnya (Next Steps)
- Uji manual di perangkat: hang saat offline (stop/start service), respon tombol Start, dan tampilan bubble multi-baris.
- Pertimbangkan UI untuk meng-adjust line spacing bila diperlukan.