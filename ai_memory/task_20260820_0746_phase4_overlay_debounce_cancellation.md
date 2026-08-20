# Task Log: Phase 4 Overlay, Debounce, Cancellation

## Status
Selesai; build debug berhasil.

## Ringkasan Perubahan
- Default `inactivityDelayMs` di `ConfigManager` diubah dari 3 detik menjadi 1,5 detik.
- Accessibility service kini merespons scroll, pergantian window, dan awal interaksi touch. Setiap aktivitas langsung mengirim perintah clear/cancel lalu menjadwalkan ulang capture setelah periode inactivity.
- `TranslationEngine` melacak satu `activeJob`; capture baru membatalkan proses lama, sedangkan perintah clear membatalkan OCR/network aktif dan membersihkan seluruh overlay/loading bubble.
- Jalur OCR, identifikasi bahasa, offline translation, dan online batch meneruskan `CancellationException` serta memakai `ensureActive()` di titik penting.
- Tinggi status bar dibaca dari resource sistem. Blok OCR yang seluruhnya berada di status bar dibuang sebelum translasi/API, sedangkan bounding box lainnya dikoreksi dengan mengurangi offset Y status bar sebelum overlay digambar.

## Verifikasi
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.

## Next Steps
- Uji pada perangkat dengan status bar/cutout berbeda dan pastikan gesture touch menghasilkan accessibility event pada aplikasi target.
