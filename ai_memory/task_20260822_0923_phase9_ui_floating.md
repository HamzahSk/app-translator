# Task Log: Phase 9 Dynamic Bubble Border & Floating Control Ball

## Status
Implemented. Build verification was attempted but Java/JAVA_HOME is unavailable in the environment.

## Changes
- Overlay translation bubble bounds now derive from `StaticLayout.width` and `layout.height`, with 10dp padding and symmetric centering around the OCR bounds.
- Added `TranslationControlState` shared pause flag.
- Added draggable `TYPE_APPLICATION_OVERLAY` control ball to `ScreenCaptureService`; tap toggles pause/play and updates the pause/play glyph.
- Accessibility triggers and screen capture are ignored while paused.

## Verification
- `sh gradlew :app:compileDebugKotlin` could not run: `JAVA_HOME is not set and no 'java' command could be found`.
