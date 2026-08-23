
**TUGAS FASE 18: REKAYASA ULANG OCR & OPTIMASI MANAJEMEN MEMORI**
## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat agar token tidak kelebihan beban.
 * **Cek Konteks:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu, dilanjutkan dengan membaca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Perekaman Log:** Setelah selesai, buat log baru bernama task_YYYYMMDD_HHMM_phase18_ocr_refactor.md dan pastikan untuk menambahkan referensinya ke dalam file index utama.
 
## 2. ANALISIS MASALAH KINERJA
Sistem OCR saat ini mengalami kegagalan struktural pada siklus pemrosesannya. Masalah utama meliputi: *delay* parah, proses menggantung (*stuck*), *thread* yang terblokir (membutuhkan *app-switching* untuk *resume*), dan absennya sistem interupsi. Ini mengindikasikan adanya manajemen *thread* yang buruk dan *memory leak* akibat proses *background* yang terus berjalan. Tugasmu adalah menganalisis akar masalah ini secara kritis dan mendesain ulang arsitektur pemrosesan tersebut. Temukan letak *bottleneck*—apakah pada manajemen konkurensi, pengolahan *Bitmap*, atau siklus hidup *library* OCR itu sendiri.

## 3. OBJEKTIF PENGEMBANGAN (FASE 18)
 * Rancang ulang algoritma OCR dari nol dengan arsitektur asinkron yang sangat responsif, pastikan *Main Thread* tidak pernah terblokir sedikit pun.
 * Ciptakan mekanisme pemantauan *event* sentuhan layar (*touch listener*) yang beroperasi secara independen dan reaktif.
 * Implementasikan sistem **Pembatalan Absolut (*Immediate Cancellation*)**: saat layar disentuh, proses OCR yang sedang berjalan harus digugurkan secara instan di tingkat *thread/coroutine*.
 * Bangun lapisan **Pembersihan Memori Agresif**: pastikan blok pelepasan sumber daya langsung menghancurkan (*recycle/release*) semua objek *Bitmap* dan *instance OCR scanner* sesaat setelah proses dibatalkan untuk membebaskan RAM.
 * Rancang peralihan UI (*State Management*) agar langsung kembali ke fase *idle* dalam hitungan milidetik pasca-pembatalan, tanpa *loading* tambahan.
 
## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * Lakukan uji stres (*stress test*) simulasi pembatalan beruntun untuk membuktikan RAM tetap stabil dan proses terhenti sempurna.
 * Jalankan perintah ./gradlew spotlessApply untuk merapikan format, lalu verifikasi keberhasilan kompilasi tanpa *error* dengan ./gradlew assembleDebug.
