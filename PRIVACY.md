# Privacy

PocketSurf includes no advertising, analytics or telemetry service. It is a network-enabled browser: the sites, search engine and downloads you choose receive ordinary network requests. Favicon requests use each site's origin; optional filter updates contact easylist.to. No third-party favicon service is used.

While the address editor is open, typed queries are sent to Google for autocomplete before submission. Google also receives submitted searches. Recent searches are stored locally during normal browsing.

Normal history, bookmarks, mappings, preferences, tab metadata and reading position are local. Website cookies/storage are managed by Android System WebView. JSON backup uses an explicit preference/bookmark allow-list, excluding history, tabs, cookies and passwords.

Private browsing on Android 9+ uses a separate process/WebView profile. Temporary profile files may survive a crash until cleanup. Explicitly saved bookmarks/files/exports, clipboard and shares can remain. Private mode does not hide activity from websites, networks or the device owner.

INTERNET supports browsing. Camera/microphone runtime access is requested only after an HTTPS site requests it and you approve its prompt. Only approved resources are granted. No broad storage permission is needed; supported generated downloads use Android's document picker.

Sharing a page or opening a saved file passes selected content to the chosen Android handler. GitHub/Ko-fi links are optional. Certificate errors are rejected; mixed content is disabled. First-party and third-party cookies are enabled, including in the separate private profile. Embedded third parties can store cross-site identifiers. Cookies are flushed to storage when the app pauses. Plain HTTP is unencrypted.

Adblocking/cookie helpers are limited and do not promise complete tracking prevention. Keep WebView updated and use per-site exceptions if needed.
