
**TUGAS FASE 9: DYNAMIC BORDER STATICLAYOUT & FLOATING CONTROL BALL**
# SYSTEM INSTRUCTION: MEMORY MANAGEMENT SYSTEM
Kamu beroperasi di lingkungan GitHub Actions yang bersifat stateless. Untuk menjaga kesinambungan pekerjaan tanpa membuat context window (token) overload, kamu WAJIB mematuhi protokol memori berikut:
## 1. Protokol Membaca & Menulis Konteks
 * **Cek Master Index:** Selalu baca file ai_memory/00_INDEX.md terlebih dahulu.
 * **Cek Log Terbaru:** Baca maksimal 2 file log terbaru di dalam folder ai_memory/.
 * **Buat File Log Baru:** Setelah selesai melakukan tugas, buat log task_YYYYMMDD_HHMM_phase9_ui_floating.md.
 * **Perbarui 00_INDEX.md:** Tambahkan referensi log baru ke dalam file index utama.
 
# INSTRUKSI PERBAIKAN BUG & PENAMBAHAN FITUR
Kamu bertugas memperbaiki kotak *background* yang bocor (*overflow*) karena ukurannya tidak mengikuti panjang teks terjemahan, serta menambahkan fitur *Floating Control Ball* untuk navigasi cepat.
## 1. Perbaikan Dynamic Border & Posisi Loading (OverlayManager)
 * **Masalah:** Teks terjemahan meluber keluar dari batas *bubble* dan posisi *loading bubble* (...) melenceng. Ini karena Canvas.drawRoundRect masih menggunakan koordinat Rect asli dari OCR, bukan dimensi asli dari StaticLayout yang sudah menyesuaikan *word-wrap*.
 * **Solusi:**
   * Di dalam OverlayManager (saat menggambar *batch* terjemahan), **setelah** StaticLayout terbentuk, hitung dimensi aktual teksnya (layout.width dan layout.height).
   * Gunakan dimensi aktual tersebut (ditambah *padding* yang wajar, misalnya 8dp - 12dp) untuk mendefinisikan batas drawRoundRect sebagai latar belakang/border, sehingga kotak akan selalu membungkus teks dengan sempurna secara dinamis.
   * Untuk *Loading Bubble*, pastikan posisinya dihitung agar sentris (*center-aligned*) terhadap *bounding box* asli yang diberikan oleh parameter.
## 2. Fitur Floating Control Ball (Play/Pause)
 * **Kebutuhan:** Pengguna ingin bisa menjeda (*pause*) dan melanjutkan (*play*) layanan terjemahan langsung dari layar mana pun tanpa harus membuka aplikasi utama.
 * **Solusi:**
   * Buat sebuah *Custom View* mengambang (seperti *Chat Head*) di ScreenCaptureService menggunakan WindowManager (dengan TYPE_APPLICATION_OVERLAY dan FLAG_NOT_FOCUSABLE).
   * Tambahkan OnTouchListener agar bola ini bisa diseret (*drag*) ke mana saja di layar.
   * Tambahkan fungsi *Click/Tap* pada bola tersebut untuk mengganti status terjemahan (*Pause* / *Play*). Jika di-*pause*, abaikan *event* dari *Accessibility Service*. Ubah ikon bola (misal: ikon *Pause* saat aktif, ikon *Play* saat jeda) untuk memberi tahu pengguna status saat ini.
## 3. Verifikasi & Build
 * Pastikan fitur blok yang digabung (*Smart Merging*) dari Fase 8 tidak terganggu.
 * Jalankan ./gradlew :app:compileDebugKotlin atau ./gradlew assembleDebug untuk memverifikasi.
