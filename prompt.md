**TUGAS FASE 13: UI SETTINGS POLISH, BUG FIX CANVAS LOADING, & REFACTOR I18N**

# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_phase13_ui_i18n_fixes.md`.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase ini, kita akan memoles UI Settings Popup, memperbaiki *bug* logika penempatan *loading bubble*, dan merombak arsitektur lokalisasi (i18n) agar lebih rapi. Kerjakan poin-poin berikut:

## 1. Poles Tampilan Settings Popup (UI/UX)
 * **Masalah:** Popup *Settings* terlihat terlalu *flat*, tidak ada tombol *Close* (X), dan *Segmented Button* (Direct / Left / Right) tidak menunjukkan state warna yang jelas saat dipilih, sehingga pengguna kebingungan.
 * **Solusi:**
   * **Tombol Close:** Tambahkan ikon "X" (Close) di sudut kanan atas pada *layout* Settings Popup untuk menutup dialog.
   * **State Warna Segmented Button:** Perbaiki *background color* atau *tint* pada tombol "Direct", "Left", dan "Right". Pastikan tombol yang sedang aktif/dipilih memiliki warna yang kontras (misal: warna *Primary* tebal) dibandingkan tombol yang tidak aktif.
   * **Shadow/Elevation:** Tambahkan sedikit *elevation*, *shadow*, atau *background* dengan sudut *rounded* yang lebih halus agar popup tidak terlihat kaku/flat.

## 2. Perbaikan Bug Placement (Direct / Left / Right) & Canvas Loading
 * **Masalah:** Opsi "Direct / Left / Right" saat ini keliru menggeser *seluruh canvas loading* menjadi lebih ke kiri/kanan. Seharusnya opsi ini berfungsi untuk mengatur penempatan/alignment gelembung teks terjemahan atau *alignment* teks di dalam kotak, sementara *canvas loading*-nya tetap berada di posisi aslinya (statis menyesuaikan ukuran *bounding box* teks asli).
 * **Solusi:**
   * Evaluasi logika *rendering* di `OverlayManager` (pada fungsi `drawLoadingBubble` dan *render* terjemahan).
   * Pastikan posisi koordinat (*x, y*) dari *canvas* utama/loading tidak ikut tergeser. 
   * Terapkan logika "Direct", "Left", dan "Right" **HANYA** untuk mengatur letak *bubble translation* di atas layar atau *text-alignment* (Center, Left, Right) di dalam *bubble* tersebut sesuai *bounding box* aslinya.

## 3. Refactor Arsitektur i18n (Lokalisasi String)
 * **Masalah:** String *hardcoded* atau manajemen `strings.xml` bawaan Android saat ini sulit dikelola untuk diterjemahkan oleh *translator* eksternal.
 * **Solusi:**
   * Pindahkan semua *string* aplikasi ke sebuah sistem i18n berbasis file di *root directory* (atau di dalam folder `assets/i18n/` agar mudah dibaca aplikasi).
   * Buat struktur sub-folder atau file terpisah untuk masing-masing bahasa:
     - Base / Default: Bahasa Inggris (`en`).
     - Tambahan: Bahasa Indonesia (`id`).
   * **Contoh Struktur:** `assets/i18n/en/strings.json` dan `assets/i18n/id/strings.json`.
   * Implementasikan *helper/manager* (misal: `I18nManager.kt`) untuk me-*load* string dari file JSON tersebut secara dinamis menggantikan pemanggilan `getString()` konvensional Android, sehingga ketika bahasa di aplikasi diganti, UI otomatis menggunakan JSON yang sesuai.

## 4. Verifikasi & Build
 * Pastikan untuk menguji apakah memencet "X" menutup popup, opsi "Left/Right" berfungsi tanpa menggeser *canvas*, dan semua teks UI merespon file i18n dengan benar.
 * Jalankan `./gradlew :app:compileDebugKotlin` atau `./gradlew assembleDebug` untuk memverifikasi tidak ada *error* kompilasi.