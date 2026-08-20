# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:

## 1. Protokol Membaca & Menulis Konteks
*   **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
*   **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/` bila butuh detail tambahan.
*   **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_[nama_task].md` (maksimal 200 kata).
*   **Perbarui `00_INDEX.md`:** Tambahkan referensi log baru ke dalam file index utama dan perbarui status proyek.

---

# TUGAS FASE 3: PERBAIKAN BUG MATERIAL SLIDER (ILLEGAL STATE EXCEPTION)

Kamu bertugas sebagai Senior Android Developer. Berdasarkan log dari *Global Crash Handler*, aplikasi mengalami *Force Close* saat inisialisasi UI. Berikut adalah detail tugasmu:

## 1. Analisis Bug Slider
*   **Log Error:** `java.lang.IllegalStateException: Value(74.0) must be equal to valueFrom(10.0) plus a multiple of stepSize(5.0) when using stepSize(5.0)`
*   **Penyebab:** Komponen Material `Slider` (untuk *Opacity*, *Text Size*, atau *Corner Radius*) di `MainActivity` memuat nilai dari `ConfigManager` yang bukan kelipatan dari `stepSize` yang dikonfigurasi.

## 2. Implementasi Perbaikan (Hapus StepSize)
*   **Instruksi:** Saya ingin pengguna bisa menggeser *slider* sebebas mungkin tanpa dibatasi kelipatan angka tertentu agar lebih fleksibel.
*   Cari file `activity_main.xml` (atau file layout yang relevan) dan **hapus** atribut `app:stepSize="5.0"` (atau ubah menjadi `app:stepSize="0.0"` untuk *continuous slider*) pada semua komponen `Slider` yang ada.
*   Pastikan di `MainActivity.kt` bagian *listener* `Slider` mengkonversi nilai *float* kembali ke *integer* dengan benar (jika diperlukan oleh `ConfigManager`) menggunakan pembulatan standar (`.toInt()` atau `Math.round()`), sehingga tidak ada lagi masalah tipe data atau *crash*.
