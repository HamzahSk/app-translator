# Pembaruan Fitur dan Optimasi Performa Aplikasi Android

Kamu bertugas sebagai Senior Android Developer. Tolong bantu saya memperbaiki isu performa dan menambahkan beberapa fitur baru pada aplikasi Screen Translator saya. Aplikasi ini dibangun dengan bahasa Kotlin, Material Design 3, dan memanfaatkan Google Play Services[span_2](start_span)[span_2](end_span)[span_3](start_span)[span_3](end_span).

Berikut adalah daftar tugas yang perlu kamu kerjakan:

## 1. Optimasi Loading / Splash Screen
* Saat ini aplikasi memiliki *animated splash screen* pada saat peluncuran[span_4](start_span)[span_4](end_span). Tolong ubah implementasinya menjadi berbasis SVG (VectorDrawable).
* Tujuannya agar aplikasi menjadi lebih ringan dan waktu *startup* lebih cepat. Berikan contoh kode implementasinya.

## 2. Perbaikan Isu Delay (Performance Optimization)
* Aplikasi terasa berat dan mengalami delay, kemungkinan karena memuat semua konten/model sekaligus.
* Tolong berikan solusi dan kode untuk menerapkan *lazy loading* atau asinkronisasi. 
* Pastikan inisialisasi berat (seperti klien Google Play Services atau ML Kit[span_5](start_span)[span_5](end_span)) dipindahkan ke *background thread* menggunakan Kotlin Coroutines.

## 3. Penambahan Mode Online (Custom AI API)
* Aplikasi saat ini bekerja 100% *offline* menggunakan model *on-device*[span_6](start_span)[span_6](end_span). Saya ingin menambahkan mode "Online".
* Buatkan implementasi *network client* (misalnya dengan Retrofit atau Ktor) yang mendukung integrasi API pihak ketiga.
* Fitur ini harus secara khusus mendukung format *endpoint* dan *payload* dari **OpenAI API** dan **Gemini API**.

## 4. Konfigurasi Split APK di Gradle
* Proyek ini menggunakan Gradle Kotlin DSL (`build.gradle.kts`) dengan `targetSdk = 34`[span_7](start_span)[span_7](end_span).
* Tolong berikan konfigurasi blok `splits` untuk menghasilkan Split APK berdasarkan arsitektur CPU (ABI).
* Buatkan konfigurasi agar Gradle menghasilkan APK `universal`, `arm64-v8a`, `armeabi-v7a`, dan arsitektur lain yang relevan.
