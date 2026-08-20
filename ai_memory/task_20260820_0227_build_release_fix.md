# Task Log: Build & Release Fix

## Status
Selesai (Build debug + release sukses)

## Ringkasan Perubahan
- **ProGuard/R8 fix** (`app/proguard-rules.pro`): R8 sebelumnya meng-obfuscate paket `com.ervareza.screentranslator.online.**` (contoh: `OpenAiChatRequest -> h1.k`, field `model -> a`, `AiApiService -> h1.b`). Akibatnya payload JSON online mode (OpenAI/Gemini) rusak saat runtime meski build sukses. Ditambahkan aturan keep untuk seluruh paket online + aturan standar Retrofit/Gson/OkHttp (`-keepattributes Signature/Annotation`, keep `retrofit2.Call/Response`, `kotlin.coroutines.Continuation`, keep method ber-annotasi `@retrofit2.http.*`, `dontwarn okhttp3/okio/retrofit2`). Hasil mapping kini membuktikan nama kelas & field Gson dipertahankan.
- **Comment signing config** (`app/build.gradle.kts`): release APK sengaja memakai `signingConfigs.getByName("debug")` untuk CI; ditandai komentar bahwa produksi Play Store harus pakai release keystore.

## Verifikasi
- `:app:assembleDebug` → SUCCESS (45s)
- `:app:assembleRelease` (R8 + shrinkResources) → SUCCESS
- Split APK terverifikasi: universal, arm64-v8a, armeabi-v7a, x86, x86_64 (5 APK).
- Mode online (Retrofit + Gson) terverifikasi aman dari R8.

## Tugas Selanjutnya (Next Steps)
- Uji runtime manual: inisialisasi lazy (Splash VectorDrawable, coroutine ML Kit) di device.
- Uji mode online dengan API key nyata (OpenAI & Gemini).
- Untuk distribusi resmi: ganti signing config release dengan release keystore.
