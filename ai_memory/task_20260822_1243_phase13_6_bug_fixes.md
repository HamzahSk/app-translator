# Task Phase 13.6: i18n, Settings Theme, and Spotless Fixes

**Timestamp:** 2026-08-22 12:43

## Changes
- Added `copyI18nAssets` in `app/build.gradle.kts` and connected it to `preBuild`, ensuring root `i18n/` XML catalogs are packaged in every Android build.
- Replaced the stale JSON loader in `I18nManager` with an `XmlPullParser` loader for `assets/i18n/<language>/strings.xml`.
- Added locale normalization and English catalog fallback; missing locale catalogs or keys now resolve through English before falling back to the key/default argument.
- Applied the Settings dialog surface background programmatically from the active Material theme, with `android:colorBackground` fallback and a transparent dialog window.
- Updated the XML drawable fallback to use `colorSurface` instead of hardcoded white.
- Ran Spotless across Kotlin sources and fixed the remaining long-line formatting issue.

## Verification
- `./gradlew spotlessApply` succeeded.
- `./gradlew assembleDebug` succeeded.
- APK inspection confirmed `assets/i18n/en/strings.xml` and `assets/i18n/id/strings.xml` are packaged in the universal debug APK.
- Build emitted one existing deprecation warning for `scaledDensity` in `OverlayManager.kt`; it does not affect build success.
