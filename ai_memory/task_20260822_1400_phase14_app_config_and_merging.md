# Task Phase 14: App Config, Block Merging, Firebase, and Package

**Timestamp:** 2026-08-22 14:00 UTC

## Changes
- Added persisted `appLanguage` (`system`, `en`, `id`) and `paragraphGroupingMargin` (1.0–3.0) settings to `ConfigManager`.
- Applied the grouping margin multiplier to `TranslationEngine.mergeBlocks()` vertical proximity.
- Changed Android namespace/applicationId and Kotlin package/imports to `com.rocat.translator`.
- Added optional Firebase Analytics and Crashlytics dependencies; initialization remains absent/optional so missing services config does not affect startup.
- Added environment-aware release signing selection with debug-signing fallback when `SIGNING_KEY` is unavailable.
- Declared the i18n asset copy/Spotless task ordering dependency required by Gradle validation.

## Verification
- `./gradlew spotlessApply` succeeded.
- `./gradlew assembleDebug` succeeded.
- Existing warning: deprecated `scaledDensity` usage in `OverlayManager.kt`.
