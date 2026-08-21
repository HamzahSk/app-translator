# Task Log: Phase 8 - OCR Block Merging & Anti-Overlap Bubble

**Tanggal:** 2026-08-20 23:00
**File:** TranslationEngine.kt

## Ringkasan
Mengatasi fragmentasi OCR dari ML Kit yang memecah satu gelembung percakapan menjadi banyak TextBlock kecil, menyebabkan bubble terjemahan saling menimpa.

## Perubahan
1. **Data Class `MergedBlock`**: Menambahkan `private data class MergedBlock(val text: String, val boundingBox: Rect)` untuk menampung hasil penggabungan.
2. **Fungsi `mergeBlocks`**: Mengimplementasikan algoritma spatial merging. Blok diurutkan berdasarkan Y (atas-bawah) lalu X. Jika jarak vertikal (`next.top - curr.bottom`) atau jarak horizontal < `1.5 * avgHeight`, teks digabung (dipisah newline) dan bounding box diperluas via `Rect.union`.
3. **Refactor `translateBlocks` & `onlineTranslate`**: Mengubah parameter dari `Text` menjadi `List<MergedBlock>`. Iterasi sekarang dilakukan pada `MergedBlock` untuk loading bubble, translasi, dan rendering akhir.
4. **`identifyAndTranslate`**: Memanggil `mergeBlocks(visionText.textBlocks)` sebelum mendistribusikan ke jalur offline/online.

## Verifikasi
- Fitur Fase 7 (state machine, preload lock-free, loading bubble render) tetap utuh.
- `./gradlew :app:compileDebugKotlin` lolos tanpa error kompilasi.

## Catatan
- Threshold jarak penggabungan (`1.5x avgHeight`) bersifat sementara; mungkin perlu di-expose sebagai slider pengaturan nanti.
