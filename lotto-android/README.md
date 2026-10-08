# Lotto Intelligence — native Android

An **independent, unofficial South African Lotto research app** built with Kotlin and Jetpack Compose. This lives entirely within `lotto-android/` so the existing Stella app is unchanged.

## Current MVP

- Native Material 3 dark/yellow UI with Home, Draws, Generate, Analyse and Saved tabs.
- Local Android SQLite database; draw history and saved lines work offline.
- Historical result import from the bundled `app/src/main/assets/draws.json` when present; five verified fallback records in `seed.json`.
- Background sync via WorkManager (approximately daily when connected), plus manual refresh and app-opening sync.
- Free BetTip draws API (attribution required): https://bettip.co.za/api/ .
- Quick Pick and range-spread number generation, saved lines, number frequency and PDF/JPEG exports via Android share sheet.
- No accounts, server, payment system or lottery ticket sales.

### Build

Use the dedicated **Build Lotto Intelligence APK** workflow from GitHub Actions; it fetches an updated archive (if accessible) before building the debug APK. Download the `lotto-intelligence-debug-apk` artifact, unzip it and install `app-debug.apk`. Alternatively open the `lotto-android` folder as an Android Studio project and build the `app` module.

Requirements: JDK 17, Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.7.3.

The APK is a **debug/testing build**, not signed for Google Play distribution. A release build needs a private keystore.

### Data and limitations

The local database starts from a bundled JSON archive when the workflow can download it, otherwise from a small explicitly labelled seed set. The application fetches source data only with an active connection and shows cached results offline. A source's `verified` flag is retained, but it does *not* mean this app independently verified the record. Review published draw numbers with the lottery operator. Historic number ranges changed across game eras; raw frequencies across eras aren't a statistical prediction.

API courtesy of **[BetTip](https://bettip.co.za/)** (free for reuse with attribution). Attribution remains visible in-app and on exports.

18+ only. Play responsibly. South African National Responsible Gambling Programme: 0800 006 008. No generator improves the mathematical likelihood that a specific valid line is drawn.
