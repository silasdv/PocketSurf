# Changelog

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
