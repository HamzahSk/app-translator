# Task Log: Phase 13 UI Settings, Placement, and i18n

## Status
Implemented and compile-verified.

## Changes
- Added a close (X) button to the Settings dialog and rounded elevated-style dialog background.
- Placement modes now change translated text alignment within the OCR bounding box; loading and translation canvas anchors remain static.
- Added translator-friendly JSON catalogs at `app/src/main/assets/i18n/en/strings.json` and `id/strings.json`.
- Added `I18nManager` for dynamic JSON catalog loading with fallback behavior.

## Verification
- `./gradlew :app:compileDebugKotlin` succeeded.
