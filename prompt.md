
### TUGAS FASE 24: IMPLEMENTASI I18N MENYELURUH
**1. PROTOKOL SISTEM & MEMORI**
 * Mengingat operasi berjalan di lingkungan GitHub Actions yang *stateless*, wajib jaga protokol memori dengan membaca ai_memory/00_INDEX.md dan maksimal 2 log terbaru sebelum mulai.
 * Buat log eksekusi baru dengan format task_YYYYMMDD_HHMM_phase24_i18n.md dan pastikan direferensikan ke index utama.
**2. OBJEKTIF PENGEMBANGAN FITUR**
 * **Migrasi i18n:** Pindahkan seluruh *string* aplikasi ke dalam sistem i18n bawaan.
 * **Cakupan Ekstraksi:** Berlaku untuk semua teks yang terlihat oleh pengguna maupun log sistem. Ini termasuk teks antarmuka, dialog, menu pengaturan warna teks, hingga notifikasi (*Toast/Snackbar*) transisi koneksi seperti "Koneksi terputus, beralih ke mode offline otomatis".
**3. IMPLEMENTASI TEKNIS & KEAMANAN**
 * Bersihkan semua *hardcoded string* di seluruh struktur *project* tanpa merusak fungsionalitas fitur yang sudah berjalan.
**4. KRITERIA VERIFIKASI**
 * Tidak ada lagi teks *hardcoded* yang tersisa di dalam *project*.
 * Tampilan UI, menu kustomisasi, dan notifikasi *offline fallback* tetap berjalan normal dengan teks yang bersumber dari i18n.
 * Lolos verifikasi kompilasi tanpa *error* menggunakan perintah ./gradlew assembleDebug.
