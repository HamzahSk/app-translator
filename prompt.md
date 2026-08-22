
**TUGAS FASE 11.5: CRITICAL BUG FIX (UI LAYOUT, ONLINE BATCH RENDERING, & SMART MERGING)**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase11_5_critical_fixes.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PERBAIKAN BUG KODE
Terdapat beberapa *bug* fatal akibat implementasi yang keliru pada fase 11. Perbaiki poin-poin berikut dengan sangat teliti:
## 1. Bug UI Layout Overlay Customization Hilang
 * **Masalah:** Saat tab "Online" dipilih, menu *Overlay Customization* hilang. Ini terjadi karena pada activity_main.xml, atribut android:id="@+id/layoutModelsCard" keliru diletakkan di MaterialCardView milik *Overlay Customization*.
 * **Solusi:**
   * Buka activity_main.xml. Pindahkan ID @+id/layoutModelsCard ke elemen MaterialCardView yang benar, yaitu yang membungkus bagian *"AI Models (OCR)"*.
   * Hapus ID tersebut dari *card* *Overlay Customization* agar selalu terlihat di kedua mode.
## 2. Bug Teks Online Menghilang (Loop Rendering Flashing)
 * **Masalah:** Di TranslationEngine.onlineTranslate, pemanggilan replaceLoading (yang memanggil drawTranslationBubble -> *clear overlay*) di dalam *loop* menyebabkan fungsi *render* saling menghapus isi layar (*flashing*), sehingga layar berakhir kosong meskipun JSON Array berhasil di-*parse*.
 * **Solusi:**
   * Di onlineTranslate, **jangan** panggil replaceLoading di dalam *loop*.
   * Kumpulkan semua hasil terjemahan dari *batch* dan *bounding box*-nya ke dalam sebuah mutableListOf<OverlayManager.Bubble>().
   * Setelah *loop* pemetaan teks selesai, panggil overlayManager.drawTranslationBatch(list) **hanya satu kali**.
   * Hapus semua *loading bubble* menggunakan iterasi removeLoading(key) di blok finally.
## 3. Posisi Loading Bubble Meleset
 * **Masalah:** Fungsi drawLoadingBubble di OverlayManager tidak mematuhi konfigurasi config.placementMode.
 * **Solusi:** Sesuaikan nilai params.x pada drawLoadingBubble dengan mengevaluasi kondisi "left" (geser ke kiri sebesar lebar kotak) dan "right", persis seperti logika *offset* yang sudah ada di drawTranslationBatch.
## 4. Perbaikan Smart Line Merging (Anti Over-merging)
 * **Masalah:** Menggunakan visionText.textBlocks mentah membuat SFX besar dan teks dialog kecil yang berdekatan menyatu.
 * **Solusi:**
   * Ubah fungsi mergeBlocks di TranslationEngine. Jangan iterasi textBlocks. Bongkar elemennya ke level baris: val allLines = blocks.flatMap { it.lines }.
   * Urutkan allLines berdasarkan koordinat Y (atas ke bawah).
   * Gabungkan *lines* menjadi satu MergedBlock **HANYA JIKA** memenuhi syarat: jarak vertikal sangat dekat (< 1.5 * tinggi_baris) **DAN** selisih tinggi kotak (representasi ukuran font) **maksimal 30%**. Jika ukuran beda jauh, jadikan blok terpisah agar lebih akurat.
## 5. Verifikasi & Build
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi.
