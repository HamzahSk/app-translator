
**TUGAS FASE 7: REWRITE TOMBOL START & RE-OPTIMASI ML KIT**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/ bila butuh detail tambahan.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_[nama_task].md (maksimal 200 kata).
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama dan perbarui status proyek.
# INSTRUKSI PERBAIKAN BUG KODE
Kamu bertugas untuk membereskan sisa *bug* performa dari fase sebelumnya dengan melakukan *rewrite* total pada logika tombol Start dan merombak cara inisialisasi ML Kit agar aplikasi benar-benar responsif tanpa jeda.
## 1. Rewrite Total Logika Tombol Start
 * **Masalah:** Penambalan kode sebelumnya masih menyisakan *delay*, tombol harus diklik dua kali agar merespons, dan sangat lambat saat dimatikan.
 * **Solusi:**
   * Hapus seluruh *listener* dan logika *state* tombol Start yang lama di MainActivity.
   * Buat ulang logika tombol dari nol menggunakan pendekatan *State* yang bersih (misal mengandalkan enum atau sealed class: IDLE, PREPARING, RUNNING).
   * Pastikan tidak ada blok kode runBlocking, Thread.sleep, atau operasi berat yang terselip. Pembuatan MediaProjectionManager.createScreenCaptureIntent() sebenarnya cukup cepat, pastikan pemanggilannya instan, dan evaluasi *permission* dilakukan via Coroutine yang tidak mengunci antarmuka utama (UI Thread).
## 2. Re-Optimasi Pre-load & Eksekusi ML Kit
 * **Masalah:** ML Kit masih sangat lambat saat *capture* pertama kali, dan indikator *loading* sering kali gagal muncul. Mekanisme *dummy call* sebelumnya sepertinya menyebabkan *bottleneck*.
 * **Solusi:**
   * Evaluasi ulang metode preloadOfflineModel(). Pindahkan inisialisasi ML Kit ke Dispatchers.Default (karena OCR dan Translator adalah operasi CPU-*bound*, bukan I/O-*bound*).
   * Pastikan proses *pre-load* atau *dummy call* berjalan di *background job* yang independen dan **tidak mengunci** (lock/mutex) instansiasi TextRecognizer saat proses *capture* layar sesungguhnya dipanggil.
   * Untuk memastikan *loading bubble* selalu muncul, paksa UI thread untuk menggambar *bubble* terlebih dahulu (Handler.post atau withContext(Dispatchers.Main)), lalu panggil delay(50) atau yield() agar sistem Android punya waktu *rendering* sebelum coroutine OCR mengambil alih resource secara masif.
## 3. Verifikasi & Build
 * Pastikan fitur perbaikan *Line Spacing* dan *Center Expansion Box* dari Fase 6 di OverlayManager tidak terhapus.
 * Setelah selesai, buat file log task_YYYYMMDD_HHMM_phase7_rewrite.md dan perbarui 00_INDEX.md.
 * Wajib jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memastikan tidak ada error kompilasi.