# PocketSurf 1.2.0

**First consolidated public release: a free, controller-first Android browser for handhelds, with full touch support.**

[Download APK](https://github.com/silasdv/PocketSurf/releases/download/v1.2.0/PocketSurf-1.2.0.apk) · [Installation and controls](https://github.com/silasdv/PocketSurf#install-or-update)

## Included

- D-pad/left-stick navigation, right-stick scrolling, separate mouse mode with hover/click, speed/acceleration controls and remappable Nintendo/PlayStation actions.
- Compact desktop-style tabs/address controls, adaptive overflow, visual horizontal Open Tabs, immediate close with Undo and Recently Closed.
- Search/setup, pinned bookmarks and bookmarks bar, searchable day-grouped history, sharing and managed HTTP(S)/supported blob/data downloads.
- Find in Page, Reader/zoom, per-site protection, limited adblocking and necessary-only choices on recognized cookie banners.
- Private sessions on Android 9+, explicit site media permission requests and local JSON bookmark/settings backup.
- Light, Dark, Auto and OLED with independent accents, corrected native/platform contrast and the new flat P/S adaptive/monochrome icon.

## Install/update

Android 8.0+; no root or Shizuku. Private mode requires Android 9+. This release uses a dedicated production signing identity. Back up and uninstall development-signed test builds first; uninstalling removes their data. Settings > Backup and restore exports bookmarks/preferences, excluding cookies, history and tabs.

## Known limits

Bookmarks/imports are capped at 500; adding beyond that can discard older entries/pins. Export important entries and remain below the limit. Cookie helpers, filter syntax, custom editors/frames and media/DRM compatibility have documented limits and depend on the installed WebView. Undo does not restore arbitrary form/POST/JavaScript state. Sync and password management are not included.

277 Android/JVM tests and 54 JavaScript tests passed. Release APK/App Bundle build and signing were verified. Host simulations/desktop checks do not certify every physical controller, IME, 120-Hz display, media site or Android document provider; see VALIDATION.md.

Signed APK/AAB, source ZIP and SHA256SUMS.txt are attached. Source excludes private keys, local caches/configuration and personal development material.

MIT; free and open source. Vibecoded with substantial AI assistance, guided by SilasDV. Optional support: https://ko-fi.com/silasdv.
