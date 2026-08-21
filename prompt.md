
**TUGAS FASE 8: SMART OCR BLOCK MERGING (SIZE & PROXIMITY AWARE) & ANTI-OVERLAP**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase8_block_merging.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
# INSTRUKSI PERBAIKAN BUG KODE
Kamu bertugas menyelesaikan masalah fragmentasi OCR di TranslationEngine.kt. Saat ini, ML Kit memecah satu gelembung percakapan menjadi banyak TextBlock kecil. Kita butuh algoritma penggabungan yang pintar agar teks dialog tidak bercampur dengan SFX atau teks pikiran yang ukurannya berbeda.
## 1. Implementasi Algoritma Smart Block Merging (Size & Proximity Aware)
 * **Masalah:** Iterasi langsung pada visionText.textBlocks membuat teks yang berdekatan digambar terpisah dan bertumpuk. Jika hanya digabung berdasarkan jarak, SFX dan Dialog yang berbeda ukuran akan ikut menyatu.
 * **Solusi:**
   * Buat struktur data baru: MergedBlock(val text: String, val boundingBox: Rect).
   * Buat fungsi mergeBlocks(blocks: List<Text.TextBlock>): List<MergedBlock> di TranslationEngine.
   * **Logika Penggabungan (Heuristic):**
     1. Hitung perkiraan tinggi teks per baris (*approximate line height*) untuk setiap blok: block.boundingBox.height() / max(1, block.lines.size).
     2. Urutkan blok berdasarkan koordinat Y (atas ke bawah).
     3. Dua blok **HANYA** boleh digabungkan jika memenuhi 3 syarat ini:
       * **Jarak Vertikal Dekat:** jarak Y < (1.5 * rata-rata line height).
       * **Bukan Kolom Berbeda:** Jarak horizontal saling beririsan atau berdekatan.
       * **Ukuran Teks Serupa (PENTING):** Selisih *line height* antara kedua blok **tidak lebih dari 25-30%**. Jika ukurannya jomplang (seperti SFX besar di sebelah teks kecil), biarkan terpisah.
     4. Jika digabung, satukan string-nya (pisahkan dengan \n) dan gunakan rect1.union(rect2) untuk *bounding box* baru.
## 2. Refactor Jalur Translasi Online & Offline
 * **Masalah:** Fungsi translateBlocks dan onlineTranslate saat ini masih menggunakan visionText.textBlocks mentah.
 * **Solusi:**
   * Ubah pemrosesan awal dengan memanggil val mergedBlocks = mergeBlocks(visionText.textBlocks).
   * Lakukan iterasi, pemanggilan drawLoadingBubble, translate, dan drawTranslationBatch menggunakan data dari mergedBlocks tersebut, bukan blok mentah. Pastikan koordinat offset (status bar) tetap diaplikasikan dengan benar.
## 3. Verifikasi & Build
 * Terapkan perubahan ini tanpa merusak struktur coroutine dan timeout dari Fase 7.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi.