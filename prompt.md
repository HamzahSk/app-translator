# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM

Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:

## 1. Protokol Membaca & Menulis Konteks
*   **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
*   **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/` bila butuh detail tambahan.
*   **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_[nama_task].md` (maksimal 200 kata).
*   **Perbarui `00_INDEX.md`:** Tambahkan referensi log baru ke dalam file index utama dan perbarui status proyek.

---

# TUGAS FASE 5: BATCH UI RENDERING, SCREENSHOT CRASH FIX, & CUSTOM FONT

Kamu bertugas sebagai Senior Android Developer. Aplikasi mengalami beberapa bug visual terkait UI Overlay (menumpuk, dimuat satu-satu, crash saat screenshot) dan membutuhkan implementasi font kustom. Tolong perbaiki masalah berikut:

## 1. Batch Overlay Rendering & Overlap Fix
*   **Masalah:** Saat ini `TranslationEngine` memanggil `overlayManager.drawTranslationBubble` secara berulang dalam *loop*. Ini membuat *canvas* muncul satu per satu, dan memicu *race condition* yang membuat *bubble* menumpuk jika layar berubah cepat.
*   **Solusi:** 
    *   Ubah pendekatan `OverlayManager` agar **mengumpulkan semua data** (*translated text* dan *bounding box*) terlebih dahulu, lalu menggambarnya **secara serentak** (bisa dengan membuat satu *Custom View* transparan *fullscreen* yang menggambar banyak *bubble* menggunakan `Canvas`, atau memastikan penambahan `WindowManager.addView` divalidasi dengan satu `batchId` unik).
    *   Pastikan jika `batchId` berubah (karena ada *request* baru), semua *view* dari *batch* sebelumnya langsung dihancurkan untuk mencegah penumpukan.

## 2. Instant Clear & Screenshot Bug Fix
*   **Masalah:** Mengambil *screenshot* menyebabkan aplikasi *crash* (kemungkinan karena `IllegalArgumentException` dari `WindowManager.removeView`). Selain itu, *scroll* tidak langsung membersihkan layar dengan bersih.
*   **Solusi:**
    *   Pastikan `clearOverlays()` dipanggil **seketika itu juga** saat `InactivityAccessibilityService` mendeteksi `TYPE_VIEW_SCROLLED`.
    *   Bungkus perintah `windowManager.removeView(view)` di dalam blok `try-catch` yang spesifik menangani `IllegalArgumentException` (terjadi jika *view* sudah tidak *attached* ke *window*, seperti saat *screenshot* diambil).

## 3. Implementasi Custom Font (.ttf)
*   **Masalah:** Aplikasi masih menggunakan *font system default*. Ada file `.ttf` di *root directory*.
*   **Solusi:**
    *   Arahkan skrip Gradle atau asumsikan file `.ttf` tersebut dipindahkan ke direktori `app/src/main/assets/fonts/` (misalnya bernama `comic_font.ttf`).
    *   Muat *font* tersebut menggunakan `Typeface.createFromAsset(context.assets, "fonts/comic_font.ttf")` di dalam `OverlayManager` dan terapkan ke `TextView` atau `Canvas` menggunakan `setTypeface()`.
