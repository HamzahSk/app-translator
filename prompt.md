
**TUGAS FASE 14: APP LANGUAGE, CUSTOM OCR MARGIN, FIREBASE, & BUILD CONFIG**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase14_app_config_and_merging.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase 14 ini, kita akan menambahkan opsi pengaturan bahasa UI, menyesuaikan logika penggabungan blok teks (block merging), menambahkan dukungan Firebase (opsional), mengatur Keystore opsional untuk release build, dan mengubah package name aplikasi.
### 1. Pengaturan Bahasa Aplikasi (App Language Setting)
 * **Fitur Baru:** Tambahkan pengaturan "App Language" di Settings Popup.
 * **Pilihan Bahasa:** Sediakan pilihan "System Default" (sebagai default), "English", dan "Indonesian".
 * **Integrasi:** Simpan preferensi di ConfigManager. Perbarui I18nManager agar memuat file XML yang sesuai (en/strings.xml atau id/strings.xml). Jika "System Default" dipilih, fallback ke bahasa sistem perangkat. Terapkan perubahan UI segera atau berikan notifikasi *restart* aplikasi.
 
### 2. Kustomisasi Jarak Paragraf (Custom Block Merging)
 * **Masalah:** Teks yang seharusnya 1 kelompok sering terpisah karena algoritma mergeBlocks() terlalu ketat.
 * **Solusi:** Tambahkan slider "Paragraph Grouping / Margin" di Settings Popup. Simpan nilainya (misal pengali 1.0 - 3.0) di ConfigManager dan terapkan pada fungsi mergeBlocks() di TranslationEngine.kt.
### 3. Integrasi Opsional Firebase (Telemetry)
 * **Fitur:** Tambahkan dependensi Firebase Analytics dan Crashlytics di build.gradle.kts.
 * **Kondisi Opsional:** Pastikan inisialisasi Firebase dibungkus dalam blok try-catch atau pengecekan kondisional. Aplikasi **TIDAK BOLEH** gagal *build* atau *crash* jika google-services.json atau environment variables tidak tersedia.
### 4. Konfigurasi Keystore Opsional
 * **Konfigurasi Release:** Perbarui build.gradle.kts untuk membaca variabel lingkungan berikut jika tersedia:
   * SIGNING_KEY (base64 untuk storeFile)
   * KEY_STORE_PASSWORD
   * ALIAS
   * KEY_PASSWORD
 * **Fallback:** Jika variabel tidak ada/kosong, pastikan *build release* tetap berhasil (misalnya fallback ke konfigurasi debug). Decode SIGNING_KEY base64 menjadi file .jks sementara saat proses *build*.
### 5. Perubahan Package Name
 * **Tindakan:** Ubah applicationId dan namespace di app/build.gradle.kts menjadi com.rocat.translator.
 * Perbarui struktur folder dari com/ervareza/screentranslator menjadi com/rocat/translator, beserta seluruh import terkait di dalam proyek.
### 6. Verifikasi Akhir
 * Jalankan ./gradlew spotlessApply.
 * Verifikasi sukses dengan ./gradlew assembleDebug.
