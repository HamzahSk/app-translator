# Task Phase 15: UI Revamp and Missing Features

**Timestamp:** 2026-08-22 23:02 UTC

## Changes
- Replaced the purple Material3 palette in light and dark themes with teal/emerald primary and azure/cyan secondary colors.
- Updated the main Start Service action and floating control ball to the new teal accent.
- Added an App Language dropdown to Overlay Customization with System Default, English, and Indonesian choices, persisted through `ConfigManager`; the dialog notifies users that restart is required.
- Added the Paragraph Grouping / Margin slider (1.0x–4.0x) and connected it to the existing `paragraphGroupingMargin` configuration used by `TranslationEngine.mergeBlocks()`.
- Added English and Indonesian catalog strings for the new controls.

## Verification
- `./gradlew spotlessApply` succeeded.
- `./gradlew assembleDebug` succeeded.
- Existing warning remains for deprecated `scaledDensity` use in `OverlayManager.kt`.
