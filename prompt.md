**TUGAS FASE 10: AUTO-HIDE FLOATING BALL, OFFLINE LANGUAGE MANAGER & DEFAULT JS TRANSLATOR**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase10_ui_and_scraper.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PERBAIKAN BUG & PENAMBAHAN FITUR
Kamu bertugas melakukan penyempurnaan UI/UX secara masif pada menu utama dan fitur *floating ball*, serta mengintegrasikan *script* JS lokal sebagai *provider* translasi alternatif.
## 1. Floating Ball: Auto-Hide, Kustomisasi & Logika Hard-Pause
 * **Masalah Utama:** *Floating ball* ukurannya terlalu besar, menutupi layar, dan saat dijeda (*pause*), proses yang sedang berjalan tidak dibatalkan.
 * **Solusi UI (Kustomisasi & Auto-Hide):**
   * Tambahkan pengaturan ukuran (*slider*) di ConfigManager dan terapkan pada *LayoutParams* bola tersebut.
   * Implementasikan *timer* selama 5 detik setiap kali bola selesai disentuh. Jika tidak ada interaksi selama 5 detik, jalankan animasi animate().translationX(...) untuk menggeser bola ke tepi layar terdekat (kiri atau kanan) dan kurangi *opacity*-nya (misal menjadi 0.5f). Sentuhan berikutnya harus mengembalikan ukuran/posisi/opacity bola dan mereset timer.
 * **Solusi Logika Hard-Pause:**
   * Saat bola ditekan untuk mengubah status menjadi **Pause**:
     1. Ubah ikon menjadi ikon *Play*.
     2. Set *flag* internal agar *Accessibility Service* berhenti mengirimkan *event capture*.
     3. **Batalkan (cancel)** seluruh activeJob yang sedang berjalan di TranslationEngine (hentikan paksa proses OCR dan translasi).
     4. Panggil overlayManager.clearOverlays() untuk **membersihkan seluruh canvas/bubble** dari layar seketika itu juga.
## 2. Peningkatan UI Mode Offline (Language Pack Manager)
 * **Masalah:** Saat tab "Offline" dipilih, pengguna tidak bisa mengatur paket bahasa ML Kit mana yang sudah/belum diunduh.
 * **Solusi:**
   * Modifikasi UI MainActivity. Saat tab "Offline" aktif, sembunyikan isian API Key dan Base URL, lalu tampilkan daftar (*RecyclerView* atau *Layout* dinamis) yang berisi bahasa ML Kit yang didukung.
   * Berikan indikator ("Installed", "Not Installed", "Downloading") dan tombol untuk mengunduh (downloadModelIfNeeded) atau menghapus model secara manual.
## 3. Integrasi "Default Translator" (Scrape AI via JS)
 * **Masalah:** Pengguna butuh opsi gratis di mode "Online" menggunakan *script* scrape_ai.js.
 * **Solusi:**
   * Pada *dropdown* **AI Provider**, tambahkan opsi **"Default Translator (Free)"**.
   * Jika dipilih, sembunyikan *API Key* dan *Base URL*.
   * Di dalam TranslationEngine atau OnlineTranslator, buat mekanisme pengeksekusi JavaScript (bisa menggunakan android.webkit.WebView tersembunyi dengan evaluateJavascript, atau *library* seperti QuickJS/Duktape jika tersedia) untuk memanggil fungsi di dalam assets/scrape_ai.js, mengirimkan *string* asli, dan mengembalikan hasil terjemahannya.
## 4. Verifikasi & Build
 * Pastikan fitur dari fase sebelumnya (Dynamic Border StaticLayout & Smart Merging) tidak terhapus.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi
 