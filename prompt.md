
**TUGAS FASE 11: FIX ONLINE BATCH PARSING (JSON) & UI TOGGLE BUG**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase11_batch_and_ui.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PERBAIKAN BUG KODE
Kamu bertugas memperbaiki *bug* fatal dari Fase 10.5. Tombol *toggle* "Offline/Online" ikut menghilang saat mode Offline ditekan, dan terjemahan mode Online gagal di-*render* karena validasi pemisahan (*split delimiter*) yang terlalu ketat saat AI membalas dengan format yang tidak terduga.
## 1. Perbaikan Bug UI Layout Toggle (MainActivity & XML)
 * **Masalah:** ID layoutOnlineConfig di activity_main.xml ditempatkan pada LinearLayout terluar yang juga membungkus MaterialButtonToggleGroup. Akibatnya, saat mode *Offline* dipilih, tombol *toggle*-nya ikut tersembunyi (*View.GONE*).
 * **Solusi:**
   * Buka activity_main.xml. Pindahkan penempatan ID @+id/layoutOnlineConfig ke dalam sebuah LinearLayout baru (berada di bawah tvOnlineHint) yang *HANYA* membungkus elemen konfigurasi khusus Online (spinnerApiProvider, editApiKey, editApiBaseUrl, dan editApiModel).
   * Pastikan modeToggleGroup dan tvOnlineHint berada di luar layoutOnlineConfig agar tetap selalu terlihat.
## 2. Rombak Sistem Online Batch Translation (Gunakan JSON)
 * **Masalah:** ChatGPT atau *provider Default* sering merespons dengan tambahan kata (basa-basi) atau merusak *delimiter* mentah, sehingga kode takeIf { it.size == texts.size } di OnlineTranslator.kt gagal divalidasi dan mengembalikan nilai *null* secara diam-diam.
 * **Solusi (JSON Array Enforcement):**
   * Buka OnlineTranslator.kt. Ubah instruksi *prompt* pada fungsi translateBatch. Instruksikan *System Prompt* dengan sangat tegas agar AI **HANYA** mengembalikan struktur data **JSON Array of Strings** (misalnya: ["translasi 1", "translasi 2"]) tanpa *markdown* atau teks tambahan apa pun.
   * Buang metode lama yang menggunakan *delimiter* dan fungsi split().
   * Buat logika *parsing*: cari indeks karakter [ pertama dan ] terakhir pada *string* respons, lalu ekstrak dan *parse* isinya menggunakan org.json.JSONArray.
   * Ubah hasil *JSONArray* tersebut kembali menjadi List<String>. Jika ternyata jumlah *array* tidak sama dengan jumlah teks *input*, jangan langsung *return null*—lakukan pemetaan sebisa mungkin (gunakan *fallback* atau biarkan sisa kotak tidak terjemahkan).
## 3. Verifikasi & Build
 * Pastikan DefaultScraperTranslator.kt dari fase sebelumnya tidak bermasalah.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi.