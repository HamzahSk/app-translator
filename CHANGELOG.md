# Changelog
All notable changes to this project will be documented in this file.

## [v1.0.7] - 2026-08-20
### Added
- Mode "Online" untuk terjemahan menggunakan Custom AI API (OpenAI & Google Gemini) via Retrofit/OkHttp. Mendukung endpoint dan payload format OpenAI Chat Completions (`/chat/completions`) dan Gemini `generateContent` (`/v1beta/models/{model}:generateContent`), termasuk custom base URL untuk endpoint yang kompatibel (OpenRouter, Groq, Ollama, dll).
- Konfigurasi Split APK berbasis ABI di Gradle: menghasilkan APK `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, dan `universal`.

### Changed
- Splash screen diganti dari video MP4 menjadi **VectorDrawable** + AnimatedVectorDrawable (SVG-style) sehingga APK lebih ringan dan startup lebih cepat. Dukungan Android 12+ SplashScreen API ditambahkan di `values-v31`.
- Inisialisasi berat (ML Kit Language Identification, recognizer, translator, dan network client) kini **lazy** dan dipindahkan ke background thread menggunakan **Kotlin Coroutines** (`Dispatchers.IO`/`Default`).
- Proses screen capture & bitmap conversion dipindahkan ke background thread.

## [v1.0.6] - 2026-07-08
### Fixed
- Memperbaiki bug Screen Capture yang tidak bekerja di v1.0.5 (bubbles langsung terhapus saat baru muncul karena event overlay memicu pembersihan).
- Memperbaiki bug download AI Model untuk Japanese/Korean dan lainnya (menambahkan `android.permission.INTERNET` di AndroidManifest).
- Menambahkan anti-double click pada tombol FAB Start Service agar dialog Screen Capture stabil muncul di Android 14/15.

## [v1.0.5] - 2026-07-08
### Added
- FAB Stop button di Main UI bisa di-klik untuk stop service langsung dari aplikasi.
- Translation models sekarang bisa didownload dengan manual di UI AI Models beserta indikator Notifikasi.
- Terjemahan (bubble) akan terhapus otomatis seketika ketika ada aktivitas scroll di layar.

### Changed
- Menu AI Models kini mengecek & mengunduh model **Translation (NLP)**, bukan model OCR (karena OCR otomatis di-download Google Play Services).

## [v1.0.4] - 2026-07-08
### Fixed
- Fix crash (IllegalStateException) saat start service di Android 14 dan Android 15.
- Menambahkan registrasi `MediaProjection.Callback` yang diwajibkan OS terbaru sebelum memulai screen capture.

## [v1.0.3] - 2026-07-08
### Added
- "Stop" action button di Foreground Notification untuk mematikan service langsung dari notifikasi.
### Changed
- Notifikasi di set menjadi `ongoing` (tidak bisa di-swipe away saat service berjalan).
- MainActivity UI sinkron otomatis ketika service di-stop dari notifikasi.

## [v1.0.2] - 2026-07-08
### Fixed
- Crash/mental keluar saat menekan Start Service di Android 14 karena SecurityException pada MediaProjection service yang membutuhkan type dan token valid sebelum pemanggilan startForeground().

## [v1.0.1] - 2026-07-08
### Fixed
- AccessibilityService malfunctioning (lateinit crash dan event flooding)

## [v1.0.0] - 2026-07-08
### Added
- Initial Release
