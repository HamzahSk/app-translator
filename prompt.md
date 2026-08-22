**TUGAS FASE 13.6: BUG FIX I18N, TEMA POPUP, & SPOTLESS FORMATTING**

# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_phase13_6_bug_fixes.md`.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.

# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase 13.6 ini, kita akan memperbaiki bug koneksi file string, isu warna background Settings Popup, dan memastikan kode memenuhi standar format dengan Spotless. Kerjakan poin-poin berikut:

## 1. Perbaikan Bug File String (i18n)
 * **Masalah:** File string XML sudah dibuat di root directory, tetapi string di aplikasi masih belum terhubung/tampil dengan benar (kemungkinan blank atau fallback ke key-nya). Hal ini sering terjadi karena *custom task* Gradle (`copyI18nAssets`) tidak terkait (depend) secara tepat ke siklus build (seperti `generateDebugAssets` atau `preBuild`), sehingga file XML tidak benar-benar tersalin ke `assets/` di dalam APK.
 * **Solusi:**
   * Perbaiki konfigurasi di `app/build.gradle.kts`. Pastikan task yang menyalin file dari `<root>/i18n/` ke `app/src/main/assets/i18n/` dikaitkan dengan benar di pipeline build Android, misalnya dengan `tasks.named("generateDebugAssets") { dependsOn("copyI18nAssets") }` atau dikaitkan ke `preBuild`.
   * Pastikan fallback mechanism di `I18nManager.kt` berfungsi sempurna. Jika file bahasa tertentu (misal `id`) tidak ditemukan, sistem harus me-load `en` sebagai fallback.
   * Pastikan file `I18nManager.kt` melakukan inisialisasi XML parser dengan path file yang benar dan bisa membaca nilai node XML tanpa throw Exception.

## 2. Perbaikan Warna Tema Settings Popup
 * **Masalah:** Warna background popup Settings masih belum menyesuaikan tema (Dark/Light mode). Atribut `?attr/colorSurface` pada file `bg_settings_dialog.xml` kadang tidak berfungsi di API atau versi library tertentu saat digunakan sebagai dialog custom.
 * **Solusi:**
   * Terapkan pendekatan programatis untuk background dialog (mengambil nilai dari atribut `android.R.attr.colorBackground` atau `R.attr.colorSurface` dari context), atau terapkan style dialog Material bawaan.
   * Pastikan teks, slider, dan tombol di dalam `SettingsDialog` bisa terbaca jelas di Dark Mode (tidak putih di atas putih atau abu-abu di atas abu-abu).

## 3. Pengecekan Kerapian Kode (Spotless)
 * **Masalah:** Diperlukan langkah untuk memastikan seluruh kode sesuai standard style Kotlin.
 * **Solusi:**
   * Jalankan task spotless setelah perubahan: `./gradlew spotlessApply`.
   * Periksa dan perbaiki error formatting jika ada.

## 4. Verifikasi & Build
 * Pastikan pengaturan i18n memuat string asli, bukan key text.
 * Pastikan UI popup otomatis beralih jika mode sistem diubah ke gelap/terang (jika memungkikan di-test atau disimulasikan).
 * Verifikasi sukses dengan menjalankan `./gradlew assembleDebug`.