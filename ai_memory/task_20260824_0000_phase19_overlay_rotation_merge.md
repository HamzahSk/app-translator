# Task Phase 19: Overlay Bug Fix, Text Rotation, Smart Merge UI

**Timestamp:** 2026-08-24

## Changes
- Added a transparent 1x1 outside-touch observer window to clear overlays when external apps receive touch input.
- Added persisted Auto Rotate Canvas configuration and rotation propagation from ML Kit `cornerPoints` into merged OCR blocks and canvas rendering.
- Moved smart merge tolerances into `ConfigManager` and exposed three live Settings sliders.

## Verification
- `./gradlew spotlessApply` — SUCCESS.
- `./gradlew assembleDebug` — SUCCESS.
- Existing warning remains for deprecated `scaledDensity` usage in `OverlayManager.kt`.
