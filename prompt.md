**TUGAS FASE 15: UI REVAMP (GREEN & BLUE THEME) & MISSING FEATURES IMPLEMENTATION**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase15_ui_revamp_and_features.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase 15 ini, kita fokus pada peningkatan UI yang signifikan, penambahan fitur yang terlewat (App Language, Custom OCR Margin), dan penyesuaian Floating Ball sesuai tema.
## 1. UI Revamp & Theme (Warna Hijau & Biru)
 * **Analisis UI Saat Ini:** Lihat image_0.png dan image_1.png. UI terlihat terlalu datar (flat) dan ungu.
 * **Perubahan Tema Global:** Terapkan skema warna baru: Hijau Teal/Emerald dan Biru Azure/Cyan.
   * Ganti warna aksen ungu (pada tombol 'System', tombol 'Start Service', slider, switch) menjadi perpaduan Hijau dan Biru.
   * Ganti warna background teks (misal teks 'Screen Translator' dan header section) yang sebelumnya ungu menjadi nuansa Hijau/Biru.
 * **Peningkatan Visual (Non-Flat):**
   * Tambahkan *depth* dengan menggunakan gradien halus (subtle gradient) pada tombol utama (seperti 'Start Service').
   * Gunakan *inner shadow* atau efek *relief* yang tipis pada kartu/kontainer section untuk membedakannya dari latar belakang.
   * Pastikan *border* pada kontainer tetap ada namun lebih halus.
 * **Terapkan ke Semua Halaman:** Perubahan ini harus mencakup Main Screen (image_0.png) dan Overlay Customization Popup (image_1.png).
 
## 2. Implementasi Fitur yang Terlewat (Missing Features)
 * **A. Pengaturan Bahasa Aplikasi (App Language Setting):**
   * **Detail:** Ganti *placeholder* "Language settings not implemented" atau tambahkan kontrol baru di Settings Popup.
   * **UI:** Tambahkan Menu Dropdown (seperti Source Language) dengan pilihan: "System Default", "English", "Indonesian".
   * **Backend:** Simpan preferensi di ConfigManager. Integrasikan dengan I18nManager agar segera memuat file XML yang sesuai (en/strings.xml atau id/strings.xml). Jika "System Default" dipilih, fallback ke bahasa sistem perangkat. Terapkan perubahan UI segera atau berikan notifikasi *restart* aplikasi.
 * **B. Kustomisasi Jarak Kelompok Teks (Custom Block Merging):**
   * **Detail:** Perbaiki masalah teks yang terpisah di algoritma mergeBlocks().
   * **UI:** Tambahkan Slider baru di Overlay Customization Popup (di bawah 'Text Size' atau di section baru) bernama "Paragraph Grouping / Margin".
   * **Backend:** Simpan nilainya (misal pengali 1.0 - 3.0) di ConfigManager dan terapkan pada fungsi mergeBlocks() di TranslationEngine.kt.
## 3. Penyesuaian Floating Ball
 * **Tindakan:** Sesuaikan ikon Floating Ball yang berada di pojok kanan bawah (di image_0.png) dengan tema baru.
 * **Desain:** Ganti skema warna ungu/pink pada ikon Play/Start tersebut menjadi perpaduan warna aksen Hijau dan Biru yang baru.
# Verifikasi Akhir
 * Jalankan ./gradlew spotlessApply.
 * Verifikasi sukses dengan ./gradlew assembleDebug.
