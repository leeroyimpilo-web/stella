# LottoLab 0.3

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

**No result history is bundled, and no fictional results are shown in the installed app.** The empty state guides users to enter published draw numbers, import an archive or explicitly connect to the public lottery website feed. Results are labelled entered/imported or downloaded feed data and must be checked with the operator. A configured feed is not automatically certified as an independently verified result or prize source.

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
