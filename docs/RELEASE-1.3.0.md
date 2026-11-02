# PocketSurf 1.3.0

- Give the selected Surf Bar action a subtle accent focus tile, thin border and slight icon scale. Keep control geometry stable and use consistent selected-choice checkmarks across Settings.
- Return from History, Downloads, Settings and other native screens to the same Surf Bar action. Leaving the bar restores the original page target or pointer. B closes a new-tab page when no menu, keyboard or Surf Bar is active.
- Persist layout zoom globally across all tabs, reloads, restored sessions and app restarts; include it in settings backups. Reset still returns to 100%.
- Animate Menu closing to the left at constant opacity. Remove pending popup layers when changing navigation mode to prevent stale gray scrims.
- Replace large L2/R2 circles with compact shoulder labels and separate arrows matching the bottom legend; halve the chrome gaps to leave more webpage space.
- Enlarge Open Tabs cards to show roughly two previews and part of the next on wide displays. Keep the current-page badge separate from the visible URL, focus the whole card and keep unavailable footer actions visibly dimmed.
- Organize Settings into Browsing, Appearance & accessibility, Controls, Privacy & protection, Backup & restore, and About & help. Use compact choice grids, consistent focus styling and separate controller action/directional mappings; add spacing between mouse controls.
- Replace the smooth Rock hand with an original pixel metalhand cursor. No EMP cursor asset is included.

Signed with the existing public release identity for direct upgrades over public 1.2.x releases. Validation: 345 Android/JVM tests and 63 JavaScript tests passed; signed release builds and lint completed. Real WebView behavior and physical-device frame-rate acceptance remain device checks.
