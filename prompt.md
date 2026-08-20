
**TUGAS FASE 6: REFINEMENT UI/UX, TOMBOL START, OFFLINE HANG/STUCK FIX & CUSTOM SPACING FONT**
Kamu bertugas melakukan *polishing* dan membereskan bug *blocker* dari Fase 5.5 sebelumnya. Terdapat masalah *hanging* pada proses *offline* dan *styling* teks yang masih perlu disempurnakan. Terapkan perbaikan berikut:
## 1. Perbaikan Proses Nyangkut (*Hanging/Stuck*) di Mode Offline
 * **Masalah:** Terkadang *loading bubble* (...) muncul di layar, tapi proses terjemahan berhenti total (*stuck*). Jika sudah begini, *request* selanjutnya tidak akan merespons kecuali *service* dimatikan lalu dinyalakan ulang.
 * **Solusi:**
   * Bungkus pemanggilan .await() pada OCR (recognizer.process) dan Translator (translator.translate) di dalam blok withTimeout(7000L) atau durasi yang wajar (7 detik). Jika ML Kit mengalami *freeze* secara internal, TimeoutCancellationException akan dilempar sehingga coroutine tidak menggantung selamanya.
   * Pastikan blok try-catch menangkap *exception* *timeout* tersebut, lalu pastikan overlayManager.removeLoading(key) dan pembersihan *state* dieksekusi agar sistem siap memproses *capture* layar selanjutnya.
   * Guna mengurangi potensi *freeze* di awal, buat mekanisme **Pre-load (Dummy Call)**. Di dalam blok preloadOfflineModel(), selain memanggil downloadModelIfNeeded(), buat pemanggilan semu ke OCR menggunakan bitmap kosong (1x1 pixel) dan jalankan translate("test"). Ini memaksa ML Kit untuk memuat model dari *storage* ke RAM (memori) sejak *service* pertama kali aktif.
## 2. Perbaikan Ekstrem Delay Tombol Start (Multi-Click & ANR Fix)
 * **Masalah:** Tombol "Start" kadang butuh diklik berkali-kali, aplikasinya *freeze* (notifikasi persetujuan layar lama muncul), dan saat *service* dimatikan juga responnya lambat.
 * **Solusi:**
   * Hapus / pindahkan pengecekan inisialisasi sistem yang memberatkan di *Main Thread* sebelum MediaProjectionManager.createScreenCaptureIntent() dipanggil.
   * Pastikan status *Permission* (Overlay & Accessibility) hanya di-*cache* atau dicek secara *asynchronous*.
   * Gunakan lifecycleScope.launch(Dispatchers.Main) untuk mengubah UI (teks tombol, *spinner*), namun pindahkan eksekusi berat sepenuhnya ke Dispatchers.IO. Jangan lupa berikan logika penahan klik ganda (*debounce*).
## 3. Custom Line Spacing & Padding Teks (StaticLayout Refinement)
 * **Masalah:** Jarak antar baris teks (*line spacing*) di terjemahan terlalu mepet. Saat teks melebihi ukuran *bounding box* aslinya, kotak *bubble* hanya memanjang ke bawah sehingga posisinya menjadi tidak sentris terhadap teks asli.
 * **Solusi:**
   * Pada StaticLayout.Builder di OverlayManager, tambahkan .setLineSpacing(4f, 1.2f) (nilai *add* dan *multiplier* dibuat *adjustable*) agar baris teks tidak saling bertabrakan.
   * **Perhitungan Bounding Box Vertikal (Center Expansion):** Setelah StaticLayout selesai di-*build*, hitung total tinggi teks (layout.height). Jika tinggi teks lebih besar dari bounding_box.height(), jangan hanya membiarkannya tumpah ke bawah. Geser koordinat Y (*top*) awal ke arah **atas** sebesar setengah dari selisih tingginya: new_Y = original_Y - ((layout.height - original_height) / 2). Ini membuat kotak teks memuai dari titik tengah.
## 4. Verifikasi Memori & Build
 * Terapkan perubahan ini tanpa merusak fitur Fase 5.
 * Setelah selesai, buat file log task_YYYYMMDD_HHMM_phase6_refinement.md sesuai protokol memori dan perbarui 00_INDEX.md.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi.