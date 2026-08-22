# Task Log: Phase 11 Online Batch JSON and UI Toggle Fix

## Status
Implemented and compile-verified.

## Changes
- Moved `layoutOnlineConfig` into a nested LinearLayout containing only online provider/API/model fields; mode toggle and hint remain visible in both modes.
- Replaced delimiter-based online batch parsing with JSON array prompting and `org.json.JSONArray` extraction from the first `[` through the last `]`.
- Updated `TranslationEngine` to use the JSON batch API; partial arrays remain usable through indexed mapping.

## Verification
- `./gradlew :app:compileDebugKotlin` succeeded.
- Existing warning: deprecated `scaledDensity` usage in `OverlayManager.kt`.
