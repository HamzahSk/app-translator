
**TUGAS FASE 10.5: BUG FIX UI OFFLINE, TOGGLE STATE, & INTEGRASI NODE.JS SCRAPER**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase10.5_ui_scraper_fix.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PERBAIKAN BUG KODE
Terdapat beberapa bug visual dan fungsional dari eksekusi Fase 10. UI *toggle* tidak menyembunyikan elemen yang tepat, dan skrip scraper membutuhkan *rewrite* ke Kotlin karena lingkungan Android tidak mendukung modul Node.js secara *native*. Terapkan perbaikan berikut:
## 1. Perbaikan UI Toggle Mode Offline vs Online
 * **Masalah:** Saat tombol tab "Offline" dipilih, layout yang berisi daftar paket bahasa (Language Pack Manager) tidak muncul, dan konfigurasi "Online" (AI Provider, API Key, Base URL) gagal disembunyikan.
 * **Solusi:**
   * Perbaiki *listener* pada MaterialButtonToggleGroup untuk Mode Translasi di MainActivity.
   * Jika "Offline" ditekan: Set visibilitas LinearLayout konfigurasi online menjadi View.GONE, lalu set layout daftar bahasa ML Kit (Language Manager) menjadi View.VISIBLE.
   * Jika "Online" ditekan: Lakukan sebaliknya.
## 2. Penyembunyian Field Konfigurasi untuk "Default Translator"
 * **Masalah:** Saat *dropdown* AI Provider dipilih ke mode "Default Translator (Free)", isian API Key dan Base URL masih terlihat.
 * **Solusi:**
   * Tambahkan OnItemSelectedListener atau TextWatcher pada *AutoCompleteTextView* AI Provider.
   * Jika teks yang terpilih adalah "Default Translator", set *visibility* untuk *layout*/isian API Key dan Base URL menjadi View.GONE. Untuk *provider* lain seperti OpenAI/Gemini, set kembali menjadi View.VISIBLE.
## 3. Rewrite Fungsionalitas scrape_ai.js ke Kotlin (OkHttp)
 * **Masalah:** Skrip scrape_ai.js yang dilampirkan menggunakan fungsi require('axios') dan crypto khas Node.js. Skrip ini akan menyebabkan *crash* atau gagal dieksekusi jika dipaksakan masuk ke WebView Android atau QuickJS.
 * **Solusi (Native Kotlin Porting):**
   * Translasikan logika *scraping* ChatGPT Anonim tersebut ke dalam *class* Kotlin baru, misal DefaultScraperTranslator.
   * Gunakan OkHttpClient standar aplikasi untuk melakukan *request* POST ke [https://android.chat.openai.com/backend-anon/sentinel/chat-requirements](https://android.chat.openai.com/backend-anon/sentinel/chat-requirements) guna mendapatkan token/cookie.
   * Gunakan java.util.UUID.randomUUID().toString() untuk meniru crypto.randomUUID().
   * Lakukan *request* *Server-Sent Events* (SSE) ke /backend-anon/f/conversation dan *parsing* balasan JSON-nya persis seperti alur di skrip JS tersebut. Gabungkan nilai yang ada di *path* /message/content/parts/ dan gunakan Regex Kotlin untuk membersihkan tag \ue200entity... (seperti pada fungsi cleanSpecialTags di JS).
## 4. Verifikasi & Build
 * Pastikan perubahan tidak merusak fitur *Auto-hide Floating Ball* dan *Smart Block Merging*.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi tidak ada *error* kompilasi pada *OkHttp requests* yang baru.
