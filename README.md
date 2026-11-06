# PocketSurf

<img src="branding/pocketsurf-app-icon.svg" width="88" alt="PocketSurf">

PocketSurf is an Android browser for landscape gaming handhelds. Browse with a gamepad, switch to a virtual mouse for hover menus, or tap any browser action directly. It runs on Android 8.0 and newer using Android System WebView.

[Download PocketSurf 1.4.0](https://github.com/silasdv/PocketSurf/releases/download/v1.4.0/PocketSurf-1.4.0.apk) · [Changelog](CHANGELOG.md)

## Start browsing

Install the signed APK, then open PocketSurf. Each launcher start adds a new tab and opens the keyboard. Enter an address or search with Google. To update a previous public release, install the APK over the existing app. Android requires matching signing keys for an in-place update.

The Search screen shows recent searches and frequently visited websites. Each website shortcut opens the page you visit most on that site. While you type, PocketSurf sends text to Google for autocomplete. Saved bookmarks and browsing history are stored locally. [Privacy details](PRIVACY.md)

## Controller guide

| Default input | Browsing action |
| --- | --- |
| D-pad / left stick | Move between elements |
| A / B | Open the selection / go back or close the current panel |
| X / Y | Close the current tab / focus the Surf Bar |
| L / R | Go back / forward in page history |
| L2 / R2 | Switch tabs |
| Select / Start | Open the browser menu / Settings |
| L3 / R3 | Toggle the virtual mouse / adjust page zoom |
| Right stick | Scroll the page |

The bottom legend shows actions for the current screen. In Settings you can remap the controls, switch between Nintendo and PlayStation labels, and adjust pointer size, speed and appearance.

## Tabs, bookmarks and privacy

- Switch between page previews in Open Tabs, reopen recently closed tabs and undo accidental closures.
- Pin up to six shortcuts on the new-tab page. All Bookmarks holds the full collection and lets you add a URL or toggle the bookmarks bar.
- Use the Surf Bar for downloads, page zoom, sharing, history and settings. Desktop-style browsing and global page zoom suit a landscape display.
- Choose Light, Dark, Auto or OLED and a separate accent color. New installations start in OLED.
- Use per-site protection settings, manage download files and export bookmarks and settings. Private browsing uses a separate WebView profile on Android 9 and newer.

## Build

Use JDK 17 and Android SDK 35. Run `./gradlew assembleDebug testDebugUnitTest` (`gradlew.bat` on Windows). For a distributable APK, configure your signing key and run `assembleRelease`. [Release signing](docs/RELEASING.md)

Created by SilasDV with OpenAI Codex. [License](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md)
