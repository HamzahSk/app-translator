### TUGAS FASE 25: IMPLEMENTASI "SMART MODEL MANAGER" & PERBAIKAN UI VISIBILITAS OCR

**1. PROTOKOL SISTEM & MEMORI**
* Wajib membaca `ai_memory/00_INDEX.md` dan maksimal 2 log terbaru sebelum memulai eksekusi[span_1](start_span)[span_1](end_span)[span_2](start_span)[span_2](end_span).
* Buat log eksekusi baru dengan format `task_YYYYMMDD_HHMM_phase25_model_manager.md` dan pastikan direferensikan ke dalam index utama.
* Pertahankan aturan i18n dari Fase 24: dilarang menggunakan *hardcoded string* untuk UI baru ini, wajib daftarkan ke file XML/JSON bahasa yang ada[span_3](start_span)[span_3](end_span).

**2. OBJEKTIF PENGEMBANGAN FITUR & BUG FIX**
* **BUG FIX - Visibilitas OCR:** Pada bagian `AI Translation Mode` (Offline/Online), perbaiki *logic* visibilitas. Bagian "AI Models (OCR)" HARUS tetap tampil meskipun *tab* "Online" sedang dipilih. Hal ini karena proses OCR selalu berjalan secara *offline* menggunakan ML Kit terlepas dari metode terjemahannya.
* **Fitur 1 - Dialog Manajer Model OCR:** 
    * Ubah daftar model OCR (Jepang, Korea, Cina, Devanagari, Latin) yang saat ini tampil memanjang di layar utama menjadi sebuah tombol berbunyi "Kelola Model OCR".
    * Jika tombol ditekan, munculkan *Material 3 Dialog* atau *BottomSheet*.
    * Di dalam dialog, tampilkan *list* model beserta: Status (Terinstal/Belum Terinstal), Estimasi Ukuran File (MB), dan tombol *action* (Download / Hapus).
* **Fitur 2 - Dialog Manajer Model Terjemahan (Offline):**
    * Di bawah opsi "Offline" pada *tab* terjemahan, tambahkan tombol "Kelola Model Terjemahan".
    * Jika ditekan, munculkan dialog serupa yang menampilkan daftar model bahasa terjemahan ML Kit.
    * Tampilkan informasi yang sama: Status, Ukuran File (MB), dan opsi Download / Hapus.

**3. IMPLEMENTASI TEKNIS & API**
* Gunakan `RemoteModelManager` dari Google ML Kit untuk mengecek status unduhan, mengunduh, dan menghapus model secara dinamis.
* Lakukan pengecekan ukuran model (jika API ML Kit tidak menyediakan ukuran pasti secara langsung, gunakan estimasi *hardcoded* yang wajar sesuai dokumentasi ML Kit, atau ambil ukuran *byte* dari *file* lokal jika sudah terunduh).
* Pastikan UI *state* bereaksi secara asinkron (*Coroutines* / *StateFlow*) agar saat pengguna menekan "Download" atau "Hapus", UI status (loading, terinstal, belum) langsung *update* tanpa perlu me-*restart* aplikasi.

**4. KRITERIA VERIFIKASI**
* Bagian OCR tidak menghilang saat mode Online aktif.
* Model bisa diunduh dan dihapus langsung dari dalam UI Dialog.
* Lolos kompilasi tanpa *error* (`./gradlew assembleDebug`) dan semua teks mematuhi sistem i18n.
