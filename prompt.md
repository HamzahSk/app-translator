**TUGAS FASE 22: VERIFIKASI KEAMANAN, KOMPATIBILITAS LINTAS OS (ANDROID 7 - 14) & AUTOMATED TESTING**

## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat agar token tidak kelebihan beban.
 * **Cek Konteks:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu, dilanjutkan dengan membaca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Perekaman Log:** Setelah selesai, buat log baru bernama `task_YYYYMMDD_HHMM_phase22_security_os_compatibility.md` dan pastikan untuk menambahkan referensinya ke dalam file index utama.

## 2. ANALISIS KEBUTUHAN & BUG
Fase ini berfokus pada stabilitas tingkat produksi (production-ready), keamanan, dan jangkauan perangkat yang lebih luas:
 1. **Kompatibilitas Versi Lama (Backward Compatibility):** Target saat ini terlalu tinggi. Kita ingin menjangkau pengguna Android lama. Aplikasi harus bisa berjalan lancar minimal di Android 8 (API 26), namun usahakan keras untuk menurunkannya hingga Android 7.0 (API 24).
 2. **Bug Eksekusi Android 12+ (API 31+):** Android 12 hingga 14 sangat ketat mengenai keamanan komponen. Sering terjadi crash karena kurangnya atribut `android:exported` di *Manifest*, penggunaan `PendingIntent` tanpa flag `IMMUTABLE`/`MUTABLE`, atau absennya deklarasi `foregroundServiceType="mediaProjection"` (krusial untuk Screen Capture di Android 14+).
 3. **Keamanan Kode (Security Hardening):** Perlu dilakukan *code audit* otomatis untuk mencegah kebocoran memori (bitmap yang tidak di-*recycle*), *intent hijacking*, dan memvalidasi izin akses aplikasi.
 4. **Automated ADB Testing di GitHub Actions:** Karena kita tidak memiliki perangkat fisik secara langsung, kita perlu mensimulasikan pengujian bug menggunakan Android Emulator (via ADB) langsung dari pipeline GitHub Actions.

## 3. OBJEKTIF PENGEMBANGAN (FASE 22)
 * **Penyesuaian API Level (`build.gradle.kts`):**
   * Turunkan `minSdkVersion` dari 26 menjadi 24 (Android 7.0). Jika Android 7 mustahil karena *dependency* (misalnya ML Kit Text Recognition versi terbaru mensyaratkan API lebih tinggi), jadikan 26 (Android 8.0) sebagai batas mutlak yang stabil.
   * Lakukan *refactoring* kode jika ada fungsi spesifik (seperti pengambilan warna atau UI) yang *deprecated* atau tidak tersedia di API 24-25, gunakan blok `if (Build.VERSION.SDK_INT >= ...)` atau `ContextCompat`.
 * **Hardening Android 12, 13 & 14:**
   * **Manifest:** Pastikan semua `<activity>`, `<service>`, dan `<receiver>` yang memiliki `<intent-filter>` secara eksplisit mencantumkan `android:exported="true"` (atau `false` jika internal).
   * **Foreground Service:** Tambahkan `android:foregroundServiceType="mediaProjection"` pada layanan Screen Capture, dan pastikan permission `FOREGROUND_SERVICE_MEDIA_PROJECTION` ditambahkan untuk Android 14.
   * **PendingIntent:** Cari seluruh inisialisasi `PendingIntent` dan pastikan menggunakan flag `PendingIntent.FLAG_IMMUTABLE` (atau kombinasi dengan `FLAG_UPDATE_CURRENT`).
   * **Notifications:** Tambahkan izin `POST_NOTIFICATIONS` untuk Android 13+.
 * **Integrasi Pengujian ADB di GitHub Actions:**
   * Buat/Perbarui file workflow (misal: `.github/workflows/android-test.yml`).
   * Gunakan action `reactivecircus/android-emulator-runner` untuk menjalankan emulator *headless* di dalam CI.
   * Konfigurasikan skrip untuk menginstal APK debug, lalu jalankan `adb shell monkey -p com.rocat.translator -c android.intent.category.LAUNCHER 500` untuk *stress-test* UI, memastikan tidak ada *crash* yang tak terduga.
 * **Security Linting:**
   * Konfigurasikan blok `lint` di `build.gradle` untuk mendeteksi celah keamanan (`checkReleaseBuilds = true`, `abortOnError = true`).

## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * Aplikasi berhasil di-*compile* dengan `minSdkVersion 24` (atau 26 jika 24 terbukti diblokir oleh ML Kit).
 * Pemeriksaan Android Lint berhasil lulus tanpa memberikan *error fatal* terkait keamanan (`exported`, `PendingIntent`, dll).
 * Workflow GitHub Actions sukses menjalankan *emulator* dan lolos dari *Monkey Test* ADB tanpa log *crash* (NullPointerException, Fatal Signal).
 * Jalankan perintah `./gradlew spotlessApply` untuk merapikan format, lalu verifikasi keberhasilan kompilasi tanpa *error* dengan `./gradlew assembleDebug`.