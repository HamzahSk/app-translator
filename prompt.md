# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:

## 1. Protokol Membaca & Menulis Konteks
*   **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
*   **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/` bila butuh detail tambahan.
*   **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_[nama_task].md` (maksimal 200 kata).
*   **Perbarui `00_INDEX.md`:** Tambahkan referensi log baru ke dalam file index utama dan perbarui status proyek.

---

# TUGAS FASE 5.5: BUG FIX OVERLAY, STATICLAYOUT, OFFLINE LOADING, PRE-LOAD ML KIT & TOMBOL START

Kamu bertugas memperbaiki bug fatal dari implementasi Fase 5 sebelumnya dan melakukan optimasi waktu muat. Berikut adalah rincian masalah dan solusi yang WAJIB diterapkan:

## 1. Layar Tidak Bisa Disentuh (Touch Interception Bug)
*   **Masalah:** *Custom View* transparan untuk batch render menggunakan `MATCH_PARENT` tanpa meneruskan *touch event* ke bawahnya, membuat layar *freeze*.
*   **Solusi:** Tambahkan `WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE` pada konfigurasi *layout params* di dalam fungsi `drawTranslationBatch` agar sentuhan tembus ke aplikasi target.

## 2. Teks Memanjang (Text Wrapping Bug)
*   **Masalah:** Menggunakan `canvas.drawText` membuat teks tidak bisa turun ke baris baru (tidak ada *word wrap*), sehingga teks memanjang keluar *bounding box*.
*   **Solusi:** Ganti `canvas.drawText` dengan `StaticLayout` (atau `StaticLayout.Builder` untuk API level baru) di dalam `onDraw`. Atur lebar `StaticLayout` sesuai lebar *bounding box*, lalu gunakan `canvas.translate()` untuk memposisikan teks dengan benar sebelum menggambarnya.

## 3. Missing Loading Bubble di Mode Offline
*   **Masalah:** Mode offline terlihat tidak responsif karena tidak ada *loading canvas*.
*   **Solusi:** Modifikasi fungsi `translateBlocks` pada `TranslationEngine`. Panggil `overlayManager.drawLoadingBubble` untuk semua blok teks yang terdeteksi *sebelum* memanggil `translator.translate()`, lalu hapus *loading* tersebut saat menggantinya dengan hasil terjemahan (*batch*).

## 4. Optimasi Cold Start (Lambat saat pertama kali dipakai)
*   **Masalah:** Pada penggunaan pertama, proses bisa memakan waktu belasan detik karena `downloadModelIfNeeded()` baru dijalankan saat layar di- *capture*.
*   **Solusi:** Buat mekanisme *pre-load* atau inisialisasi awal. Pindahkan/tambahkan pemicu `downloadModelIfNeeded()` ke latar belakang saat aplikasi pertama kali dibuka (misalnya di `MainActivity` atau saat `Service` menyala) agar model bahasa dan OCR *Client* sudah siap sebelum pengguna melakukan *capture* pertama kali.

## 5. Tombol Start Tidak Responsif (Unresponsive Button)
*   **Masalah:** Tombol "Start" di aplikasi terkadang tidak merespons saat ditekan akibat *Main Thread* yang terblokir atau masalah pencegahan klik beruntun.
*   **Solusi:** 
    *   Terapkan mekanisme *debounce* pada *listener* tombol Start (abaikan klik lanjutan selama ~500ms).
    *   Pastikan aksi yang dipicu tombol ini diproses di *background thread* (menggunakan Coroutines `Dispatchers.IO`) agar tidak memblokir UI.
    *   Berikan indikator visual (*loading spinner* atau perubahan state tombol) saat memproses.

## 6. Verifikasi & Build
*   Pastikan selalu menjalankan `./gradlew :app:compileDebugKotlin` atau `./gradlew assembleDebug` setelah melakukan perubahan untuk memverifikasi tidak ada *error* kompilasi.
