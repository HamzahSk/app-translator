**TUGAS FASE 19: OVERLAY BUG FIX, DETEKSI ROTASI TEKS & UI KONTROL SMART MERGE**

## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat agar token tidak kelebihan beban.
 * **Cek Konteks:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu, dilanjutkan dengan membaca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Perekaman Log:** Setelah selesai, buat log baru bernama `task_YYYYMMDD_HHMM_phase19_overlay_rotation_merge.md` dan pastikan untuk menambahkan referensinya ke dalam file index utama.

## 2. ANALISIS KEBUTUHAN & BUG
Fase ini berfokus pada penyelesaian bug spesifik dan penambahan fitur kontrol visual:
 1. **Bug Overlay Kebal (Gallery Bug):** Di aplikasi tertentu seperti Galeri, *touch events* terblokir oleh *custom canvas* aplikasi. Akibatnya, `OverlayManager` tidak mendeteksi sentuhan untuk menghapus *canvas* saat layar di-*scroll* atau di-*zoom*.
 2. **Deteksi Kemiringan Teks (Auto Rotate):** Teks pada komik sering melengkung/miring. OCR saat ini hanya mengambil `boundingBox` 2D yang kaku, sehingga *bubble* terjemahan tidak sejajar dengan kemiringan teks asli.
 3. **Hardcoded Merge Heuristics:** Nilai toleransi penggabungan teks (`MERGE_VERTICAL_GAP_MULTIPLIER`, `MERGE_HORIZONTAL_GAP_RATIO`, `MERGE_SIZE_TOLERANCE`) tertanam mati di kode. Pengguna butuh UI *slider* di Pengaturan untuk menyesuaikan sensitivitas ini secara dinamis.

## 3. OBJEKTIF PENGEMBANGAN (FASE 19)
 * **Selesaikan Bug Overlay (Observer Trick):** 
   * Di `OverlayManager.kt` pada fungsi `drawTranslationBatch`, buat *View* "mata-mata" berukuran 1x1 pixel transparan dengan parameter `FLAG_WATCH_OUTSIDE_TOUCH` dan `FLAG_NOT_TOUCH_MODAL`.
   * Beri *Touch Listener* pada *View* ini untuk menangkap `MotionEvent.ACTION_OUTSIDE` dan memicu `clearOverlaysInternal()`. Daftarkan ke `windowManager` dan `activeViews`.
 * **Deteksi Kemiringan & UI Auto Rotate:**
   * Di `ConfigManager.kt`, tambahkan variabel `isAutoRotateEnabled` (boolean, default: false).
   * Di `SettingsDialog.kt`, tambahkan UI `MaterialSwitch` untuk "Auto Rotate Canvas" yang mengubah nilai config tersebut.
   * Di `TranslationEngine.kt` (fungsi `mergeBlocks`), jika fitur aktif, ubah pengambilan batas dari `boundingBox` menjadi `cornerPoints`. Hitung rotasi dengan: $\theta = \text{atan2}(y_2 - y_1, x_2 - x_1) \times \frac{180}{\pi}$.
   * Terapkan nilai sudut ini di `MergedBlock`, lalu gunakan `canvas.rotate()` di `OverlayManager.kt` saat menggambar *bubble*.
 * **Ekspos Variabel Smart Merge ke UI:**
   * Pindahkan konstanta ini dari `TranslationEngine.kt` menjadi variabel di `ConfigManager.kt`:
     * `mergeVerticalGapMultiplier` (Default: 1.2f)
     * `mergeHorizontalGapRatio` (Default: 0.15f)
     * `mergeSizeTolerance` (Default: 0.45f)
   * Di `SettingsDialog.kt`, gunakan fungsi `bindSlider` yang sudah ada untuk membuat 3 *Slider* baru agar pengguna bisa mengatur nilai ketiga variabel tersebut secara *real-time*.
   * Pastikan algoritma `mergeBlocks` langsung membaca nilai dari config, bukan lagi konstanta statis.

## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * Uji sentuhan pada aplikasi Galeri untuk memverifikasi trik 1x1 pixel berhasil menghapus *canvas* saat layar disentuh di luar *bubble*.
 * Buka Pengaturan, verifikasi keberadaan satu *Switch* baru (Auto Rotate) dan tiga *Slider* baru (Smart Merge) berfungsi tanpa *crash*.
 * Modifikasi nilai *Slider* Smart Merge, dan buktikan pengelompokan teks berubah sesuai toleransi baru.
 * Jalankan perintah `./gradlew spotlessApply` untuk merapikan format, lalu verifikasi keberhasilan kompilasi tanpa *error* dengan `./gradlew assembleDebug`.
