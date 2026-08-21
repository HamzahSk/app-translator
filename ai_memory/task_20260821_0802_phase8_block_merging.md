# Task Log: Phase 8 Smart OCR Block Merging (Size & Proximity Aware)

## Status
Selesai; `./gradlew :app:compileDebugKotlin` dan `./gradlew :app:spotlessCheck` BUILD SUCCESSFUL.

## Ringkasan Perubahan
- `TranslationEngine.kt`:
  - Tambah `data class MergedBlock(val text: String, val rect: Rect, val lineHeight: Float)` di top-level. `lineHeight` disimpan supaya heuristic komparasi ukuran tidak dihitung ulang tiap iterasi.
  - Tambah `private fun mergeBlocks(blocks: List<Text.TextBlock>): List<MergedBlock>`.
    - Filter fragmen kosong/blank/null box.
    - Hitung `lineHeight = rect.height() / max(1, lines.size)`.
    - Sort top-down (top, lalu left).
    - Greedy linear pass: dua blok hanya digabung jika SEMUA syarat terpenuhi:
      1. `verticalGap >= 0` dan `< 1.5 * max(lineHeight)` (constant `MERGE_VERTICAL_GAP_MULTIPLIER`).
      2. `horizontalGap < 0.25 * max(widthA, widthB)` (constant `MERGE_HORIZONTAL_GAP_RATIO`) — overlap dihitung sebagai gap 0.
      3. `|lineHeightA - lineHeightB| <= 0.30 * max(lineHeightA, lineHeightB)` (constant `MERGE_SIZE_TOLERANCE`) — inilah anti-overlap yang memisahkan dialog kecil dari SFX besar.
    - Saat gabung: `text = a.text + "\n" + b.text`, `rect = a.rect ∪ b.rect`, `lineHeight = max(a.lineHeight, b.lineHeight)` (jendela merge berikutnya tetap murah-hati).
  - `translateBlocks(visionText, sourceLangCode)`: pemrosesan awal diubah menjadi `val mergedBlocks = mergeBlocks(visionText.textBlocks)`. Iterasi `drawLoadingBubble`, `translate`, `drawTranslationBatch`, dan `removeLoading` memakai `mergedBlocks`. Offset status bar tetap diaplikasikan lewat `adjustedBoundingBox(block.rect)` yang sudah ada.
  - `onlineTranslate(visionText)`: filter & drawLoadingBubble, `translateBatch`, `replaceLoading`, dan `removeLoading` memakai `mergedBlocks`. Teks yang dikirim ke API sudah digabung (dengan `\n`), API cukup membalas satu segmen per merged bubble.
  - Companion object ditambah tiga konstanta tuning merge (`MERGE_VERTICAL_GAP_MULTIPLIER = 1.5f`, `MERGE_HORIZONTAL_GAP_RATIO = 0.25f`, `MERGE_SIZE_TOLERANCE = 0.30f`).
- Struktur coroutine dan timeout Fase 7 (`scope`, `preloadScope`, `mlKitCall`, `withTimeout(ML_KIT_TIMEOUT_MS)`, `ensureActive`, `CancellationException`) tidak diubah. State machine tombol Start di `MainActivity.kt` dan render batch di `OverlayManager.kt` tidak disentuh.

## Verifikasi
- `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL (hanya warning deprecated `scaledDensity` di `OverlayManager.kt:66` yang sudah ada sejak Fase 6).
- `./gradlew :app:spotlessCheck` BUILD SUCCESSFUL setelah `spotlessApply` menambahkan satu blank line kosong di companion object.

## Catatan Algoritma
- Sorting pakai `compareBy<MergedBlock> { it.rect.top }.thenBy { it.rect.left }` — menjaga urutan baca natural untuk bubble multi-baris.
- `Rect.union(other)` adalah API Android yang sudah tersedia; tidak perlu implementasi ulang.
- `mergedBlocks.indices` dipakai untuk `removeLoading` agar key konsisten antara draw dan cleanup pada kedua jalur (offline & online).
- SFX besar dan dialog kecil di posisi bersebelahan akan TETAP TERPISAH karena syarat #3 (size tolerance) gagal; tidak ada kasus "bubble numpuk".

## Next Steps
- Uji manual di perangkat: manga page dengan dialog multi-baris + SFX besar — pastikan satu bubble cuma muncul satu overlay.
- Uji dua bubble dalam kolom berbeda — pastikan tidak ikut gabung.
