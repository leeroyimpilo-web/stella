# LottoLab 0.2 validation

Checked in the local build environment on 15 October 2026.

- Android application and instrumentation sources compile against SDK 36.
- Standard Gradle unit tests, lint, debug APK and instrumentation APK tasks complete successfully.
- 36 unit tests pass: 16 core tests and 20 tool/rule/storage-model tests; no failures or errors.
- Android lint reports 0 errors and 6 warnings in the retained backup configuration/vector view helpers.
- APK packaging and v2/v3 signing verification pass for the local test build.
- Manifest retains `za.co.lottolab.studio`, raises versionCode to 2, targets SDK 36 and supports SDK 26 upward.

The native device suite is implemented for version-1 migration, ticket checks, history search, six-line generation, saving, PDF sharing, backup/restore, immutable snapshots, recorded spending, correction history, offline restart and narrow-screen/large-font layouts. The local emulator did not complete startup within 10 minutes without hardware acceleration, so these screen journeys did not run. Device verification remains pending in the existing CI emulator workflow; no device pass is claimed here.

A local verification build uses a separate test signer. The first approved CI run built successfully but stopped at the original-certificate check: the 0.1 signing cache had never been saved because its path did not exist. The corrected workflow explicitly creates and caches a development key before tests and checks the APK against it. The 0.2 APK cannot update an installed 0.1 preview in place; it requires a fresh installation or the unavailable original private key. Existing version-1 rows and preferences are exercised by the native migration suite.

No live official draw feed or verified prize payout integration is included. Data-dependent tools use user-entered/imported records or an explicitly configured feed and display that limitation.
