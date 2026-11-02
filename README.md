# PocketSurf

<img src="branding/pocketsurf-mark.svg" width="96" alt="PocketSurf PS icon">

**A free, controller-first Android browser for gaming handhelds, with full touch support.**

PocketSurf by **SilasDV** brings desktop-style tabs, a compact top address bar, D-pad navigation, mouse mode and right-stick scrolling to devices such as the Retroid Pocket 6. It uses the installed Android System WebView.

**Version 1.2.9 · Android 8.0+ · No root or Shizuku required**

[Download APK](https://github.com/silasdv/PocketSurf/releases/download/v1.2.9/PocketSurf-1.2.9.apk) · [Release notes](docs/RELEASE-1.2.9.md) · [Changelog](CHANGELOG.md)

## Install or update

1. Download **PocketSurf-1.2.9.apk** from the [official release](https://github.com/silasdv/PocketSurf/releases/tag/v1.2.9) and install it.
2. Choose a search engine and optionally pin websites in the two-page setup. Google is the default. Setup can be skipped and reopened in About.
3. Use touch or a controller. Open the top-right gear or press Start for Settings; remap actions under Controller and adjust the pointer under Mouse.
4. Choose a theme and accent, optionally enable the bookmarks bar, and use About to open Android's default-browser chooser.

**Earlier local test APKs:** this public release uses a dedicated release key. Android cannot update a development-signed installation with it. Export bookmarks/settings through **Settings > Backup and restore** before uninstalling a test build; uninstalling clears its data. Restore the JSON after installing the release. Cookies, history and open tabs are not included in that backup. Future public releases must retain this signing identity.

A launcher start opens a fresh tab while retaining existing tabs. Share and Reload stay visible and dimmed on the new-tab page.

## Features

- Surf Bar: nine grouped actions beside the address field; Y selects New Tab and the D-pad/left stick reaches the other actions. Up/down stays in the chrome; Y or Back leaves it. Leaving it restores the previous page target. The percentage between Downloads and New Tab opens page zoom; Y still starts on New Tab.
- Compact layout-zoom slider with Reset to 100%, touch control and 5% controller steps. The CSS viewport reflows at the selected zoom.
- Persistent animated download status with known-size progress and a completion marker.
- Thin accent page-loading bar and hidden Android status bar for more webpage space.
- Full touch operation, D-pad/left-stick selection and right-stick page/panel scrolling.
- Permanent desktop browsing with a desktop user-agent, supported desktop client hints and a wide viewport; no mobile-mode toggle.
- Separate mouse navigation with native hover/click, analog stick speed, edge scrolling, a speed slider and optional acceleration. Pointer Size sits below movement speed with an always-visible preview. Mouse Appearance offers five original shapes and independent fill/outline choices in a compact pop-up.
- Basic Android Custom Tabs provider for compatible app links, with a separate browser window and shared downloads.
- Remappable actions, Nintendo SNES colors or vector PlayStation symbols and compact context-aware hints.
- Progressive title truncation and responsive compact tabs with stable favicon/close insets; overflow uses a +N entry.
- Adaptive tab strip, visual previews, a horizontal Open Tabs view beyond three cards, Recently Closed, Undo and Close All.
- Inline address/search editing, suggestions, several search providers and custom search templates.
- One bookmark collection, six pinned start-page slots, an optional scrolling bookmarks bar and searchable management.
- Searchable history grouped by day, Find in Page, Reader view, reading zoom and Android sharing.
- An internal downloads list for normal HTTP(S) downloads and supported blob/data saves through Android's document picker.
- Limited URL-filter adblocking, optional EasyList updates, per-site controls and necessary-only cookie choices on recognized banners.
- Private sessions on Android 9+ with a separate WebView profile, and explicit website camera/microphone requests.
- Light, Dark, Auto and OLED, seven independent accents, text/touch settings and local JSON backup/restore.
- Original flat P/S icon with adaptive and Android 13 monochrome launcher layers.

## Default controls

| Input | Action |
| --- | --- |
| D-pad / left stick | Select page elements and interface controls |
| Right stick | Scroll the page or active scrollable panel |
| A / Cross | Activate; hold on a page target for context options |
| B / Circle | Page back; close the current menu/keyboard first; exit with zero tabs |
| Android Back | Page back; return through menus/native screens; close keyboard first |
| X / Square | Close current tab; close selected card in Open Tabs |
| Y / Triangle | Enter/leave Surf Bar; initially select New Tab |
| L / R | Page back/forward |
| L2 / R2 | Previous/next tab; stop at the ends |
| L3 | Switch mouse/element navigation; test the pointer in Mouse Settings |
| R3 | Page zoom choices; D-pad left/right adjusts the percentage |
| Start | Settings |
| Select | Menu |

Hints follow the current mapping and context. The Surf Bar opens tab overview and other actions; the top tab strip and page-history arrows remain touch targets with controller shortcuts, not D-pad stops. Mouse entry centers the pointer; returning to element mode adopts the target underneath it. The small touch × is not a separate controller focus target. Firmware can report different physical button codes; choose labels and remap accordingly.

## Bookmarks and sessions

Pinned start-page entries are bookmarks, not a separate app collection. **All bookmarks** opens the complete collection; additional bookmarks-bar items scroll horizontally.

Tab closing is immediate, with Undo/Recently Closed. Normal sessions restore metadata, reading position and bounded URL-only GET history, not arbitrary unsent forms, POST requests, logins or exact JavaScript/media state. Only three live WebViews are retained, considering recent/form/media/download activity.

**Current limit:** bookmarks/imports are capped at 500. Adding beyond 500 can discard an older bookmark and its pin; keep important exports and stay below that limit. History/recently closed lists are bounded too. Sync, bookmark folders and a password manager are not included.

## Compatibility and privacy

Keep Android System WebView updated: it supplies the engine, codecs, DRM and website compatibility. Frames, closed shadow roots, canvas controls and custom editors may require mouse or touch. Adblocking supports only a subset of filter syntax; cookie helpers do not guarantee rejection on every site. Per-site exceptions are available.

Private mode requires Android 9+. Temporary private profile files can remain after a crash until cleanup. Explicitly saved files/bookmarks, clipboard and shares can remain; private mode is not network anonymity.

No advertising or analytics are included. Browsing contacts the sites/search engine you use; favicons come from the site's origin and optional filters from EasyList. Camera/microphone prompts appear only after a website request. [Privacy](PRIVACY.md) · [Third-party notices](THIRD_PARTY_NOTICES.md).

Automated checks cover native Android simulations and separate desktop fixtures. They do not certify every handheld, installed keyboard, 120-Hz display, media site or OS document provider. [Validation](VALIDATION.md) · [Device checks](DEVICE-TEST.md).

## Build and contribute

Use JDK 17, Android SDK platform 35 / Build Tools 35.0.0 and the included Gradle 8.11.1 wrapper:

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
node --test app/src/test/js/navigation.test.cjs app/src/test/js/cookie-consent.test.cjs app/src/test/js/desktop.test.cjs tools/test-generated-download.mjs
```

Windows uses `gradlew.bat`. Set `ANDROID_HOME` or `local.properties`. Release signing needs your own configuration; without it, a verification build can be unsigned. [Release signing](docs/RELEASING.md) · [Contributing](CONTRIBUTING.md). Never commit keys, passwords, caches or local SDK paths.

## Vibecoded and support

PocketSurf is **vibecoded**: substantial AI-assisted implementation/documentation through OpenAI Codex, guided and iteratively tested by **SilasDV**. Automated tests cover selected behavior; human review and contributions are welcome.

Free and open source under [MIT](LICENSE). Optional tips: **[ko-fi.com/silasdv](https://ko-fi.com/silasdv)**. All features remain free. [More projects](https://github.com/silasdv).
