# PocketSurf 1.2.9

- Keep Surf Bar focus inside the chrome when pressing up/down. Both D-pad and left stick navigate its actions, including while mouse mode is enabled; Y or Back returns to the previous page target. Remove the duplicate Y badge and add spacing/division before Menu and Settings.
- Recover stale native/web editing states without toggling mouse mode. Do not treat an unactivated webpage autofocus as a permanent controller editing session; protect deliberate input and keyboard opening.
- Replace the zoom list with a compact slider and Reset to 100%. Adjust in 5% steps with the controller or directly by touch. Layout zoom changes the CSS viewport width and scale so responsive content can reflow, rather than only magnifying a fixed viewport. Add a small Zoom caption above the Surf Bar percentage.
- Move Pointer Size below movement speed, with an always-visible preview. Mouse Appearance offers five original vector shapes: Classic, Compact, Pixel, Rock hand and Diamond. Choose white/black/accent fill and none/white/black outline independently; keep focus on the selected choice and remove preview backing boxes.
- Show an animated ring while downloads run, with aggregate progress where sizes are known and an indeterminate ring otherwise. Use blue, or green with a blue accent. Completed downloads keep a colored icon until Downloads is opened; failed/cancelled downloads do not count as successful completion.
- Shrink L2/R2 circles, soften their gray borders and center the labels independently of the small arrows. Animate tab widths and close-glyph space together, without translating tabs over each other.
- Keep Surf Bar backgrounds clear during selection; use icon accent and subtle scaling. Draw an actual stop square while loading, distinct from the tab-close cross.
- Remove Loading percentages from the address field. Show a thin accent progress bar below the chrome, balance the vertical gaps and hide the Android status bar while retaining navigation/keyboard handling.

Signed with the existing public release identity; direct upgrade over 1.2.x. Validation: 337 Android/JVM tests and 63 JavaScript tests passed; signed release builds and lint completed. Physical-device website behavior and frame-rate acceptance remain device checks. No forward-edge gesture or overlay suppression has been added.
