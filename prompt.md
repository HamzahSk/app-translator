**TUGAS FASE 20: FIX BUG ROTASI CANVAS, MODE TRANSPARAN & EFEK PENGHAPUS (SMART ERASER)**

## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat agar token tidak kelebihan beban.
 * **Cek Konteks:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu, dilanjutkan dengan membaca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Perekaman Log:** Setelah selesai, buat log baru bernama `task_YYYYMMDD_HHMM_phase20_transparent_eraser_fix.md` dan pastikan untuk menambahkan referensinya ke dalam file index utama.

## 2. ANALISIS KEBUTUHAN & BUG
Fase ini berfokus pada perbaikan *bug* rotasi dari Fase 19 dan peningkatan estetika visual agar teks terjemahan terasa menyatu (*seamless*) dengan gambar komik:
 1. **Bug Rotasi Canvas:** Saat ini, fitur Auto Rotate hanya memutar teksnya saja. *Background* dan *border* *bubble* tetap tegak lurus karena `canvas.rotate()` diletakkan *setelah* `canvas.drawRoundRect()`.
 2. **Mode Transparan:** Pengguna ingin menghilangkan kotak *background* dan *border* pada *bubble* terjemahan agar visual komik tidak tertutup kotak kaku.
 3. **Text Outline (Stroke):** Ketika *background* dihilangkan, teks bisa tidak terbaca jika warnanya bertabrakan dengan warna gambar di belakangnya. Teks memerlukan efek garis luar (*outline* tebal) agar kontras.
 4. **Efek Penghapus Teks Asli (Smart Eraser):** Untuk meniru efek "penghapus", sistem tidak bisa memanipulasi aplikasi di bawahnya. Sebagai gantinya, sistem harus melakukan *Color Sampling* (mengambil warna dominan di tepi area *bounding box* dari *Bitmap* hasil *capture*), lalu mengecat area teks asli dengan warna tersebut sebagai penutup sebelum teks terjemahan digambar.

## 3. OBJEKTIF PENGEMBANGAN (FASE 20)
 * **Selesaikan Bug Rotasi di `OverlayManager.kt`:**
   * Pindahkan pemanggilan `canvas.save()` dan `canvas.rotate(bubble.rotation, ...)` ke atas, tepat **SEBELUM** blok kode yang menggambar *background* (`canvas.drawRoundRect()`). Pastikan *background*, *border*, dan teks berada dalam satu *state* rotasi yang sama.
 * **Penambahan Konfigurasi & UI:**
   * Di `ConfigManager.kt`, tambahkan `isTransparentModeEnabled` (boolean, default: false) dan `isEraserModeEnabled` (boolean, default: false).
   * Di `SettingsDialog.kt`, tambahkan dua UI `MaterialSwitch` untuk mengaktifkan "Transparent Mode" dan "Smart Eraser (Hide Original Text)".
 * **Algoritma Smart Eraser (Color Sampling) di `TranslationEngine.kt`:**
   * Buat fungsi *helper* `getDominantBackgroundColor(bitmap: Bitmap, rect: Rect): Int`. Fungsi ini membaca piksel-piksel di tepi (perimeter) `rect` pada `Bitmap` dan mencari warna yang paling sering muncul (meniru warna kanvas komik).
   * Ekstrak warna ini di `processImageInternal` (atau saat *merging*), simpan ke dalam `MergedBlock`, dan teruskan ke `OverlayManager.Bubble`.
 * **Implementasi Transparent Mode & Outline di `OverlayManager.kt`:**
   * Jika `isTransparentModeEnabled` aktif: lewati proses `canvas.drawRoundRect` untuk *background* dan *border*.
   * Tambahkan efek *Outline* pada teks terjemahan: 
     * Ubah cara menggambar `StaticLayout`. Pertama, set `textPaint.style = Paint.Style.STROKE` dengan ketebalan yang memadai (misal 4dp/5dp) dan warna kontras (misal kebalikan dari warna teks) lalu panggil `layout.draw(canvas)`.
     * Kedua, set `textPaint.style = Paint.Style.FILL` dengan warna teks utama dan panggil `layout.draw(canvas)` lagi di posisi yang sama.
 * **Eksekusi Efek Eraser:**
   * Di `OverlayManager.kt`, jika `isEraserModeEnabled` aktif dan `isTransparentModeEnabled` juga aktif, gambar sebuah kotak (`canvas.drawRect` atau `drawRoundRect`) menutupi seluruh `boundingBox` menggunakan `sampledColor` yang didapat dari AI *Engine*, sebelum menggambar teks terjemahan.

## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * Terjemahkan gambar dengan teks miring, pastikan *background* *bubble* kini ikut miring mengikuti arah teks secara presisi.
 * Aktifkan Mode Transparan, pastikan *background bubble* hilang namun teks tetap sangat mudah dibaca karena adanya *outline*.
 * Aktifkan Mode Smart Eraser, pastikan kotak teks asli tertutup sempurna oleh blok warna yang senada dengan latar belakang komik tersebut.
 * Jalankan perintah `./gradlew spotlessApply` untuk merapikan format, lalu verifikasi keberhasilan kompilasi tanpa *error* dengan `./gradlew assembleDebug`.
