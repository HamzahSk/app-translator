# Task Phase 23: UI Text Color and Offline Fallback

**Timestamp:** 2026-08-25 12:35 UTC

## Changes
- Added a persisted translated-text color field and Auto-Detect toggle to Settings.
- Added per-OCR-line text fill color sampling. The sampler quantizes antialiased pixels, rejects background-like colors, and penalizes colors adjacent to the background so outline/shadow colors are not selected as the glyph body.
- Propagated detected colors through merged OCR blocks into overlay rendering; manual color remains the fallback when Auto-Detect is disabled.
- Added a service-scoped `ConnectivityManager.NetworkCallback` using validated internet capability checks and lifecycle-safe unregistering.
- Online mode now uses offline ML Kit whenever connectivity is unavailable before capture, and retries the same OCR result offline when connectivity drops during an online request.
- Shows the required Toast once on each connected-to-disconnected transition: `Koneksi terputus, beralih ke mode offline otomatis`. Toast does not require `POST_NOTIFICATIONS`; the manifest and UI permission flow retain Android 13+ notification permission handling for service notifications.

## Verification
- `./gradlew spotlessApply assembleDebug --no-daemon`: Kotlin compilation succeeded; first Split APK packaging attempt failed without a detailed packaging cause.
- `./gradlew assembleDebug --no-daemon --stacktrace`: SUCCESS on immediate retry.
- Existing warning remains for deprecated `scaledDensity` usage in `OverlayManager.kt`.
