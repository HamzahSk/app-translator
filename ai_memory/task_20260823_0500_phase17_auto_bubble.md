# Task Phase 17: Auto Font Sizing & Tight Bubble Wrap

**Timestamp:** 2026-08-23 05:00 UTC

## Changes
- Added persisted `ConfigManager.autoTextFitEnabled` setting.
- Added Settings dialog switch with English/Indonesian labels.
- Added binary-search font sizing in `OverlayManager` so translated text fits the original OCR bounds.
- Added tight wrap-content bubble sizing with padding when auto fit is enabled; legacy slider sizing remains unchanged when disabled.

## Verification
- `./gradlew spotlessApply` — SUCCESS.
- `./gradlew assembleDebug` — SUCCESS.
- Existing warning remains for deprecated `scaledDensity` usage in `OverlayManager.kt`.
