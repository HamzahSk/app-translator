# AI Memory Master Index — Screen Translator

## Status Proyek Terkini
Aplikasi **Screen Translator** (Android, Kotlin, AGP 8.2.1, targetSdk 34, minSdk 26).
Fitur inti sudah lengkap: Splash berbasis VectorDrawable, lazy loading ML Kit via Coroutines, Screen Capture Service, Accessibility trigger, Overlay translation bubble, OCR multi-bahasa (ja/ko/zh/hi/en), mode terjemahan offline (ML Kit) dan online (OpenAI/Gemini via Retrofit), serta Split APK per ABI.
**Build debug SUCCESS; release R8 terhenti karena heap daemon 512 MiB.** Runtime cache online/ML Kit, overlay thread handling, batch parsing, dan global crash reporting sudah di-hardening. Seluruh Material Slider kini continuous. Fase 4 selesai: offset status bar/filter OCR, debounce default 1,5 detik, dan cancellation job saat aktivitas layar sudah diterapkan.

## Riwayat (terbaru di atas)

| Tanggal | File Log | Ringkasan |
|---------|----------|-----------|
| 2026-08-20 08:15 | [task_20260820_0815_phase5_batch_overlay_font.md](task_20260820_0815_phase5_batch_overlay_font.md) | Batch fullscreen overlay, clear aman saat scroll/screenshot, dan custom font Comic dipasang. Compile debug sukses. |
| 2026-08-20 09:00 | [task_20260820_0900_phase55_overlay_preload_start.md](task_20260820_0900_phase55_overlay_preload_start.md) | Fix touch interception, StaticLayout wrapping, offline loading bubble, preload model, debounce/state tombol Start. Compile debug sukses. |
| 2026-08-20 07:46 | [task_20260820_0746_phase4_overlay_debounce_cancellation.md](task_20260820_0746_phase4_overlay_debounce_cancellation.md) | Koreksi/filter status bar, debounce 1,5 detik, dan pembatalan OCR/network aktif saat scroll/touch; build debug sukses. |
| 2026-08-20 05:53 | [task_20260820_0553_phase3_material_slider_fix.md](task_20260820_0553_phase3_material_slider_fix.md) | Menghapus stepSize dari seluruh Material Slider dan memvalidasi konversi nilai listener; build debug sukses. |
| 2026-08-20 05:28 | [task_20260820_0528_phase25_global_crash_handler.md](task_20260820_0528_phase25_global_crash_handler.md) | Perbaikan thread overlay dan parsing batch, GlobalExceptionHandler, CrashActivity, serta log crash lokal. |
| 2026-08-20 02:27 | [task_20260820_0227_build_release_fix.md](task_20260820_0227_build_release_fix.md) | Fix ProGuard/R8 untuk mode online (Gson/Retrofit keep rules), verifikasi build debug+release sukses, verifikasi 5 Split APK ABI. |
| 2026-08-20 03:15 | [task_20260820_0315_runtime_cache_hardening.md](task_20260820_0315_runtime_cache_hardening.md) | Hardening lifecycle splash, refresh client API saat konfigurasi berubah, dan cache ML Kit thread-safe; build debug+release sukses. |
| 2026-08-20 04:33 | [task_20260820_0433_phase2_translation_ui_spotless.md](task_20260820_0433_phase2_translation_ui_spotless.md) | Spotless ktlint, satu request translasi online per layar, dan loading bubble overlay sebelum hasil tersedia; compile berhasil. |

## Catatan
- Baca maks. 2 log terbaru bila butuh detail; jangan baca seluruh folder sekaligus.
- Jika jumlah log > 10, arsipkan ke `archive_phase_N.md` lalu hapus log individu.
