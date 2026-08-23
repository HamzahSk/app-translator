# Task Phase 16: Bug Fixes & OCR Block Merging Refinement

**Timestamp:** 2026-08-23 02:49 UTC

## Changes
- Tuned `TranslationEngine.mergeBlocks()` to merge fragmented OCR lines with overlapping boxes and larger inter-line leading: vertical tolerance 2.2× line height (scaled by grouping margin), size tolerance 45%, and normalized negative gaps.
- Corrected loading overlay dimensions to use the OCR bounding box directly instead of an unrelated 100×32 minimum, with clamped screen coordinates.
- Implemented visible bubble border rendering in `OverlayManager` when `bubbleBorderEnabled` is true, using the configured text color and rounded geometry.
- Verified `SettingsDialog` border switch binding remains connected to `ConfigManager.bubbleBorderEnabled`.

## Verification
- `./gradlew spotlessApply` — SUCCESS.
- `./gradlew assembleDebug` — SUCCESS.
- Existing warning remains for deprecated `scaledDensity` usage in `OverlayManager.kt`.
