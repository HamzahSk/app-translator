
**TUGAS FASE 27: IMPLEMENTASI HALAMAN DEBUG & MONITORING PROSES**

**1. PROTOKOL SISTEM & MEMORI**
 * Wajib membaca log eksekusi dari Fase 26 yang berfokus pada persiapan rilis dan perapian struktur Kotlin.
 * Buat log eksekusi baru dengan format task_YYYYMMDD_HHMM_phase27_debug_monitor.md dan tambahkan ke dalam indeks utama.
 * Prioritas utama fase ini adalah memberikan visibilitas terhadap proses *background* yang berjalan untuk menganalisis dan mengatasi masalah *delay* akibat penumpukan proses.
 
**2. MODIFIKASI ANTARMUKA UTAMA (UI)**
 * Buka MainActivity atau komponen UI utama tempat tombol **Setting** berada.
 * Tambahkan tombol **Debug** baru tepat di samping tombol **Setting**.
 * Pastikan desain tombol selaras dengan panduan Material Design 3 yang sudah digunakan pada aplikasi.
 * Hubungkan *listener* tombol ini untuk menavigasikan pengguna ke DebugActivity atau DebugFragment yang baru dibuat.
**3. PEMBUATAN HALAMAN DEBUG (DEBUG SCREEN)**
 * **Manajemen Cache:** Sediakan tombol khusus di dalam halaman Debug untuk menghapus seluruh *cache* aplikasi secara manual.
 * **Monitoring Proses (Process Viewer):** Buat antarmuka berbasis daftar (*list*) yang menampilkan status *real-time* dari *coroutine* atau *thread* yang sedang berjalan.
 * **Visibilitas OCR & Translator:** Integrasikan pemantauan langsung ke dalam TranslationEngine. Tampilkan semua status antrean atau eksekusi proses OCR (Google ML Kit) dan API penerjemah (Offline/Online) secara mendetail.
**4. FITUR KENDALI PROSES (PROCESS CLEARING)**
 * Tambahkan fungsi **Clear All Processes** pada halaman Debug.
 * Fungsi ini harus dapat menghentikan (*cancel*) semua tugas di dalam TranslationEngine atau operasi *capture* dari ScreenCaptureService yang tertunda atau berjalan ganda.
 * Pastikan pembersihan proses ini dilakukan dengan aman (*graceful shutdown*) menggunakan *Coroutine Scope cancellation* agar aplikasi tidak *crash* dan mencegah memori *leak*.
**5. KRITERIA VERIFIKASI PENGUJIAN**
 * Tombol Debug muncul di samping tombol Setting pada layar utama dan berfungsi membuka halaman Debug.
 * Halaman Debug berhasil menampilkan daftar proses OCR dan Translator yang sedang aktif.
 * Menekan tombol "Clear Processes" sukses menghentikan penumpukan *job* tanpa menyebabkan aplikasi *force close*.
 * Fungsi penghapusan *cache* bekerja dengan benar dan membebaskan ruang penyimpanan aplikasi.
