# Task Log: Phase 12 UI/UX Revamp and AI Prompt Context

## Status
Implemented and compile-verified.

## Changes
- Reworked online single and batch prompts for natural Manga/Manhwa dialogue while preserving JSON-array output requirements.
- Removed delay and overlay customization controls from the home screen.
- Added a toolbar gear action opening a scrollable Material settings dialog.
- Moved placement, opacity, corner radius, text size, auto-clear, bubble border, inactivity delay, and floating-ball size controls into the dialog.
- Added slider increments for 0.5-second inactivity delay and 5% bubble opacity; values persist to `ConfigManager` in real time.

## Verification
- `./gradlew :app:compileDebugKotlin` succeeded.
