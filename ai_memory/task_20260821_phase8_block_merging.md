# Task Log: Phase 8 OCR Block Merging & Anti-Overlap Bubble

## Status
Selesai; `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL (hanya warning
`scaledDensity` yang sudah ada sejak Fase 7).

## Ringkasan Perubahan (`TranslationEngine.kt`)
- **Data class baru**: `MergedBlock(val text: String, val boundingBox: Rect)`
  merepresentasikan satu bubble percakapan hasil penggabungan spasial
  TextBlock ML Kit.
- **Fungsi utilitas baru** `mergeBlocks(blocks: List<Text.TextBlock>): List<MergedBlock>`
  melakukan 4 langkah:
  1. Buang TextBlock dengan rect kosong / teks kosong lewat
     `adjustedBoundingBox` (status bar sudah terfilter otomatis).
  2. Hitung tinggi rata-rata blok (`avgHeight`) sebagai proksi line-height.
  3. Sortir reading-order (atas-bawah, lalu kiri-kanan dalam satu baris).
  4. Greedy merge: sebuah kandidat digabung ke blok sebelumnya bila
     `verticalGap in 0..gapThreshold` (di mana
     `gapThreshold = 1.5 * avgHeight`), ATAU rect overlap vertikal &
     horizontal-nya sangat dekat (≤ `gapThreshold`). Bound box hasil =
     `Rect.union`, teks digabung dengan newline (bila blok kandidat
     dimulai di paruh-bawah rect sebelumnya) atau spasi. Konstanta
     `MERGE_GAP_FACTOR = 1.5` disimpan di companion object agar mudah
     di-tune (atau dijadikan slider di iterasi berikutnya).
- **`identifyAndTranslate(visionText: Text)`** sekarang memanggil
  `mergeBlocks(visionText.textBlocks)` tepat setelah language ID sukses;
  list `MergedBlock` diteruskan ke `onlineTranslate` atau `translateBlocks`.
  Tidak ada perubahan alur utama (loading bubble → translasi → batch draw);
  hanya struktur input yang bergeser dari `Text`/`TextBlock` menjadi
  `List<MergedBlock>`.
- **`translateBlocks(blocks: List<MergedBlock>, sourceLangCode)`**
  refactor total: iterasi `MergedBlock` untuk menggambar loading bubble
  (`offline_$index`), memanggil `translator.translate(merged.text)`,
  dan menyusun `OverlayManager.Bubble(translated, rect)` dari rect
  merged (bukan rect ML Kit mentah). Path offline mempertahankan
  timeout ML Kit 7 dtk + dummy preload dari Fase 6/7.
- **`onlineTranslate(blocks: List<MergedBlock>)`** refactor paralel:
  iterasi `MergedBlock` untuk loading bubble + `replaceLoading()` dengan
  rect merged; payload batch dikirim sebagai `visible.map { it.text }`.
  Delimiter `<<<SCREEN_TRANSLATOR_SEGMENT>>>` tetap dipakai agar jumlah
  segmen dari API konsisten.

## Verifikasi
- `./gradlew :app:compileDebugKotlin` BUILD SUCCESSFUL dalam 9 detik.
- Tidak ada error kompilasi; satu warning `scaledDensity` adalah warisan
  Fase 7 dan tidak terkait perubahan ini.
- Tidak ada perubahan di `OverlayManager.kt` (Bubble API & layout sudah
  kompatibel dengan rect union hasil merge).

## Catatan / Next Steps
- Threshold `MERGE_GAP_FACTOR = 1.5` saat ini konstan di companion object.
  Bisa dipromosikan menjadi konfigurasi user (slider di layar Settings)
  bila uji lapangan menunjukkan terlalu banyak/gabung yang masih
  overlap. Saat ini dijaga sebagai `const val` agar perubahan tuning
  satu tempat dan konsisten dengan helper `gapThreshold`.
- Perlu uji manual di device pada layar chat (line/whatsapp/telegram
  style) untuk mengonfirmasi tidak ada bubble yang kepotong atau
  bertumpuk lagi.
