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

Release verification: 412 Android tests and 60 JavaScript tests passed. The signed release APK installed over the existing Retroid Pocket 6 app and opened the keyboard on cold launch.
