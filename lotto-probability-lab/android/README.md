# Lotto Lab for Android

App name: Lotto Lab. Package: za.co.lottolab.probability. Android 8.0+; a current Android System WebView is required.

This wrapper bundles ../dist as APK assets, uses a secure local HTTPS origin, and requests no network or broad storage permission. Numbers, saved selections, coverage workers and PDF generation run offline. Research links open in the external browser. PDF export uses Android's system Save As picker and copy uses the native clipboard. It installs separately from prior lotto applications.

Build: Java 17, Gradle 8.9, Android SDK 35, then `gradle :app:assembleDebug` in this directory. GitHub Actions also verifies the signing certificate and runs an emulator smoke test. The sideload APK is development-signed. Production/Play Store distribution needs a private release signing key; do not publish the development key as a release identity.

PDF export is bridged only from bundled local content. Remote WebView resources are blocked; file/content URL access is disabled. Pending exports are cancelled if the activity/process is destroyed. Browser data and Android app data are separate; selections saved on the website are not transferred automatically.
