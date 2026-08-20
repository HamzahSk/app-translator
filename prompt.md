# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:

## 1. Protokol Membaca & Menulis Konteks
*   **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
*   **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/` bila butuh detail tambahan.
*   **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_[nama_task].md` (maksimal 200 kata).
*   **Perbarui `00_INDEX.md`:** Tambahkan referensi log baru ke dalam file index utama dan perbarui status proyek.

---

# TUGAS FASE 4: PERBAIKAN KOORDINAT OVERLAY & OPTIMASI BACKGROUND PROCESS

Kamu bertugas sebagai Senior Android Developer. Aplikasi saat ini sudah bisa memproses translasi *batch*, namun terdapat isu pada pemetaan koordinat UI dan manajemen antrean proses. Tolong perbaiki masalah berikut:

## 1. Koreksi Offset Bounding Box & Status Bar
*   **Masalah:** *Canvas/bubble* translasi tidak pas menimpa teks asli (melenceng ke bawah). Selain itu, status bar (sinyal, jam, baterai) ikut ter-OCR dan diterjemahkan.
*   **Solusi:** 
    *   Dapatkan tinggi *Status Bar* sistem (menggunakan `WindowInsets` atau *resource* dimensi).
    *   Kurangi koordinat Y dari *bounding box* hasil OCR dengan tinggi *Status Bar* tersebut agar *overlay* pas di tempatnya.
    *   Tambahkan filter agar hasil OCR yang berada di area *Status Bar* (koordinat Y di bawah nilai tinggi status bar) diabaikan/dihapus dari daftar yang akan dikirim ke API.

## 2. Inactivity Delay (Debounce) Custom
*   **Masalah:** Proses deteksi OCR perlu jeda agar tidak terlalu agresif saat pengguna membaca.
*   **Solusi:** Tambahkan konfigurasi *delay custom* (opsi default **1.5 detik**) di `ConfigManager`. Proses *Screen Capture* dan OCR hanya boleh dipicu jika tidak ada aktivitas *scroll* selama waktu tersebut.

## 3. Pembatalan Proses (Coroutine Cancellation)
*   **Masalah:** Jika pengguna melakukan *scroll* saat proses OCR/Network sedang berjalan, proses tersebut terus berlanjut di *background* sehingga *canvas* menumpuk.
*   **Solusi:** 
    *   Gunakan manajemen Kotlin Coroutine `Job` pada *service* pendeteksi aktivitas.
    *   Setiap kali ada aktivitas layar (*scroll/touch*), segera **batalkan (cancel)** `Job` translasi yang sedang berjalan saat itu juga.
    *   Pastikan logika `TranslationEngine` merespons *cancellation* ini (menggunakan `ensureActive()` atau mengecek status *coroutine*) dan segera menjalankan perintah `clearOverlays()` untuk membersihkan layar dari sisa *loading bubble* yang terputus.
