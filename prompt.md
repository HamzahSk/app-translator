
**TUGAS FASE 16: BUG FIXES & OCR BLOCK MERGING REFINEMENT**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase16_bug_fixes.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Silakan lakukan analisis visual pada gambar Screenshot_2026-08-23-09-34-45-05_8868b52a0a3daaaefef85acdbe777777.jpg dan Screenshot_2026-08-23-09-34-30-63_8868b52a0a3daaaefef85acdbe777777.jpg yang ada di *root directory*. Berdasarkan temuan pada gambar tersebut, lakukan perbaikan berikut:
## 1. Perbaikan Akurasi Deteksi Teks (Block Merging)
 * **Analisis Masalah:** Pada tangkapan layar, terlihat ada teks di dalam satu balon dialog yang terpecah menjadi banyak blok kecil-kecil, padahal seharusnya digabung menjadi 1 kelompok teks. Kasus ini terjadi di beberapa kondisi spesifik.
 * **Tindakan (Backend):** Evaluasi dan perbarui algoritma mergeBlocks() di TranslationEngine.kt. Perbaiki kalkulasi toleransi jarak spasial (*vertical/horizontal gap*), ukuran teks, dan logika *overlap* agar teks yang berada di dalam satu balon dialog tidak keliru dibaca sebagai elemen yang terpisah.
## 2. Perbaikan UI & Presisi Canvas (Overlay)
 * **Presisi Canvas Loading:** Posisi *canvas loading* indikator terlihat masih kurang presisi dan tidak menutupi teks asli dengan tepat. Perbaiki kalkulasi koordinat dan *bounding box* pada saat melakukan proses *render* *placeholder loading*.
 * **Bug Bubble Border & Settings:** Fitur *bubble border* sudah diaktifkan di sisi UI/konfigurasi namun secara visual tidak terlihat (*tidak ter-render*). Periksa dan perbaiki kode yang menggambar *border* tersebut di OverlayManager.kt. Lakukan juga pengecekan pada logika *binding* di SettingsDialog.kt untuk memastikan tidak ada *bug* serupa pada pengaturan *styling* lainnya.
# Verifikasi Akhir
 * Jalankan ./gradlew spotlessApply.
 * Verifikasi sukses dengan ./gradlew assembleDebug.
