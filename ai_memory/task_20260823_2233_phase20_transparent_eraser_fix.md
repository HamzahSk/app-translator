# Task Phase 20: Transparent Mode, Smart Eraser, and Rotated Bubble Fix

**Timestamp:** 2026-08-23 22:33

## Changes
- Added persisted `isTransparentModeEnabled` and `isEraserModeEnabled` settings with Material Switch controls.
- Moved canvas save/rotation before bubble background and border rendering so all bubble elements rotate together.
- Added transparent rendering with a two-pass text outline/fill.
- Added perimeter color sampling in `TranslationEngine` and propagated the sampled color through `MergedBlock` and `OverlayManager.Bubble`.
- Smart Eraser paints the OCR bounding box with the sampled dominant edge color when both eraser and transparent modes are enabled.

## Verification
- `./gradlew spotlessApply` — SUCCESS.
- `./gradlew assembleDebug` — SUCCESS.
- Existing warning remains for deprecated `scaledDensity` usage in `OverlayManager.kt`.
