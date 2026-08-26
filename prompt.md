
### TUGAS FASE 26: PERSIAPAN RILIS, PEMBARUAN DOKUMENTASI & PERAPIAN STRUKTUR KOTLIN
**1. PROTOKOL SISTEM & MEMORI**
 * Wajib membaca ai_memory/00_INDEX.md dan log eksekusi dari Fase 25 (task_*_phase25_model_manager.md) untuk memahami *state* proyek terakhir.
 * Buat log eksekusi baru dengan format task_YYYYMMDD_HHMM_phase26_release_prep.md dan tambahkan ke dalam *index* utama.
 * Prioritas utama fase ini adalah stabilitas, dokumentasi, dan perapian. Dilarang merusak fungsionalitas fitur (*Smart Model Manager* & perbaikan UI OCR) yang sudah stabil di Fase 25.
 
**2. PEMBARUAN DOKUMENTASI & CHANGELOG**
 * **Changelog (CHANGELOG.md):**
   * Buat file ini jika belum ada.
   * Tambahkan entri untuk versi rilis yang akan datang (atau di bawah *tag* [Unreleased]).
   * Rangkum fitur-fitur terbaru secara profesional: Implementasi *Smart Model Manager* (pengelolaan model ML Kit untuk OCR dan Terjemahan Offline), perbaikan *bug* visibilitas OCR, dan pembaruan sistem i18n.
 * **Update README / Docs:**
   * Perbarui file README.md (atau folder docs/).
   * Tambahkan bagian penjelasan singkat terkait fitur baru, arsitektur pengelolaan model (ML Kit), dan status *offline/online translation*.
 * **KDoc / Inline Documentation:**
   * Lakukan *scanning* pada *class* utama (*ViewModel*, *Repository*, *Dialog Components*). Tambahkan komentar KDoc yang memadai untuk memudahkan *maintenance* di masa depan.
**3. PERAPIAN STRUKTUR KOTLIN & PERSIAPAN RILIS**
 * **Strukturisasi & Refactoring:**
   * Evaluasi struktur *package* Kotlin saat ini. Pastikan *separation of concerns* diterapkan (misal: pisahkan *package* ui.dialogs, data.repository, domain.models, dll).
   * Pindahkan *file* atau *class* yang masih berantakan ke *package* yang sesuai dengan arsitektur (MVVM/Clean Architecture).
 * **Pembersihan Kode:**
   * Hapus semua *unused imports*, *dead code*, *TODOs* yang sudah usang, dan hapus/komen semua Log.d atau println yang digunakan untuk *debugging* pada fase sebelumnya.
 * **Konfigurasi Build & Keamanan:**
   * Siapkan konfigurasi untuk rilis. Naikkan versionCode dan versionName di build.gradle.kts (atau build.gradle).
   * Periksa proguard-rules.pro. Tambahkan aturan (*keep rules*) yang diperlukan untuk Google ML Kit agar aplikasi tidak *crash* saat di-*build* dalam mode rilis (R8 obfuscation).
**4. KRITERIA VERIFIKASI PENGUJIAN**
 * File CHANGELOG.md dan dokumentasi berhasil diperbarui dengan log fitur Fase 24 & 25.
 * Struktur *package* Kotlin rapi dan terorganisir, tidak ada *warning lint* terkait *unused imports*.
 * Proyek lolos kompilasi tanpa *error* untuk *build* rilis (./gradlew assembleRelease).
 * Di akhir tugas, berikan ringkasan (*summary*) daftar *file* yang dimodifikasi, dihapus, atau dipindahkan.
