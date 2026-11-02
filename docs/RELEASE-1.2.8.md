# PocketSurf 1.2.8

- Repair Surf Bar activation at higher display densities. The Surf Bar and address field now share one height; actions activate on the first press rather than repeatedly reporting a clipped selection.
- Use accent-colored, subtly enlarged Surf Bar icons for controller selection. Add white-on-black circular Y/L2/R2 hints, balanced divider spacing, rounded chrome and smoother tab-position changes.
- Open Menu from the left with constant opacity. Menu icons stay neutral until selected.
- Add a page-zoom percentage between Downloads and New Tab. Touch opens zoom choices; controller left/right adjusts the percentage. Y still initially selects New Tab.
- Add Mouse Appearance as a compact pop-up in Mouse Settings: a 50–220% size slider and Windows-, macOS- and Linux-inspired pointer previews, plus black/white and outlined/plain variants. Appearance is retained in local backup/restore.
- Use permanent desktop browsing for normal, restored, popup, private and Custom Tab pages: desktop user-agent, supported desktop client hints, wide overview and a 1280-CSS-pixel viewport. No mobile-mode toggle. Individual sites can still enforce their own redirects or browser restrictions.
- Fix touch Back from bookmark management when its search field retains focus. Compact bookmark actions and use explicit pages where longer settings cannot fit without scrolling; Mouse Appearance itself is a pop-up.

Signed with the existing public release identity; direct upgrade over 1.2.x. Validation: 327 Android/JVM tests and 59 JavaScript tests passed; signed release builds and lint completed. Physical-device acceptance is not claimed.
