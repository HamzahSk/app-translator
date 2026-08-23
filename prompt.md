**TUGAS FASE 17: AUTO FONT SIZING & TIGHT BUBBLE WRAP**

# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase17_auto_bubble.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR
Silakan kembangkan fitur baru untuk penyesuaian ukuran teks dan kanvas secara dinamis:

## 1. Pengaturan Konfigurasi (Settings)
 * Di `ConfigManager.kt`, tambahkan properti boolean baru (misal: `autoTextFitEnabled`).
 * Di `SettingsDialog.kt`, tambahkan elemen UI *toggle/switch* agar user bisa menyalakan atau mematikan fitur **Auto Text Fit & Wrap** ini. Ikatkan (bind) state-nya dengan properti baru di `ConfigManager` tadi.

## 2. Fitur Auto Font Sizing
 * Buka `OverlayManager.kt`. Pada bagian logika yang merender teks (menggunakan `StaticLayout`), periksa apakah fitur `autoTextFitEnabled` sedang AKTIF.
 * Jika AKTIF: Abaikan ukuran font dari pengaturan slider (`overlayTextSize`). Lakukan kalkulasi dinamis (iterasi atau binary search) untuk mencari ukuran font terbesar yang teksnya masih muat sepenuhnya di dalam *bounding box* OCR asli tanpa terpotong.

## 3. Fitur Tight Bubble Wrap (Background Menyesuaikan Teks)
 * Masih di `OverlayManager.kt`, jika fitur `autoTextFitEnabled` AKTIF, modifikasi ukuran *background* (dan *border* jika aktif).
 * Alih-alih mengikuti ukuran kotak OCR asli yang mungkin kebesaran, buat kotak kanvas/background menjadi persegi panjang yang "memeluk" rapat dimensi teks aktual yang sudah ter-render (*wrap-content*). 
 * Tambahkan *padding* yang wajar agar teks tidak menabrak *border*.

# Verifikasi Akhir
 * Jalankan `./gradlew spotlessApply`.
 * Verifikasi sukses dengan `./gradlew assembleDebug`.
