# 0.4.0 — data engine (no UI redesign)

- Locks the four-tab LottoLab UI and theme, retaining existing saved entries and application identity.
- Bundles independently published 2025–2026 draw history into an offline SQLite seed at build time after strict per-game/date validation.
- Automatically opts new installations into the public operator-results endpoint, schedules six-hour network-aware checks and network-connected catch-up on app launch.
- Keeps the existing manual Settings → Refresh results button.
- Performs idempotent, non-destructive seed insertion so offline records and user's own data survive updates.
- Rejects incomplete source datasets during the build; missing Wi-Fi/mobile data does not block normal app use.
- Records source URL and import time for each historical draw. Third-party archives and public operator results are not independently certified.

# 0.3.0

- Fixed the number picker sizing for narrow screens, increased font scales and system navigation insets.
- Added explicit opt-in to recent National Lottery website results from its public, undocumented feed; preserved strict validation, source provenance and existing user history.
- Preserved manual archive import and optional custom HTTPS feed settings.
- Added native tests for operator pool mappings, duplicates and dialog action visibility.
- Kept prize payout verification and long-term feed availability explicitly unconfirmed.

# 0.2.0

- Replaced the five-tab console with Home, Results, Lotto Lab and My Numbers.
- Grouped tools by task and added guided number entry, predictable back navigation and honest empty/error states.
- Removed fictional runtime draw fixtures.
- Added historical rule-era validation, source metadata and retained correction revisions.
- Added constrained unique generation, full systems, coverage, pair/triple searches, gaps, patterns, odds and simulations.
- Added dated ticket snapshots, purchase-month spending, notes, backup/merge restore and native sharing.
- Retained the original package and introduced a non-destructive database migration.
- Corrected development signing-key retention; the unavailable 0.1 private key prevents an in-place preview update.
- Kept the official live-feed and verified prize-payout limitations visible in the app.
