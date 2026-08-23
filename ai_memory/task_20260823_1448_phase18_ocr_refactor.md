# Task Phase 18: OCR Refactor and Memory Management

**Timestamp:** 2026-08-23 14:48 UTC

## Root Cause Analysis
- Capture triggers could overlap because each trigger launched an independent capture coroutine.
- Captured bitmaps were handed to the OCR pipeline without an ownership/cleanup contract, so completed or cancelled jobs could retain native pixel memory.
- A cached OCR recognizer remained alive for the process lifetime and was not cancellable at the recognizer instance level.
- The floating control ball cancelled only on `ACTION_UP`, allowing OCR work to continue during touch handling.

## Changes
- Added a single active capture job; a new capture cancels the previous capture.
- Added active bitmap ownership tracking and aggressive `Bitmap.recycle()` cleanup on completion, replacement, pause, close, and cancellation.
- Reworked OCR recognizer use to create, track, close, and release one recognizer per operation, including preload cleanup.
- Touch `ACTION_DOWN` now immediately cancels OCR, closes the active recognizer, clears overlays, and returns the UI to idle.
- Existing coroutine cancellation and ML Kit timeout boundaries remain in place; heavy work stays off the main thread.

## Verification
- `./gradlew spotlessApply` — SUCCESS.
- `./gradlew assembleDebug` — SUCCESS.
- `git diff --check` reports only pre-existing whitespace in the user-modified `ai-response-recap.txt`.
- No instrumentation device was available for a runtime RAM stress test; the implementation uses deterministic cancellation and native resource release paths.
