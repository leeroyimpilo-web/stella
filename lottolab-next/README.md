# LottoLab Next

Fresh native Android project, created October 2026. Application ID: `za.co.lottolab.studio`.

The user-approved laboratory concept guides an actual native interface: vector-drawn chamfered panels, orange controls, green number tokens, accessible Android widgets and five working tabs. No previous LottoLab application code is used. No screenshot is used as the app interface.

## Preview 0.1

- Home and searchable History for LOTTO, Plus 1, 5 Max, PowerBall, Xtra and Daily LOTTO.
- Up to six editable combinations per game, research subsets, separate PowerBall, random valid selection.
- Main-match distributions, exact repetitions, subset counts, frequencies, pair counts, structure and line overlap.
- Named saved combinations and persisted drafts.
- Planner with required add-on costs, personal budget and saved settings.
- Validated, atomic CSV import into SQLite; archive CSV export and A4 research PDF export through Android's file picker.
- Offline operation with no runtime network dependency, account or broad storage permission.

**Bundled draw results are fictional design fixtures, prominently labelled SAMPLE DATA.** They are never described as official or live. When a game's imported rows exist, analysis uses only those rows and labels them unverified imported data. The first build supports the current June 2026-onward rule matrices. No operator feed, full historical rule eras, prize claims, notifications or full wheel engine is connected.

## Build

Requires JDK 17, Gradle 8.13, Android SDK platform 36 and build tools 35.0.0.

```sh
gradle -p lottolab-next :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

The `LottoLab Next / APK and native UI checks` workflow builds on `lottolab-next/**` branches. It also runs device-level smoke checks on an API 35 emulator, validates storage and captures actual native UI screenshots. The APK is signed with a development preview identity; production signing will be configured for a production release.

## Import format

```csv
game,date,numbers,bonus,powerball
LOTTO,2026-10-14,4 12 19 28 36 47,8,
POWERBALL,2026-10-13,4 12 19 28 47,,8
DAILY,2026-10-14,4 12 19 28 36,,
```

These examples are fictional. Main numbers may be space-separated or a quoted comma-separated field. A file is validated completely before a transaction writes it. Files are limited to 2 MB and 10,000 draws. Re-import replaces matching game/date rows; there is no operator-verification claim or correction audit in this preview. Missing LOTTO bonus values are allowed, but the supplied bonus cannot duplicate a main number. PowerBall is required for PowerBall draw records and may equal a main number because it uses a separate pool. Unsupported games, historical rules before June 2026, future dates, duplicate rows and invalid combinations are rejected.

## Data and planning boundaries

Historical frequency is descriptive. Every distinct valid complete entry has the same jackpot chance in a fair future draw. Main frequency excludes the LOTTO bonus ball. Full PowerBall matches require the selected PowerBall. Subset occurrences and full-entry repetitions remain distinct.

Entry prices in cents: LOTTO 500; Plus 1 adds 250; 5 Max adds 250; PowerBall 1000; Xtra adds 500; Daily 300. Planner uses the complete required bundle for add-ons and excludes sales-channel fees. Scheduled dates are estimates; official exceptions and channel cut-offs are not connected.

## Next implementation stages

1. Obtain a permitted, verified draw feed and authoritative rule cut-over dates.
2. Import and audit real historical coverage with rule-version identity and source provenance.
3. Add historical prize divisions, dated experiment snapshots, triple search and advanced simulations.
4. Design production signing, backups, optional sync and notification delivery.
