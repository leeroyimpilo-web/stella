# MP3 Dork for Android

A native Android app for Google MP3 searches, source browsing, direct audio downloads, saved links and offline listening. Android 8.0 and newer. Package: `za.co.mp3dork`.

## Features

- Four editable Google query presets: direct MP3, indexed folders, open audio and Internet Archive.
- Exact phrases, domain restrictions and excluded words.
- An in-app WebView for real Google results and source pages; external-browser fallback.
- Tap a detected MP3, long-press a link, paste/share a URL, or list MP3 links on the current page.
- Tap an MP3 link, choose a collected link, paste a direct URL or retry to queue a download immediately. No audio verification request or blocking download dialog.
- Long-press for optional preview, save, copy and source-page actions. Previews use the player directly.
- Android DownloadManager with live progress, cancellation, retry, duplicate checks, notifications and optional Wi-Fi-only downloads.
- Public Downloads/MP3Dork or Music/MP3Dork storage. Unique filenames prevent accidental overwrite.
- Downloaded/imported audio library, saved URLs, saved searches and history.
- Foreground-service audio playback with a media session, notification controls, audio focus, seeking and headphone-disconnection handling.
- Share files and rename their display titles in the library. Library renaming preserves the on-disk filename.

## Behaviour

Google results are rendered in the WebView, not scraped into a fabricated results list. The user completes any Google consent or CAPTCHA normally. Sites that reject embedded browsers can be opened externally, and their audio URLs shared back to the app. Only download URLs actually offered by the source are used. The app does not convert video, bypass authentication or remove DRM.

Download retries/resumption are managed by Android and depend on source support. Manual pause/resume is not exposed. Saved links require a connection; completed downloads and imported documents can be played offline. No account or app backend is required. Local records are not uploaded. Search requests, visited pages, streaming and downloads contact their respective hosts.

## Build

Use Java 17, Gradle 8.9 and Android SDK 35:

```sh
gradle :app:testDebugUnitTest :app:assembleDebug
```

The repository workflow `.github/workflows/mp3-dork-apk.yml` runs core tests and an API 35 emulator smoke test. The smoke test checks navigation, genuine MP3 validation, rejection of disguised HTML, a real DownloadManager transfer, immediate download without a probe, responsive navigation with a stalled source, record persistence and offline/background playback against a controlled fixture server. It then verifies the APK certificate and uploads `MP3Dork-1.1.apk`.

The artifact is development-signed for direct installation. The signing key is retained in the workflow cache, not in source control. A separately secured release key is needed for formal distribution and a guaranteed signing identity after cache expiry.
