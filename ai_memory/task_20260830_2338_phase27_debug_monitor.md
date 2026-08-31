# Task Log: Phase 27 Debug & Process Monitoring

## Status
Selesai.

## Ringkasan Perubahan
- Menambahkan tombol Debug di toolbar utama, berdampingan dengan Settings, dan `DebugActivity` berbasis Material 3.
- Menambahkan `ProcessMonitor` thread-safe serta daftar real-time untuk aktivitas capture, OCR, identifikasi bahasa, dan translator online/offline.
- Menambahkan aksi Clear Processes yang membatalkan capture dan pipeline `TranslationEngine` aktif secara graceful, menutup recognizer, serta membersihkan overlay.
- Memperbaiki serialisasi capture dengan menyimpan coroutine ke `captureJob`, sehingga capture baru membatalkan capture lama.
- Menambahkan pembersihan isi cache dan code cache di background thread dengan laporan ukuran ruang yang dibebaskan.
- Manifest, layout, dan string resource diperbarui untuk layar baru.

## Verifikasi
- `./gradlew spotlessApply compileDebugKotlin`: sukses.
- Resource linking dan kompilasi Kotlin: sukses.
- `assembleDebug`: mencapai packaging APK, lalu gagal karena heap Gradle environment habis (`OutOfMemoryError: Java heap space`), bukan error source/resource.
- `git diff --check -- app/src ai_memory`: sukses sebelum penambahan log ini.

## Tugas Selanjutnya (Next Steps)
Uji tombol Clear Processes dan ukuran cache pada perangkat sambil service capture aktif; jalankan `assembleDebug` dengan heap Gradle lebih besar.
