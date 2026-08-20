# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless[span_1](start_span)[span_1](end_span). Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut[span_2](start_span)[span_2](end_span):

## 1. Protokol Membaca Konteks (SEBELUM BEKERJA)
*   **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu untuk memahami status proyek dan riwayat singkat secara keseluruhan[span_3](start_span)[span_3](end_span).
*   **Cek Log Terbaru:** Jika butuh detail pekerjaan terakhir, baca **maksimal 2 file log terbaru** di dalam folder `ai_memory/` (berdasarkan urutan waktu/tanggal)[span_4](start_span)[span_4](end_span).
*   **DILARANG** membaca seluruh file di folder `ai_memory/` sekaligus agar context window tidak kehabisan batas[span_5](start_span)[span_5](end_span).

## 2. Protokol Menulis Konteks (SETELAH SELESAI BEKERJA)
Setiap kali kamu selesai melakukan suatu tugas/commit, lakukan 2 hal berikut[span_6](start_span)[span_6](end_span):
1.  **Buat File Log Baru:** Buat file `.md` baru di folder `ai_memory/` dengan format nama: `task_YYYYMMDD_HHMM_[nama_task].md`[span_7](start_span)[span_7](end_span). Isinya harus singkat (maksimal 150-200 kata) dengan struktur: Status, Ringkasan Perubahan, dan Tugas Selanjutnya (Next Steps)[span_8](start_span)[span_8](end_span).
2.  **Perbarui `00_INDEX.md`:** Tambahkan baris baru di daftar riwayat `00_INDEX.md` yang mengarah ke file log baru tersebut, dan update bagian **"Status Proyek Terkini"**[span_9](start_span)[span_9](end_span).

## 3. Protokol Auto-Archive / Rolling Summary (PEMBERSIHAN OTOMATIS)
*   Periksa jumlah file log di dalam `ai_memory/` (tidak termasuk `00_INDEX.md`)[span_10](start_span)[span_10](end_span).
*   **Jika jumlah file log sudah melebihi 10 file:** Gabungkan dan rangkum isi dari 10 log tersebut menjadi 1 file arsip, misalnya: `archive_phase_1.md`[span_11](start_span)[span_11](end_span). Update `00_INDEX.md` untuk mencatat pengarsipan tersebut, lalu hapus 10 file log individu agar folder tetap bersih[span_12](start_span)[span_12](end_span).

---

# TUGAS FASE 2.5: PERBAIKAN BUG DAN SISTEM PENANGANAN CRASH GLOBAL

Kamu bertugas sebagai Senior Android Developer. Saat ini, aplikasi mengalami *Force Close* secara acak (diduga akibat *thread handling* pada UI Overlay atau kegagalan *parsing* dari batch translation API). Tolong kerjakan tugas berikut:

## 1. Analisis dan Perbaiki Penyebab Crash
*   Tinjau kembali logika di `OverlayManager` dan `TranslationEngine`. Pastikan semua modifikasi UI (menambah/menghapus *loading bubble* dan *translation bubble*) HANYA dijalankan di *Main Thread* (UI Thread).
*   Pastikan blok `try-catch` menangani *NullPointerException* atau *IndexOutOfBoundsException* saat memecah *string* berdasarkan *delimiter*.

## 2. Implementasi Global Crash Handler
*   Buat kelas `GlobalExceptionHandler` yang mengimplementasikan `Thread.UncaughtExceptionHandler`.
*   Tangkap semua *error* fatal yang tidak tertangani agar aplikasi tidak langsung terhenti paksa oleh OS.

## 3. Buat UI Layar Error (CrashActivity)
*   Jika *crash* terdeteksi, arahkan pengguna ke `CrashActivity` baru.
*   Layar ini harus menampilkan pesan *stack trace error* secara lengkap.
*   Sediakan tombol "Salin Error" (Copy to Clipboard) dan tombol "Bagikan" (Share Intent) agar pengguna bisa dengan mudah melaporkan *bug* tersebut.

## 4. Sistem Log Error Lokal (Android/data/...)
*   Simpan setiap *stack trace crash* (beserta info *timestamp* dan spesifikasi *device*) ke dalam file `.txt`.
*   Simpan file ini di direktori eksternal aplikasi, yaitu menggunakan `context.getExternalFilesDir(null)` sehingga file tersimpan rapi di `Android/data/com.ervareza.screentranslator/files/` dan mudah diakses pengguna via File Manager.
