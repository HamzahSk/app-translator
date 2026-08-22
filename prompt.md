
**TUGAS FASE 12: UI/UX REVAMP (SETTINGS POPUP) & AI PROMPT CONTEXT OPTIMIZATION**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase12_ui_prompt_revamp.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PENGEMBANGAN FITUR & PERBAIKAN
Pada fase ini, kita akan merapikan UI agar layar beranda lebih bersih dan mengoptimalkan *prompt* AI agar terjemahan lebih natural. Kerjakan poin-poin berikut:
## 1. Optimalisasi AI Prompt (Konteks Manga/Manhwa)
 * **Masalah:** Saat ini, *prompt* di OnlineTranslator.kt terlalu kaku ("You are a precise manga translation engine..."), sehingga hasil terjemahan kadang kurang natural.
 * **Solusi:**
   * Buka file OnlineTranslator.kt.
   * Ubah variabel prompt di dalam fungsi translate menjadi lebih berkonteks, misalnya: *"Kamu adalah penerjemah profesional untuk Manga dan Manhwa. Terjemahkan teks berikut ke dalam bahasa $targetLangName. Gunakan gaya bahasa yang natural, santai, dan sesuai dengan percakapan komik sehari-hari. JANGAN menambahkan penjelasan, catatan, atau tanda kutip. Teks sumber: $text"*.
   * Lakukan penyesuaian konteks yang sama pada *prompt* di fungsi translateBatch, pastikan AI tetap mengembalikan format JSON Array yang valid namun dengan bahasa yang tidak kaku.
## 2. Pembuatan Settings Dialog/Popup
 * **Masalah:** Halaman utama terlalu penuh dengan pengaturan *Overlay Customization* (Bubble Placement, Opacity, Corner Radius, dll).
 * **Solusi:**
   * Hapus komponen UI *Overlay Customization* dan *Bubble Color* dari tampilan utama (activity_main.xml atau *layout* beranda terkait).
   * Tambahkan sebuah tombol *Settings* (ikon *gear*) di sudut layar utama atau di dekat judul aplikasi.
   * Buat sebuah *Custom Dialog* atau *BottomSheet* (misal: SettingsBottomSheet.kt atau SettingsDialog.kt) yang akan muncul saat tombol *Settings* ditekan.
   * Pindahkan semua pengaturan slider (Opacity, Radius, Text Size, Delay, Auto-Clear) dan pengaturan *Floating Ball* ke dalam Dialog/Popup ini.
## 3. Penyesuaian Nilai Kelipatan pada Slider (Customization)
 * **Masalah:** Nilai slider saat ini tidak konsisten atau terlalu detail.
 * **Solusi:** Di dalam logika UI Settings yang baru, sesuaikan *step* atau kelipatan untuk masing-masing pengaturan:
   * **Inactivity Delay:** Sesuaikan menjadi kelipatan **0.5s** (misal: 0.5s, 1.0s, 1.5s, dst).
   * **Bubble Opacity:** Sesuaikan menjadi kelipatan **5%** (misal: 85%, 90%, 95%, 100%).
   * **Lainnya:** Pastikan UI merespons perubahan slider dengan kelipatan tersebut secara *real-time* dan menyimpannya ke ConfigManager.
## 4. Pembersihan Home Screen
 * **Masalah:** Tampilan *Home* harus lebih berfokus pada fungsi utama.
 * **Solusi:** Pastikan layar utama hanya menyisakan menu esensial: *Appearance* (Light/Dark/System), *Translation Settings* (Source/Target Language), *AI Translation Mode* (Offline/Online), dan tombol *Start Service*.
## 5. Verifikasi & Build
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi. Pastikan aplikasi tidak *crash* saat Settings Popup dibuka.
