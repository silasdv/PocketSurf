# Changelog

## 1.2.8

- Repair Surf Bar activation at higher display densities. The Surf Bar and address field now share one height; actions activate on the first press rather than repeatedly reporting a clipped selection.
- Use accent-colored, subtly enlarged Surf Bar icons for controller selection. Add white-on-black circular Y/L2/R2 hints, balanced divider spacing, rounded chrome and smoother tab-position changes.
- Open Menu from the left with constant opacity. Menu icons stay neutral until selected.
- Add a page-zoom percentage between Downloads and New Tab. Touch opens zoom choices; controller left/right adjusts the percentage. Y still initially selects New Tab.
- Add Mouse Appearance as a compact pop-up in Mouse Settings: a 50–220% size slider and Windows-, macOS- and Linux-inspired pointer previews, plus black/white and outlined/plain variants. Appearance is retained in local backup/restore.
- Use permanent desktop browsing for normal, restored, popup, private and Custom Tab pages: desktop user-agent, supported desktop client hints, wide overview and a 1280-CSS-pixel viewport. No mobile-mode toggle. Individual sites can still enforce their own redirects or browser restrictions.
- Fix touch Back from bookmark management when its search field retains focus. Compact bookmark actions and use explicit pages where longer settings cannot fit without scrolling; Mouse Appearance itself is a pop-up.

Signed with the existing public release identity; direct upgrade over 1.2.x. Validation: 327 Android/JVM tests and 59 JavaScript tests passed; signed release builds and lint completed. Physical-device acceptance is not claimed.


## 1.2.7

- Add the Surf Bar beside the address field: Reload, History, Downloads, New Tab, Tabs, Share, Menu and Settings. Press Y to select New Tab, then use the D-pad and A. Move left to reach the address field.
- Keep the top row for tabs, with compact L2/R2 edge hints, more space for titles and a touch close button only on the active tab.
- Restore the previous page selection when leaving the Surf Bar, including after a native screen. Mouse position is retained during a Surf Bar roundtrip; navigation invalidates stale focus.
- Update controls: A Open, B Back, X Close Tab, Y Surf Bar, Select Menu, Start Settings. L/R still navigate history; Android Back now follows page history. Existing remapped physical bindings are retained.
- Center the pointer on each mouse-mode entry. Switching to elements selects the interactive target beneath the pointer without clicking it.
- Refresh the context-aware legend, with new L3/R3/RS stick icons, left/right grouping and a compact two-line layout on narrow screens.

Signed with the existing public release identity; direct upgrade over 1.2.x. Automated validation: 320 Android/JVM tests and 59 JavaScript tests passed; release APK/App Bundle and lint completed. Physical handheld/website acceptance is not claimed.


## 1.2.6

- Compact About actions and pad mouse switches; keep the larger address shortcut visible and add a paper-plane submit action with Select support.
- Add basic Android Custom Tabs support so compatible apps can open embedded links in PocketSurf. These windows use PocketSurf navigation and downloads, preserve the normal tab session, and can be closed to return to the opening app.
- Scale mouse movement with stick intensity: slight deflection moves slowly; full deflection retains the previous maximum speed. The existing speed slider and optional acceleration remain available.
- Add a short fade when Reload/Stop or the tab-count icon changes, without moving its button or touch target.
- Use the selected accent for webpage controller highlights, text selection and native selection highlights.


## 1.2.5
- Preserve download filename suffixes when servers report a generic MIME type, including APK, archive and ROM files.
- Resolve APK opening to Android's package installer and declare the user-confirmed installation capability.
- Reject unprompted new windows; keep user-opened windows in separate tabs with a return path to their opener.
- Preserve native popup requests, including POST, and the source-page referer for popup downloads.

## 1.2.4
- Fix controller-confirm activation in mouse mode: send paired native tap events instead of incomplete mouse button events.
- Keep real mouse hover and restore it after activation without changing the input mode.
- Add an end-to-end controller-input regression for the native WebView event path.

## 1.2.3 — 1 November 2026

- Distribute available width across all visible tabs in title, compact and overflow modes.
- Remove abrupt fixed-minimum-width shrinkage and trailing unused space.
- Allocate rounding remainders to individual tabs; keep favicon/close insets stable.

## 1.2.2 — 1 November 2026

- Keep readable truncated titles for longer before switching to compact tabs.
- Preserve fixed favicon and close-button insets in both title and compact modes.
- Use smaller fixed-width compact tabs instead of wide empty cards, then +N overflow.
- Account for actual tab-row padding, hint/close controls and New Tab width when planning available space.

## 1.2.1 — 1 November 2026

- Open and select a fresh tab on launcher start/re-entry, retaining existing tabs.
- Avoid extra tabs on Activity recreation and when directly opening an external link.
- Keep Share and Reload visible, disabled and dimmed on the new-tab page; enable them again for websites.

## 1.2.0 — 1 November 2026

First consolidated public release, following local development through 1.1.9.

- Controller-first WebView browsing with full touch support, remapping, separate mouse navigation and compact context-aware hints.
- Adaptive tabs, visual horizontal overview, immediate close with Undo/Recently Closed, searchable history/bookmarks and optional bookmarks bar.
- Search/setup, sharing/downloads, Find in Page, Reader/zoom, limited adblocking/cookie helpers, private sessions and website media permissions.
- Corrected Light/Dark/Auto/OLED contrast, independent app/platform accents, draft-preserving forms and native keyboard/controller recovery.
- Original flat P/S monogram with adaptive/monochrome launcher layers.
- Dedicated release signing, APK/App Bundle/source/checksums, build/privacy/contribution documentation.

Current compatibility/storage limits are documented in README.md. Local milestone labels were not separate public releases.
