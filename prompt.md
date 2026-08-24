
**TUGAS FASE 21: PENINGKATAN UI/UX SETTINGS, ANIMASI TRANSISI & CLEANUP FITUR REDUNDAN**
## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat.
 * **Cek Konteks:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu, dilanjutkan membaca maksimal 2 file log terbaru.
 * **Perekaman Log:** Setelah selesai, buat log task_YYYYMMDD_HHMM_phase21_ui_ux_improvements.md dan tambahkan referensinya ke file index.
 
## 2. ANALISIS KEBUTUHAN & BUG
Fase ini berfokus pada UX dinamis dan pembersihan kode:
 1. **Kontrol UI Bertabrakan:** Saat "Auto Text Fit" aktif, slider "Ukuran Teks" harus dinonaktifkan (isEnabled = false) agar pengguna tidak bingung.
 2. **Kustomisasi Outline Dinamis:** Pengguna butuh slider ketebalan & input warna *outline* yang **hanya muncul** jika *Transparent Mode* diaktifkan, disertai animasi transisi *expand/collapse* yang halus.
 3. **Redundansi Paragraph Grouping:** Fitur "Paragraph Grouping" tumpang tindih dengan algoritma algoritma *Smart Merge* di TranslationEngine.kt. Fitur ini harus dihapus sepenuhnya.
 
## 3. OBJEKTIF PENGEMBANGAN (FASE 21)
 * **Cleanup Paragraph Grouping:**
   * **ConfigManager.kt:** Hapus variabel paragraphGroupingMargin beserta getter/setter-nya.
   * **dialog_settings.xml:** Hapus <TextView ... android:id="@+id/tvSettingsGrouping"> dan <Slider ... android:id="@+id/sliderSettingsGrouping">.
   * **SettingsDialog.kt:** Hapus pemanggilan bindSlider untuk sliderSettingsGrouping.
   * **TranslationEngine.kt:** Di fungsi mergeBlocks, ubah deklarasi closeVertically dengan menghapus perkalian config.paragraphGroupingMargin. Sisakan hanya config.mergeVerticalGapMultiplier * avgLineHeight.
 * **Penambahan Konfigurasi Outline di ConfigManager.kt:**
   * Tambahkan: outlineThickness (Float, default: 4f, range: 1f - 10f) dan outlineColor (String, default: "#000000").
 * **Pembaruan Layout & Animasi (SettingsDialog.kt & XML):**
   * Di dialog_settings.xml, bungkus pengaturan Outline baru (Slider ketebalan & komponen input warna) ke dalam sebuah <LinearLayout ... android:id="@+id/layoutOutlineConfig" android:visibility="gone"> tepat di bawah switch *Transparent Mode*.
   * **Logika Auto Text Fit:** Di SettingsDialog.kt, set sliderSettingsTextSize.isEnabled = !config.autoTextFitEnabled saat inisialisasi dan di dalam setOnCheckedChangeListener.
   * **Logika Animasi Transparent Mode:** Saat switch "Transparent Mode" diklik, gunakan android.transition.TransitionManager.beginDelayedTransition(findViewById<ViewGroup>(R.id.settingsDialogRoot)) sebelum mengubah *visibility* layoutOutlineConfig menjadi VISIBLE atau GONE. Set juga *visibility* awal berdasarkan config.isTransparentModeEnabled.
 * **Implementasi Visual Outline (OverlayManager.kt):**
   * Ganti logika *hardcode* outline dari Fase 20.
   * Saat menggambar teks di mode transparan (textPaint.style = Paint.Style.STROKE), ubah strokeWidth menggunakan nilai konversi dpToPx(config.outlineThickness.toInt()) dan hapus logika warna kontras otomatis (Color.luminance), ganti dengan Color.parseColor(config.outlineColor).
   
## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * UI "Paragraph Grouping" hilang dan kompilasi tidak *error* karena referensi variabel yang tertinggal.
 * Aktifkan "Auto Text Fit", pastikan slider "Ukuran Teks" seketika menjadi abu-abu (*disabled*).
 * Aktifkan "Transparent Mode", pastikan menu *Outline* muncul mulus (animasi).
 * Jalankan perintah ./gradlew spotlessApply lalu verifikasi kompilasi dengan ./gradlew assembleDebug.
 