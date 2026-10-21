# LeadRadar USA Android Companion v0.1
Offline, locally functional lead manager. Save permitted manually discovered US opportunities, filter by service and seven-day freshness, search, note, bookmark and track contact stages.

**It is not yet a Craigslist scanner.** It cannot scan websites, receive Craigslist alerts, synchronize to Neon, or perform AI scoring. The backend Next.js code remains a separate web project. Local data stays inside the APK and is deleted if uninstalled or app data is cleared. No automatic downloads/scraping or messaging occur.

To compile: `gradle -p leadradar-android assembleDebug`; APK at `leadradar-android/app/build/outputs/apk/debug/app-debug.apk`. Debug signing is meant for device testing, not store distribution.
