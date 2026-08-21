
**TUGAS FASE 8: OCR BLOCK MERGING & ANTI-OVERLAP BUBBLE**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase8_block_merging.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
# INSTRUKSI PERBAIKAN BUG KODE
Kamu bertugas menyelesaikan masalah fragmentasi OCR di TranslationEngine.kt. Saat ini, ML Kit memecah satu gelembung percakapan menjadi banyak TextBlock kecil, menyebabkan hasil terjemahan bertumpuk. Terapkan solusi berikut:
## 1. Implementasi Algoritma Spatial Block Merging
 * **Masalah:** Iterasi langsung pada visionText.textBlocks membuat teks yang berdekatan digambar terpisah dan saling menimpa.
 * **Solusi:**
   * Buat sebuah *data class* baru, misal MergedBlock(val text: String, val boundingBox: Rect).
   * Buat fungsi utilitas mergeBlocks(blocks: List<Text.TextBlock>): List<MergedBlock> di dalam TranslationEngine.
   * **Logika Penggabungan:** Urutkan blok berdasarkan koordinat Y (atas ke bawah). Evaluasi jarak antar blok. Jika jarak vertikal (block2.top - block1.bottom) atau jarak horizontal antar blok berdekatan sangat kecil (misal kurang dari **1.5x tinggi rata-rata teks**), gabungkan teksnya (pisahkan dengan spasi atau *newline*) dan perluas *bounding box*-nya menggunakan rect1.union(rect2).
## 2. Refactor Jalur Translasi Online & Offline
 * **Masalah:** Pemanggilan fungsi translateBlocks dan onlineTranslate harus disesuaikan dengan struktur data yang baru.
 * **Solusi:**
   * Ubah argumen fungsi dari visionText: Text menjadi menggunakan hasil dari mergeBlocks().
   * Terapkan iterasi pada MergedBlock untuk proses *loading bubble*, translasi, dan *rendering* teks akhir.
## 3. Verifikasi & Build
 * Terapkan perubahan ini tanpa merusak fitur Fase 7.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi.
Silakan dicoba! Sebagai catatan, jarak toleransi penggabungan blok (*threshold*) mungkin perlu diatur ulang (di-*tweak*) nantinya. Apakah kamu ingin kita jadikan batas jarak antar-blok ini sebagai fitur *slider* di pengaturan aplikasi agar bisa diatur manual?
