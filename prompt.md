
**TUGAS FASE 21: PENINGKATAN UI/UX SETTINGS, ANIMASI TRANSISI & CLEANUP FITUR REDUNDAN**
## 1. PROTOKOL SISTEM & MEMORI
 * Mengingat kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless, protokol memori WAJIB dijaga ketat agar token tidak kelebihan beban.
 * **Cek Konteks:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu, dilanjutkan dengan membaca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Perekaman Log:** Setelah selesai, buat log baru bernama task_YYYYMMDD_HHMM_phase21_ui_ux_improvements.md dan pastikan untuk menambahkan referensinya ke dalam file index utama.
 
## 2. ANALISIS KEBUTUHAN & BUG
Fase ini berfokus pada penyempurnaan interaksi pengguna (UX), animasi UI, dan pembersihan kode dari pengaturan yang redundan:
 1. **Kontrol UI yang Bertabrakan:** Saat pengguna mengaktifkan "Sesuaikan Teks & Wrap Otomatis" (Auto Text Fit), slider "Ukuran Teks" masih aktif meskipun nilainya diabaikan sistem. Slider yang tidak relevan harus dinonaktifkan (menjadi abu-abu) secara dinamis agar pengguna tidak bingung.
 2. **Kustomisasi Outline di Mode Transparan:** Pengguna butuh pengaturan tambahan (slider ketebalan & pilihan warna *outline*) yang **hanya muncul** ketika *Transparent Mode* diaktifkan. Kemunculan menu ini harus disertai animasi yang halus, bukan muncul tiba-tiba.
 3. **Redundansi Paragraph Grouping:** Pengaturan "Paragraph Grouping / Margin" secara fungsional tumpang tindih dengan "Vertical Gap" di algoritma *Smart Merge* dan kerap menyebabkan masalah sinkronisasi logika. Fitur "Paragraph Grouping" harus **dihapus sepenuhnya** dari aplikasi untuk menyederhanakan konfigurasi.
 
## 3. OBJEKTIF PENGEMBANGAN (FASE 21)
 * **Cleanup Paragraph Grouping:**
   * **ConfigManager.kt:** Hapus variabel paragraphGroupingMargin beserta getter/setter-nya.
   * **dialog_settings.xml:** Hapus komponen UI <TextView ... android:id="@+id/tvSettingsGrouping"> dan <com.google.android.material.slider.Slider android:id="@+id/sliderSettingsGrouping"...>.
   * **SettingsDialog.kt:** Hapus referensi bindSlider untuk sliderSettingsGrouping.
   * **TranslationEngine.kt:** Di dalam fungsi mergeBlocks, hapus perkalian config.paragraphGroupingMargin dari perhitungan verticalGap / closeVertically. Sisakan hanya config.mergeVerticalGapMultiplier * avgLineHeight.
 * **Penambahan Konfigurasi Outline di ConfigManager.kt:**
   * Tambahkan variabel: outlineThickness (Float, default: 4f, range: 1f - 10f) dan outlineColor (String, default: "#000000" atau warna kontras yang sesuai).
 * **Pembaruan Layout & Animasi di SettingsDialog.kt & XML:**
   * Tambahkan komponen UI baru (misalnya Slider untuk outlineThickness dan UI untuk outlineColor) di dialog_settings.xml tepat di bawah *switch* "Transparent Mode", set visibility awal ke gone.
   * **Logika Dinamis (Auto Text Fit):** Jika *switch* "Auto Text Fit" bernilai true, set sliderSettingsTextSize.isEnabled = false. Jika false, set isEnabled = true. Terapkan juga saat dialog diinisialisasi.
   * **Logika Dinamis & Animasi (Transparent Mode):** Saat *switch* "Transparent Mode" di-klik, gunakan android.transition.TransitionManager.beginDelayedTransition(findViewById(R.id.settingsDialogRoot)) sebelum mengubah *visibility* pengaturan *Outline* (menjadi VISIBLE atau GONE). Ini akan memberikan efek animasi *expand/collapse* yang halus.
 * **Implementasi Visual Outline di OverlayManager.kt:**
   * Baca config.outlineThickness dan config.outlineColor.
   * Saat menggambar teks di mode transparan (style STROKE), gunakan nilai ketebalan dan warna dari *ConfigManager* tersebut.
   
## 4. KRITERIA VERIFIKASI & PENGUJIAN
 * Pastikan UI "Paragraph Grouping" hilang dari menu pengaturan dan algoritma *merge* tetap berjalan normal menggunakan *Vertical Gap* saja.
 * Aktifkan "Auto Text Fit", pastikan slider "Ukuran Teks" seketika menjadi abu-abu (*disabled*).
 * Aktifkan "Transparent Mode", pastikan menu pengaturan *Outline* muncul dengan transisi animasi yang mulus (menggeser elemen di bawahnya secara halus).
 * Ubah nilai ketebalan *Outline*, jalankan terjemahan, dan pastikan tebal garis luar teks berubah sesuai pengaturan baru.
 * Jalankan perintah ./gradlew spotlessApply untuk merapikan format, lalu verifikasi keberhasilan kompilasi tanpa *error* dengan ./gradlew assembleDebug.