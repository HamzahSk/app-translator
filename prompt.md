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

# TUGAS FASE 2: OPTIMASI TRANSLASI, UI LOADING, DAN SPOTLESS

Kamu bertugas sebagai Senior Android Developer. Lanjutkan pengembangan aplikasi Screen Translator dengan mengimplementasikan tugas-tugas berikut:

## 1. Implementasi Plugin Spotless
*   Tambahkan plugin `com.diffplug.spotless` ke dalam file `build.gradle.kts` tingkat aplikasi.
*   Buatkan konfigurasi blok `spotless { ... }` untuk memastikan format kode Kotlin (misalnya menggunakan `ktlint`) tervalidasi saat CI/CD menjalankan task `spotlessCheck`.

## 2. Optimasi Batch Translation (Network Request)
*   **Masalah Saat Ini:** Aplikasi mengirimkan teks ke API (OpenAI/Gemini) satu per satu per *bounding box* hasil OCR. Ini memakan waktu terlalu lama dan boros *request*.
*   **Solusi yang Diharapkan:** 
    *   Kumpulkan semua teks hasil OCR dari layar.
    *   Gabungkan semua teks tersebut ke dalam satu *string* panjang, dipisahkan dengan *delimiter* atau pemisah yang unik (misalnya `|||` atau `\n---\n`).
    *   Kirimkan HANYA SATU *request* ke API AI untuk menerjemahkan teks gabungan tersebut.
    *   Setelah *response* diterima, pecah kembali hasilnya menggunakan *delimiter* yang sama dan petakan kembali ke *bounding box* aslinya.
*   Berikan pembaruan pada logika `TranslationEngine` atau *network client* untuk menangani *batch processing* ini.

## 3. Penambahan Indikator Loading (Skeleton/Empty Canvas) UI
*   **Masalah Saat Ini:** Saat proses OCR dan translasi berjalan via *network*, layar tidak memberikan *feedback* visual, sehingga *user* bingung apakah aplikasi sedang memproses atau tidak.
*   **Solusi yang Diharapkan:**
    *   Segera setelah proses OCR selesai mendeteksi posisi *bounding box* (sebelum *request* translasi selesai), gambar *bubble* atau kanvas kosong berwarna solid transparan (seperti *skeleton loading*) di atas posisi teks asli.
    *   Tambahkan animasi *shimmer* atau indikator *loading* sederhana di dalam kanvas kosong tersebut.
    *   Setelah hasil translasi dari API diterima, timpa kanvas kosong tersebut dengan teks hasil terjemahan.
*   Berikan kode implementasi untuk pembaruan UI *overlay* ini.
