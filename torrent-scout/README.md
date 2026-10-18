# TorrentScout

Android torrent metadata search app with built-in Internet Archive search and configurable Torznab-compatible indexers (such as Prowlarr or Jackett).

Features: any-file-type keyword search, category chips (video, audio, software, books, games, other), seeders/size sorting, local favorites, magnet-link handoff to tTorrent or other installed clients, .torrent downloads through Android DownloadManager, and source management.

### Add more search providers

In Sources, enter a Torznab endpoint, a display name, and an API key if needed. The full endpoint should end in the Torznab API path, for example: https://host/api/v1/indexer/1/newznab . HTTP is accepted only for LAN or localhost addresses; HTTPS is recommended. Results depend on connected sources. Not every item has a magnet link or a seed count, and no source can guarantee exhaustive coverage.

### Build

JDK 17, Android SDK 35, Gradle 8.9.

Run from the torrent-scout directory: gradle :app:assembleDebug

GitHub Actions builds the test APK, uploads an artifact, and publishes a Release.

API keys remain in device preferences and are not encrypted. Device backup is disabled. The app doesn't download torrent payloads, bypass trackers, or host files. Use for content you have the right to access.
