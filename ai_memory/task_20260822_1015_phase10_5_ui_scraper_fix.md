# Task Log: Phase 10.5 UI Toggle and Native Scraper Fix

## Status
Implemented and compile-verified.

## Changes
- Translation mode toggle now hides online configuration and shows ML Kit model manager in Offline mode, with inverse behavior for Online mode.
- Default Translator provider hides API Key and Base URL fields; other providers restore them.
- Added `DefaultScraperTranslator`, a Kotlin/OkHttp native port for anonymous ChatGPT session setup, SSE conversation parsing, UUID identifiers, and special-tag cleanup.
- Default provider now routes translation through the native scraper.

## Verification
- `./gradlew :app:compileDebugKotlin` succeeded.
