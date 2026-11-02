# PocketSurf 1.3.1

- Replace the Settings category landing page and nested option screens with one fixed dashboard: Browsing, Appearance, Controller, Mouse, Privacy and About. The category rail, section heading and footer stay in the same positions.
- Use consistent control surfaces, row heights, spacing, focus tiles and selected-choice checkmarks. Change switches, sliders, theme/accent choices and pointer previews directly in place. Custom search and start-page editors appear inline; unfinished addresses are not saved.
- Keep controller remapping, physical-button selection, recording, collision confirmation and privacy confirmations on the same Settings surface. B returns from controls to the category rail, then to the browser. Restore the selected category/control after theme changes and recreation.
- Show mouse style previews, independent fill/outline choices, actual-size pointer preview and a movement test directly in Mouse settings. L3 toggles the test. Changing fill or shape no longer implicitly changes the outline.
- Keep the Settings dashboard free of app popups, nested pages and vertical scrolling at the validated handheld sizes. Android keyboards, default-app settings and file pickers remain system interfaces.
- Support Page Zoom with both sticks, D-pad and touch. The right stick adjusts the visible slider in 5% steps only while the Page Zoom menu is open; normal webpage scrolling remains unchanged.
- Give the bookmarks bar equal 4dp gaps above and below and let its height follow its content, including larger touch targets.
- Android Back now closes a new-tab page after handling menus, keyboard and Surf Bar first. B has the same new-tab behavior; X still closes tabs directly.

Validation: 356 Android/JVM tests passed; signed APK/App Bundle builds and lint completed. The unchanged JavaScript suite retains 63 passing tests from 1.3.0. Layout renders cover compact 640×360, wide 960×540 and relevant portrait fixtures; physical-device WebView, keyboard and performance acceptance remain device checks. Signed with the existing public release identity.
