# Task Log: Phase 2.5 Global Crash Handling

## Status
Implementasi selesai; debug build dan Spotless sukses. Release R8 terhenti karena daemon Gradle kehabisan heap 512 MiB.

## Ringkasan Perubahan
- Memastikan operasi overlay dijadwalkan di Main Looper, memvalidasi Rect, dan menangkap kegagalan WindowManager.
- Memperketat parsing batch translation terhadap null, mismatch jumlah segmen, NPE, dan IndexOutOfBoundsException; loading bubble dibersihkan pada semua jalur gagal.
- Menambahkan GlobalExceptionHandler, logging stack trace + timestamp + device ke getExternalFilesDir(null), dan penjadwalan CrashActivity.
- Menambahkan CrashActivity dengan tampilan stack trace, Salin Error, dan Bagikan; mendaftarkan Application/Activity di manifest.

## Tugas Selanjutnya (Next Steps)
- Jalankan release build dengan heap Gradle lebih besar pada CI/perangkat build.
- Uji runtime crash flow dan akses file laporan pada perangkat Android nyata.
