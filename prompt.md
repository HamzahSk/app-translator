**TUGAS FASE 14: LANGUAGE SETTINGS, CUSTOM BLOCK MERGING, & BUILD CONFIGURATION**

# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_phase14_app_config_and_merging.md`.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.

# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase 14 ini, kita akan menambahkan opsi pengaturan bahasa UI, menyesuaikan logika penggabungan blok teks (block merging) untuk OCR, menambahkan dukungan Firebase secara kondisional (opsional), mengatur Keystore untuk release build, dan mengubah package name aplikasi. Kerjakan dengan saksama:

## 1. Pengaturan Bahasa Aplikasi (App Language Setting)
 * **Fitur Baru:** Tambahkan pengaturan "App Language" di Settings Popup (misalnya menggunakan Dropdown/Spinner atau MaterialButtonToggleGroup) untuk memilih bahasa UI aplikasi.
 * **Pilihan Bahasa:** Sediakan pilihan "System Default", "English", dan "Indonesian".
 * **Integrasi:** Simpan preferensi ini di `ConfigManager`. Perbarui `I18nManager` agar ketika pengguna memilih bahasa tertentu, aplikasi langsung menggunakan file XML yang sesuai (misal: jika pengguna memilih "English", `I18nManager` memuat `en/strings.xml`, mengabaikan bahasa sistem Android; jika "System Default", gunakan bahasa sistem).
 * Pastikan UI langsung memperbarui string-nya saat bahasa diganti, atau berikan instruksi kepada pengguna untuk me-restart aplikasi.

## 2. Kustomisasi Jarak Paragraf (Custom Block Merging)
 * **Masalah:** Saat ini, teks yang seharusnya tergabung dalam satu gelembung (bubble) sering terpisah menjadi beberapa blok terjemahan karena batas toleransi jarak (vertical/horizontal gap) pada algoritma `mergeBlocks()` terlalu ketat.
 * **Solusi:** 
   * Tambahkan slider baru di Settings Popup bernama "Paragraph Grouping / Margin" (atau nama yang sesuai).
   * Nilai slider ini (misal: skala 1.0 hingga 3.0, yang merepresentasikan pengali/multiplier jarak antar teks) harus disimpan di `ConfigManager`.
   * Terapkan nilai pengali ini pada fungsi `mergeBlocks()` di `TranslationEngine.kt` (atau di mana pun logika *block merging* berada). Ganti nilai *hardcoded* (seperti `1.5 * lineHeight`) dengan nilai yang diatur pengguna, sehingga pengguna bisa menyesuaikan seberapa jauh teks yang berdekatan dianggap sebagai satu paragraf.

## 3. Opsional Telemetry (Firebase Analytics/Crashlytics)
 * **Fitur Baru:** Tambahkan integrasi Firebase Analytics dan Crashlytics pada `app/build.gradle.kts`.
 * **Penting (Opsional):** Pembungkus kode atau inisialisasi Firebase (di `MainActivity` atau `Application` class) harus dibungkus dalam blok `try-catch` atau pengecekan kondisional. Aplikasi TIDAK BOLEH *crash* jika file `google-services.json` tidak ada atau jika environment variables Firebase tidak dikonfigurasi saat proses build.

## 4. Konfigurasi Keystore & Build Release
 * **Konfigurasi:** Perbarui `app/build.gradle.kts` untuk mendukung *release signing config* menggunakan *Environment Variables* (berasal dari GitHub Secrets).
 * **Parameter:** Gunakan variabel lingkungan berikut:
   * `STORE_FILE` (yang diambil dari decode `SIGNING_KEY` base64)
   * `STORE_PASSWORD`
   * `KEY_ALIAS`
   * `KEY_PASSWORD`
 * Pastikan build *release* berhasil dijalankan tanpa *hardcoded* password di file gradle.

## 5. Perubahan Package Name
 * **Perubahan:** Ubah *package name* aplikasi (applicationId) dari yang sekarang menjadi `com.rocat.translator`.
 * **Tindakan:** Perbarui nilai `applicationId` di `app/build.gradle.kts` dan ganti nama direktori/package di seluruh file sumber daya dan Kotlin (`AndroidManifest.xml`, struktur folder `src/main/java/com/...`, import statement, dll).
 * Pastikan namespace di gradle juga diperbarui.

## 6. Verifikasi & Build
 * Jalankan task pemformatan `./gradlew spotlessApply`.
 * Pastikan proyek berhasil dikompilasi (run `./gradlew assembleDebug`).
 * Laporkan hasil dan buat log memory.
