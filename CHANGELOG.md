# Changelog

## 1.4.2 · 10 November 2026

- Show recent searches immediately when opening an empty address field, and restore them when clearing a typed query.
- Discard autocomplete responses whose query no longer matches the address field.
- Enable first-party and third-party cookies in browser WebViews. Third-party cookies also allow embedded services to store cross-site identifiers; this changes the previous cookie policy.
- Flush cookies to storage when the activity pauses.
- Add regression coverage for recent-search display and cookie acceptance.

Existing display-scaling limitations documented in 1.4.1 remain applicable.

## 1.4.1 · 7 November 2026

### Display scaling and TV integration
- Scale the native browser interface uniformly to the available application window, including text, icons, tabs, settings, menus and search controls.
- Support proportional layouts across 16:9 and 4:3 windows. Additional vertical space remains available on 4:3 screens.
- Add Android TV launcher discovery, a TV banner and D-pad-center activation. Touchscreen hardware is optional.
- Preserve search text, selection, keyboard dismissal and the existing WebView when window dimensions, density or font settings change.
- Keep website rendering, page zoom, system keyboards and pointer/scroll speed independent of native interface scaling.
- Improve tab width rounding and settings control sizing at fractional scales. Window resizing and favicon/title updates no longer trigger tab-selection animations.
- Prevent temporary keyboard insets from changing the global interface scale.

### Validation and known limitations
- Automated coverage includes 16:9 and 4:3 windows, density and font variations, TV configuration, keyboard occlusion and live resizing.
- The development agent tested the Retroid Pocket 6 at native resolution and temporary 4:3 and 1440p overrides.
- With Inlay after forced resolution changes, some captures showed black regions while the keyboard was open. The cause remains unresolved. Those override/keyboard combinations are not confirmed visually correct.
- Physical Android TV, external-display and full controller compatibility testing remain outstanding.

Signed with the existing public-release key for in-place updates.

## 1.4.0 · 6 November 2026

### Search and navigation
- Launching PocketSurf opens a fresh tab alongside the existing session. The address editor requests the Android keyboard once its window can accept input.
- The dedicated Search screen combines recent submitted searches, Google autocomplete and shortcuts to frequently visited sites. Shortcuts lead to the most-used page on each website, with the latest visit breaking ties.
- Suggestions update after a short typing pause. New rows fade in while retained rows preserve focus. Superseded requests are discarded and repeated queries can reuse cached suggestions.
- Google handles searches, including installations that previously selected another engine. Back and Forward hints remain in place and reflect the current tab’s navigation history.

### Bookmarks and browsing
- Pin a suggested site or enter a website address from All Bookmarks. Suggestions omit sites already saved and can use a frequently visited subpage. Saved bookmarks remain separate entries.
- The bookmarks bar is enabled by default and can stay visible on websites and new tabs. Controller navigation connects the address field, bookmarks bar and pinned shortcuts.
- The first-run setup has been removed. Browsing settings group the default-browser action, page zoom and bookmark controls.

### Appearance and input
- OLED is the default theme for new installations. Theme previews, an expanded accent palette and clearer controller badges improve visibility in light and dark themes.
- Settings use grouped controls for appearance, privacy, button mapping and pointer customization. Directional choices and sliders enter edit mode before controller adjustments.
- Pointer styles include a pixel-art rock hand, configurable fill and outline, size and a dedicated movement test.
- Updated launcher and interface branding, smoother selection transitions and an accent-colored edge on the slide-in menu.

## 1.3.1

- Replace the Settings category landing page and nested option screens with one fixed dashboard: Browsing, Appearance, Controller, Mouse, Privacy and About. The category rail, section heading and footer stay in the same positions.
- Use consistent control surfaces, row heights, spacing, focus tiles and selected-choice checkmarks. Change switches, sliders, theme/accent choices and pointer previews directly in place. Custom search and start-page editors appear inline; unfinished addresses are not saved.
- Keep controller remapping, physical-button selection, recording, collision confirmation and privacy confirmations on the same Settings surface. B returns from controls to the category rail, then to the browser. Restore the selected category/control after theme changes and recreation.
- Show mouse style previews, independent fill/outline choices, actual-size pointer preview and a movement test directly in Mouse settings. L3 toggles the test. Changing fill or shape no longer implicitly changes the outline.
- Keep the Settings dashboard free of app popups, nested pages and vertical scrolling at the validated handheld sizes. Android keyboards, default-app settings and file pickers remain system interfaces.
- Support Page Zoom with both sticks, D-pad and touch. The right stick adjusts the visible slider in 5% steps only while the Page Zoom menu is open; normal webpage scrolling remains unchanged.
- Give the bookmarks bar equal 4dp gaps above and below and let its height follow its content, including larger touch targets.
- Android Back now closes a new-tab page after handling menus, keyboard and Surf Bar first. B has the same new-tab behavior; X still closes tabs directly.

Validation: 356 Android/JVM tests passed; signed APK/App Bundle builds and lint completed. The unchanged JavaScript suite retains 63 passing tests from 1.3.0. Layout renders cover compact 640×360, wide 960×540 and relevant portrait fixtures; physical-device WebView, keyboard and performance acceptance remain device checks. Signed with the existing public release identity.


## 1.3.0

- Give the selected Surf Bar action a subtle accent focus tile, thin border and slight icon scale. Keep control geometry stable and use consistent selected-choice checkmarks across Settings.
- Return from History, Downloads, Settings and other native screens to the same Surf Bar action. Leaving the bar restores the original page target or pointer. B closes a new-tab page when no menu, keyboard or Surf Bar is active.
- Persist layout zoom globally across all tabs, reloads, restored sessions and app restarts; include it in settings backups. Reset still returns to 100%.
- Animate Menu closing to the left at constant opacity. Remove pending popup layers when changing navigation mode to prevent stale gray scrims.
- Replace large L2/R2 circles with compact shoulder labels and separate arrows matching the bottom legend; halve the chrome gaps to leave more webpage space.
- Enlarge Open Tabs cards to show roughly two previews and part of the next on wide displays. Keep the current-page badge separate from the visible URL, focus the whole card and keep unavailable footer actions visibly dimmed.
- Organize Settings into Browsing, Appearance & accessibility, Controls, Privacy & protection, Backup & restore, and About & help. Use compact choice grids, consistent focus styling and separate controller action/directional mappings; add spacing between mouse controls.
- Replace the smooth Rock hand with an original pixel metalhand cursor. No EMP cursor asset is included.

Signed with the existing public release identity for direct upgrades over public 1.2.x releases. Validation: 345 Android/JVM tests and 63 JavaScript tests passed; signed release builds and lint completed. Real WebView behavior and physical-device frame-rate acceptance remain device checks.


## 1.2.9

- Keep Surf Bar focus inside the chrome when pressing up/down. Both D-pad and left stick navigate its actions, including while mouse mode is enabled; Y or Back returns to the previous page target. Remove the duplicate Y badge and add spacing/division before Menu and Settings.
- Recover stale native/web editing states without toggling mouse mode. Do not treat an unactivated webpage autofocus as a permanent controller editing session; protect deliberate input and keyboard opening.
- Replace the zoom list with a compact slider and Reset to 100%. Adjust in 5% steps with the controller or directly by touch. Layout zoom changes the CSS viewport width and scale so responsive content can reflow, rather than only magnifying a fixed viewport. Add a small Zoom caption above the Surf Bar percentage.
- Move Pointer Size below movement speed, with an always-visible preview. Mouse Appearance offers five original vector shapes: Classic, Compact, Pixel, Rock hand and Diamond. Choose white/black/accent fill and none/white/black outline independently; keep focus on the selected choice and remove preview backing boxes.
- Show an animated ring while downloads run, with aggregate progress where sizes are known and an indeterminate ring otherwise. Use blue, or green with a blue accent. Completed downloads keep a colored icon until Downloads is opened; failed/cancelled downloads do not count as successful completion.
- Shrink L2/R2 circles, soften their gray borders and center the labels independently of the small arrows. Animate tab widths and close-glyph space together, without translating tabs over each other.
- Keep Surf Bar backgrounds clear during selection; use icon accent and subtle scaling. Draw an actual stop square while loading, distinct from the tab-close cross.
- Remove Loading percentages from the address field. Show a thin accent progress bar below the chrome, balance the vertical gaps and hide the Android status bar while retaining navigation/keyboard handling.

Signed with the existing public release identity; direct upgrade over 1.2.x. Validation: 337 Android/JVM tests and 63 JavaScript tests passed; signed release builds and lint completed. Physical-device website behavior and frame-rate acceptance remain device checks. No forward-edge gesture or overlay suppression has been added.


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
