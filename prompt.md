### TUGAS FASE 23: KUSTOMISASI UI (WARNA TEKS) & SISTEM FALLBACK TRANSLASI OFFLINE
**1. PROTOKOL SISTEM & MEMORI**
 * Mengingat operasi berjalan di lingkungan GitHub Actions yang *stateless*, wajib jaga protokol memori dengan membaca ai_memory/00_INDEX.md dan maksimal 2 log terbaru sebelum mulai.
 * Buat log eksekusi baru dengan format task_YYYYMMDD_HHMM_phase23_ui_color_offline_fallback.md dan pastikan direferensikan ke index utama.
**2. OBJEKTIF PENGEMBANGAN FITUR**
 * **Kustomisasi & Deteksi Warna Teks:** Tambahkan menu pengaturan agar pengguna bisa mengubah warna teks hasil terjemahan. Buat juga fitur **Auto-Detect** untuk mendeteksi warna asli dari teks sumber. *Perhatian Khusus:* Implementasikan filter akurat agar sistem mengambil warna dominan dari *body* teks, bukan mendeteksi warna *outline* (garis tepi) atau bayangannya.
 * **Sistem Transisi Offline (Fallback):** Tanamkan *Network Connectivity Listener*. Jika aplikasi mendeteksi tidak ada koneksi internet, otomatis arahkan sistem untuk menggunakan model translasi *offline*.
 * **Transisi Real-Time & Notifikasi:** Fitur *fallback* ini wajib berlaku saat *translator* sudah di-*start*. Jika koneksi terputus tiba-tiba di tengah proses, lakukan transisi ke *offline* tanpa menyebabkan *crash*, lalu segera tampilkan notifikasi (Toast/Snackbar) berisi: "Koneksi terputus, beralih ke mode offline otomatis".
**3. IMPLEMENTASI TEKNIS & KEAMANAN**
 * Gunakan ConnectivityManager yang aman dan tidak membebani memori untuk memantau status jaringan secara *real-time*.
 * Pastikan pemanggilan notifikasi transisi mematuhi izin POST_NOTIFICATIONS untuk Android 13+ agar tidak terjadi kendala kompatibilitas OS.
**4. KRITERIA VERIFIKASI**
 * Menu perubahan warna teks berfungsi, dan auto-detect warna berhasil mengabaikan elemen *outline*.
 * Aplikasi sukses berpindah ke mode *offline* secara mulus (saat sebelum dan sesudah *start* translasi) disertai munculnya notifikasi saat internet dimatikan.
 * Lolos verifikasi kompilasi tanpa *error* menggunakan perintah ./gradlew assembleDebug.