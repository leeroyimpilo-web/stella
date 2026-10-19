# LottoLab 0.7

A native Android lottery workspace with four clear tabs. This upgrades the existing `lottolab-next` project and retains application ID `za.co.lottolab.studio`, its SQLite database and saved preferences. It uses the existing Java/native Android stack rather than replacing the app with a WebView or introducing a new package.

## Screen map

| Tab | Purpose | Main routes |
| --- | --- | --- |
| Home | Choose a task and see the latest available result | Check my numbers; Generate numbers; Explore history |
| Results | Browse imported results and their provenance | Game; rule period; number and date filters; draw details |
| Lotto Lab | Find a tool by what you want to do | Check; Explore; Build; Plan |
| My Numbers | Organise personal records | Favourites; dated tickets; spending; exports |

Settings, data management and help live in the top menu. Tool screens retain the selected game and have back navigation. Advanced generator settings are optional. The number editor has tappable cells, a selection counter, paste support and separate PowerBall input. Native font scaling and narrow layouts are supported.

## Implemented tools

- Dated full-line checking; missing results are unknown rather than losses.
- Main-number subset search and exact complete-line search.
- Six-line historical comparison with match distributions and tied best dates.
- Main frequencies, hot/cold descriptions, last appearances and gaps.
- Pair/triple co-occurrences and draw patterns with CSV exports.
- Random unique generation with locked/excluded numbers, fixed PowerBall and odd-count preferences.
- Full-system expansion, cost preview and precise conditional coverage (up to 2,000 generated lines).
- Duplicate, overlap and structural coverage inspection.
- Exact match-category probabilities with distinct bonus/PowerBall treatment.
- Entry-cost planning, monthly spending limits and recorded-purchase tracking.
- Bounded randomness simulations clearly labelled imaginary.
- Saved favourites, names, notes and immutable dated ticket snapshots.
- Native A4 PDF/CSV saving and sharing; local JSON backup and merge restore.
- Optional HTTPS feed adapter, periodic Android jobs and opt-in result notifications.

## Data boundaries

**The 0.4 APK bundles a validated, dated offline 2025–2026 historical archive; no fictional results are shown in the installed app.** The empty state guides users to enter published draw numbers, import an archive or explicitly connect to the public lottery website feed. Results are labelled entered/imported or downloaded feed data and must be checked with the operator. A configured feed is not automatically certified as an independently verified result or prize source.

Verified prize divisions/payouts are not connected. The checker reports main, bonus and PowerBall matches without claiming a monetary win. The odds screen shows mathematical match categories, explicitly not an authenticated prize table. Sports pools are outside this number-draw build.

Supported rule eras begin 21 September 2025. LOTTO-family history uses the documented 6/58 era before June 2026 and 6/52 thereafter; PowerBall history uses its separate 20-ball or 16-ball pool. Legacy add-on identities remain distinct from current names. Earlier unverified rules are rejected. Analyses default to one era and show the available records, dates and absent scheduled dates; schedule exceptions and complete coverage are not certified.

Primary rule references checked 15 October 2026:

- [Current matrices and add-on dependencies — Nedbank](https://personal.nedbank.co.za/legal/terms-and-conditions/lottery-games-terms-and-conditions.html)
- [Current ranges and renamed/discontinued games — Capitec](https://www.capitecbank.co.za/personal/transact/lotto/)
- [Capitec current terms](https://www.capitecbank.co.za/globalassets/pages/home/personal/lotto/lotto-terms-and-conditions-.pdf)
- [Current detailed pricing — Absa](https://www.absa.co.za/self-service/digital-banking-services/anytime-anywhere-banking-transactions/lotto-and-powerball/) (use its detailed game sections; older generic FAQ wording is inconsistent)
- [Historical LOTTO 6/58 rules](https://content.nationallottery.co.za/images/docs/LOTTO_Game_Rules%26Regulations_10Oct25.pdf)

Prices in cents, including required base entries for add-ons: LOTTO 500; Plus 1 bundle 750; 5 Max bundle 1000; PowerBall 1000; Xtra bundle 1500; Daily 300. Costs exclude sales-channel fees. A planned entry is not a purchase. Actual spending is counted by recorded purchase month, not the future draw month; record the paid amount of a bundle only once.

## Storage and upgrades

SQLite schema version 2 adds source metadata and correction history through non-destructive migration. Existing `saved`, `draft_*`, `game`, `budget` and planner preferences are retained. Identical draw imports are idempotent; changed numbers retain the earlier revision. Saved-line edits cannot change dated ticket selections.

Backup restoration validates first and merges favourites/ticket IDs without removing existing records. Existing draws take priority over conflicting backup copies. Source connection credentials/settings are not activated from a restored backup. Large report/export snapshots stay in private cache rather than Android's size-limited activity-state Bundle.

## Import and feed formats

CSV requires `game,date,numbers`; optional columns are `bonus,powerball,source_url,fetched_at`. Main numbers are space separated. PowerBall results must include their separately selected PowerBall. Imports are bounded to 2 MB/10,000 rows, checked completely and committed transactionally. Dates cannot be in the future; source links use HTTPS. Do not import generated selections as historical draws.

An optional approved JSON feed has a top-level `draws` array. Each object has `game`, ISO `date`, an integer `numbers` array, and the applicable `bonus`, `powerball` and optional HTTPS `source_url`. No endpoint is assumed. Duplicate identities, fractions, invalid numbers or future dates reject the update and retain the cache. Configure a permitted feed through Settings; periodic work runs only after one is configured. An additional opt-in connection can use the lottery operator website's undocumented public POST endpoint to download recent draws for all six current pools, with strict parsing, transactional storage and source labelling. It may stop working without notice and is not a licensed or guaranteed developer API.

## Build and verification

JDK 17, Gradle 8.13, SDK 36 and build tools 35.0.0:

```sh
gradle -p lottolab-next :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

The existing GitHub Actions workflow builds, checks APK signing and runs native device journeys on an API 35 emulator. Tests cover migration, calculations, validation, backup, persistence, snapshots and real native PDF generation. The device test additionally captures narrow-screen and large-font layouts for visual inspection. Fictional test fixtures exist only in test source sets and are not shipped as app history.

The CI workflow explicitly creates and retains a development signer before running tests, and checks that the APK matches that key. Production store signing is a separate release setup. Local verification builds use a separate test certificate.

**Installation boundary:** the 0.1 workflow attempted to cache a keystore path that did not exist, so its original private signing key is unavailable. This 0.2 APK cannot update an installed 0.1 preview in place. Do not uninstall a preview containing important records without first extracting or exporting them. The package and schema migration are preserved for installations with the matching signing key; the device suite verifies preservation of version-1 rows and saved preferences. Retain the corrected CI signing cache for subsequent 0.2/0.3 updates.


## Offline-first history and automatic updates (0.4)

**UI freeze:** The existing four-tab layouts, theme, button labels and visual design are retained.
The historical-data importer is build-time only; it does not collect user information.

The Android APK bundles `app/src/main/assets/seed_draws.csv`, regenerated from publicly
readable, independently operated ZA national-lottery archive pages during a successful CI build.
The supported date period is from 21 September 2025; these are historical facts with explicit
source URLs, not an independently certified complete official archive. If the source is unavailable,
the build fails rather than shipping empty or imaginary data.

The first launch imports seed records transactionally into local SQLite (INSERT OR IGNORE).
Existing entered/imported records, tickets, saved lines and preferences are never replaced.
All research, checking, generating, exporting and archiving remain usable without Wi-Fi/data.

New installs default to the operator website's optional *undocumented* public results endpoint and
enable a network-constrained six-hour background refresh. After extended offline use, opening the
app schedules a network-connected catch-up job. Android may defer tasks for battery optimization.
Previously saved explicit feed choices and background-update switches are kept.
To update immediately, use Settings → Refresh results. Refresh failure leaves cached draws untouched.
You can still import CSV manually, disable updates or choose a custom source in Settings.

Run `python3 lottolab-next/tools/build_seed.py` before local builds; its parser fixture
can run offline with `--fixture-only`. Build includes historical seed automatically in CI.
The archive site is independent of the lottery operator and does not provide an official developer
API SLA. Check the operator's published results before making a prize claim.


## Usability refresh (0.5)

The same lab-inspired colors and native Android UI remain. A top-left **☰** button opens the primary
side menu, with the features grouped by what someone is trying to do: start here, my numbers,
past results, other tools, help and data. The redundant bottom tab bar is hidden. Previously
working tools and their data models are retained.

Home now follows: game choice → locally computed “AI Lotto Stats” → latest saved result →
clear Check / Generate / Browse / My Numbers actions → help entry points. The name “AI Lotto
Stats” describes *smart historical analytics* only; there is no external AI service, future
prediction or paid service. Numbers come exclusively from up to 30 available same-era draws.

The User manual library and FAQ are packaged as Java assets, requiring no internet connection.
Help text avoids unexplained formulas and includes data-source, update, backup and
18+ information. The existing optional online feed and bundled SQLite offline archive continue
unchanged. No sensitive data is uploaded for stats or help.

User testing: verify screen reader access to each drawer link, narrow Android screens,
all drawer routes, Help chapters and FAQ dialogs, Home stats consistency, offline startup,
preserved tickets and signed upgrade from 0.4.


## 0.6 — automatic updates and richer stats

- No data-source, API URL or background scheduling toggle appears in Settings.
- New and existing installations automatically enable network-constrained, persisted Android
  periodic result checks approximately every six hours, and schedule a catch-up attempt
  on launch when a connection and enough time have elapsed. Android may defer work.
- A prior approved custom HTTPS source is retained; when no source is present, the existing
  public National Lottery *undocumented* endpoint is used automatically. No user key is required.
  This is best-effort access, **not a guaranteed official developer API**; last-success timestamps
  confirm an accepted download, not independent prize validation.
- Offline SQLite history, saved numbers, dated tickets, exports, backup files and navigation
  remain unchanged. An internet failure never deletes cached history.
- Home AI Lotto Stats now shows leading pairs/triples and average patterns; a window chooser
  opens 30, 100, or all available draws in the same rule era. Rank ties are deterministic
  and missing archive dates affect descriptive statistics.
- The latest result and historical draw cards link to a dated independent public
  prize-breakdown page with jackpot and published winner payouts *when that page exists*.
  This opens online; payout figures are NOT yet imported into the phone database and
  are NOT used to claim or calculate a monetary win. Confirm tickets and cash prizes
  through the operator or a retailer. The archive publisher is independent of LottoLab.
- Home also matches saved dated ticket numbers against cached draw results automatically.\n  A missing draw is shown as unknown; main, bonus and PowerBall matches are\n  displayed without claiming a verified prize or cash payment.\n- Result notifications remain a single understandable opt-in, since newer Android
  versions require explicit notification permission. Selecting it enables notifications
  for all currently supported games rather than showing one checkbox per game.

Further production work: obtain permission and a documented provider feed for signed prize
division data (jackpot, rollover, draw ID, division match definition, winners and payout
amount), implement audited import and non-destructive offline storage, then reconcile each
result against the operator before adding monetary prize estimates.


## 0.7 — BetTip draw sync (key-free, attributed)

The app is now wired to the public [BetTip Draws API](https://bettip.co.za/api/).
The documented free data licence requires a visible BetTip link. LottoLab remains
an independent research app and does not claim official operator certification.

- Default automatic refresh uses `GET /api/v1/lotto/latest.json`, over HTTPS,
  typically on Android's network-constrained six-hour WorkScheduler cycle and on
  opportunistic catch-up after extended offline periods.
- `GET /api/v1/lotto/draws.json` imports the available supported historical
  game eras once on first successful online sync and then roughly every seven
  days, to avoid hammering the archive service. A manual Refresh Now also
  downloads this archive when it is due.
- Only records with valid actual published draw dates, valid number ranges,
  correct game identities, matching rule eras and the required special balls are
  accepted. Provider 'official' or 'verified' flags are *not* independent verification.
- When the latest feed succeeds but the larger archive fails, the latest facts
  remain saved and history is retried later. If all network work fails, existing
  offline results and saved selections are untouched.
- Current and historical `powerball-plus` labels map respectively to PowerBall
  XTRA (after 1 June 2026) and archive-only PowerBall Plus (before 1 June 2026).
  LOTTO Plus 2, LOTTO 5 MAX and retired Daily Lotto Plus are mapped by era.
- Previously automatically selected unofficial operator-site endpoints are
  migrated to BetTip, while explicitly configured third-party approved HTTPS
  feeds remain intact. There is no account, API key or manual source setup.
- Linked BetTip source attribution appears in the UI and the app retains existing
  native screens, database, user tickets, backups and package ID.
- BetTip additionally documents `GET /api/v1/lotto/payouts.json`. Its prize
  breakdown structure has *not yet* been validated and therefore financial
  payouts are not silently imported or calculated. Historical ticket comparisons
  remain factual number matches with no verified monetary payout.
- At the time of integration, BetTip's documented latest payload listed
  `lotto`, `lotto-plus-1`, `powerball`, `powerball-plus` (PowerBall XTRA),
  and `daily-lotto`. Game coverage may change. The app will never invent
  absent Lotto 5 Max draws or pretend missing published results are losses.

Automated parser unit tests include the observed public BetTip latest response
shape, date/cutover checks, duplicates and rejection of malformed or future
results. Full Android emulator QA and live network availability are separately
subject to the GitHub Actions workflow; this code change alone is not a proven
published APK.
