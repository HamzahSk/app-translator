**TUGAS FASE 13.5: PERBAIKAN TEMA POPUP, STATE BUTTON, & REFACTOR STRING XML**

# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file `ai_memory/00_INDEX.md` terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder `ai_memory/`.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log `task_YYYYMMDD_HHMM_phase13_5_ui_string_fixes.md`.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase 13.5 ini, fokus utama adalah memperbaiki isu UI pada Settings Popup (terutama di Dark Mode) dan merombak sistem string i18n sesuai dengan spesifikasi baru. Kerjakan poin-poin berikut:

## 1. Perbaikan Tema Settings Popup (Dark/Light Mode)
 * **Masalah:** Saat ini background popup menggunakan warna *hardcoded* putih (`@android:color/white`) di `bg_settings_dialog.xml`. Akibatnya, pada Dark Mode, teks dan elemen UI (yang otomatis menjadi terang) menjadi tidak terbaca (putih di atas putih).
 * **Solusi:** 
   * Ubah warna background pada `bg_settings_dialog.xml` menjadi atribut yang beradaptasi dengan tema, seperti `?android:attr/colorBackground` atau `?attr/colorSurface`.
   * Pastikan teks label dan slider memiliki visibilitas yang baik pada mode gelap maupun terang.

## 2. Visual State Segmented Button (Direct / Left / Right)
 * **Masalah:** Tombol *Segmented Button* untuk pengaturan penempatan (Direct, Left, Right) tidak membedakan secara visual antara tombol yang sedang aktif (*selected*) dan yang tidak aktif (*unselected*). Semua tombol terlihat sama atau warnanya berbenturan.
 * **Solusi:**
   * Terapkan styling yang benar pada `MaterialButtonToggleGroup`. Gunakan `style="?attr/materialButtonOutlinedStyle"` pada setiap tombolnya, atau pastikan state *checked* memiliki warna *background* dan teks yang kontras (misal: warna *Primary* untuk tombol aktif, dan transparan/abu-abu untuk tombol non-aktif).
   * Pastikan indikator *selected* ini langsung terlihat jelas begitu popup dibuka.

## 3. Refactor Lokalisasi String (XML di Root Directory)
 * **Masalah:** File string saat ini berformat JSON, disimpan di dalam folder `app/`, dan isinya masih kurang lengkap (banyak teks *hardcoded* di UI yang belum diekstrak).
 * **Solusi:**
   * **Pindahkan Direktori:** Buat sistem penyimpanan *string* di **root directory** dari repositori (sejajar dengan folder `app/`, misalnya di `<root>/i18n/en/strings.xml` dan `<root>/i18n/id/strings.xml`). 
   * **Ubah Format ke XML:** Gunakan format XML (struktur standar `<resources><string name="key">Value</string></resources>`) alih-alih JSON.
   * **Lengkapi String:** Ekstrak **seluruh** teks yang ada di dalam aplikasi (MainActivity, SettingsDialog, prompt AI, Overlay, tombol-tombol, dll) ke dalam file XML tersebut. Tidak boleh ada sisa *hardcoded string* berbahasa Inggris/Indonesia di dalam kode Kotlin atau layout XML yang ditujukan untuk dibaca pengguna.
   * **Build Integration & Parsing:** Karena file berada di luar folder `app/`, kamu perlu menambahkan script di `app/build.gradle.kts` (atau sejenisnya) untuk otomatis menyalin folder `<root>/i18n/` ke `app/src/main/assets/i18n/` setiap kali proses build berjalan.
   * **Perbarui I18nManager:** Modifikasi `I18nManager.kt` agar membaca dan mem-parsing file XML tersebut menggunakan `XmlPullParser` atau pustaka XML Android standar yang ringan.

## 4. Verifikasi & Build
 * Pastikan UI terlihat normal di mode Dark dan Light.
 * Pastikan tidak ada string yang terlewat.
 * Jalankan `./gradlew :app:compileDebugKotlin` atau `./gradlew assembleDebug` untuk memastikan integrasi file *asset* XML dari root berjalan lancar dan kode berhasil dikompilasi tanpa error.