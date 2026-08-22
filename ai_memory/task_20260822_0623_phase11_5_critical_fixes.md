# Task Log: Phase 11.5 Critical Fixes

## Status
Implemented and compile-verified.

## Changes
- Moved `layoutModelsCard` from Overlay Customization to the AI Models (OCR) card, preserving overlay settings in both translation modes.
- Online batch translations now collect `OverlayManager.Bubble` values and render once with `drawTranslationBatch`; loading bubbles are removed only in `finally`.
- Loading bubbles now honor left/right placement offsets.
- Smart merging now flattens ML Kit text blocks to individual lines, sorts them by Y position, and uses each line's height for the 1.5x vertical and 30% size checks.

## Verification
- `./gradlew :app:compileDebugKotlin` succeeded.
- Existing warning remains: Android deprecates `scaledDensity` in `OverlayManager.kt`.
