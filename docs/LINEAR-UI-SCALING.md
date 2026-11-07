# Unified linear UI scaling — 7 November 2026

## Design and formula

Reference viewport: **832 × 468 logical dp**, the 16:9 handheld design corresponding closely to the Retroid Pocket 6's approximately 833 × 468 dp window.

Let W and H be the measured application-window dimensions after visible system-bar and cutout insets, in pixels, and D its real Android density:

```
widthDp = W / D
heightDp = H / D
scale = min(widthDp / 832, heightDp / 468)
uiDensity = D * scale
```

There is no maximum scale and no clamp to 1. Smaller windows scale down; larger logical viewports scale up. One uniform factor applies to both axes, including text, icons, controls, chrome, settings, popups, circles, margins, padding, radii and outlines. Real Android density is applied once. Native unit conversion retains the exact continuous floating-point density. Android's integer density-DPI is used only to select resource buckets. Final integer-pixel layout dimensions still require rounding; there are no device-specific scaling tiers.

At constant physical window bounds, changing Android display density changes the reported scale inversely: the resulting native pixel geometry remains proportional to the window. Font accessibility scaling remains separate and is applied once by Android SP conversion. Geometry measurement uses the same SP conversion as native text, including Android 15 font scaling. Favicon fallback letters use explicit pixel sizes inside their icon artwork.

## Architecture

BrowserUiScale owns private resources/context for the browser's native UI. Native Views, themed defaults, custom components and dialogs use that context; Activity/application/system resources are unchanged. The WebView is constructed with the original Activity context and keeps its ordinary density, CSS viewport and page zoom. Android keyboards, permission dialogs, file pickers and other system UI retain system sizing.

On API 30+, current WindowMetrics supplies app-window bounds and actual system/cutout insets. The IME is excluded from the scale calculation. Stable non-IME system insets are retained throughout Search Mode and its closing transition, preventing transient IME/system-bar visibility from restarting search; actual IME insets and the existing visible-window-frame fallback still reduce search layout space. API 26–29 use app-display metrics.

The native context exposes dimensions in scaled design units for flexible layout decisions. Containers fill the entire real window: a 4:3 window gains usable vertical space rather than stretching children, cropping or letterboxing the app. Search retains its weighted two columns, dynamic complete-row capacity and measured one-row shortcut capacity.

Window/density/font changes refresh native geometry through the existing appearance restoration path without recreating the Activity or WebViews. Query, cursor selection, explicit keyboard dismissal, focus, native scroll position and menu state are preserved. The existing native controller routes remain intact. D-pad-center confirms when it has no explicit custom binding. TV launcher discovery, an optional touchscreen requirement and a branding banner are declared.

## Corrected dimensional and animation behavior

- BrowserActivity's shared dp helper and native constructors use the same UI context; secondary screens and custom components inherit it.
- Settings no longer apply a second spare-height stretching multiplier or alternate visual-size tiers.
- Tab widths are budgeted in pixels after scaling design constants. Integer remainders are distributed, avoiding accumulated per-tab rounding overflow. Title/compact/overflow decisions and active close-target geometry remain coordinated.
- Composite settings slider rows derive their parent height from the sum of their individually scaled caption, gap and control dimensions. This avoids a one-pixel rounding clip without shrinking any control or text.
- Search, drawer, tab selection/open/close and reload timing remain unchanged. Their dp distances and measured bounds scale with the same system.
- Only tab membership/selection changes animate tab geometry. Window resize, theme changes, title updates and favicons do not become tab-selection events.
- Pointer and webpage scrolling speeds retain their original real-density behavior. Visual scaling does not alter input speed or WebView CSS conversion.

## Automated viewport matrix

The following WindowMetrics fixtures ran with no system insets; real windows subtract their actual insets. Native font rendering checks cover buttons, text, toolbar/icon geometry, circle aspect ratio, settings categories, bookmark/history/download/tab screens, search transitions and 50/60/70% synthetic keyboard occlusion. The tests also verify unchanged WebView identity/density and its fill-parent host contract; Robolectric does not render Chromium content.

| App window px | Density | Logical viewport dp | Scale | Effective Android UI density |
|---|---:|---|---:|---:|
| 1280 × 720 | 1 | 1280 × 720 | 1.538462 | 1.538462 |
| 1920 × 1080 | 1 | 1920 × 1080 | 2.307692 | 2.307692 |
| 2560 × 1440 | 1 | 2560 × 1440 | 3.076923 | 3.076923 |
| 3840 × 2160 | 1 | 3840 × 2160 | 4.615385 | 4.615385 |
| 1024 × 768 | 1 | 1024 × 768 | 1.230769 | 1.230769 |
| 1600 × 1200 | 1 | 1600 × 1200 | 1.923077 | 1.923077 |
| 2048 × 1536 | 1 | 2048 × 1536 | 2.461539 | 2.461539 |
| 1280 × 720 | 2 | 640 × 360 | 0.769231 | 1.538462 |
| 1920 × 1080 | 2 | 960 × 540 | 1.153846 | 2.307692 |
| 1920 × 1080 | 3 | 640 × 360 | 0.769231 | 2.307692 |
| TV app window 1920 × 1080 | 2 | 960 × 540 | 1.153846 | 2.307692 |

Additional tests cover 1.3 font scaling, Android 15 with 1.5 font scaling, live 832 × 468 → 1600 × 1200 resize preserving the query/selection/keyboard preference/WebView, D-pad-center confirmation, explicit controller mapping precedence and keyboard-only configuration callbacks preserving the same input View. Pure math verifies scale 0.5 and 8 without clamping.

## Modified files

Production:
- BrowserUiScale.kt (new): window/inset measurement, shared scale, private native resources, theme refresh and diagnostic sizes.
- BrowserActivity.kt: native construction/unit helpers, persistent chrome geometry, resize restoration, exact tab width budget, animation event separation, SP measurements and physical WebView/input boundary.
- SettingsDashboard.kt: shared design dimensions and removal of secondary expansion rules.
- SettingsContent.kt: natural sizing without an independent expansion multiplier.
- SurfTabLayout.kt: scaled constants with exact pixel budget/remainder distribution.
- SiteIconView.kt: pixel-sized fallback artwork text.
- ControllerBindings.kt: unmapped TV D-pad-center fallback with custom bindings taking precedence.
- AndroidManifest.xml: optional TV/touch features, TV launcher/banner, handled window/font/density changes.
- res/drawable/tv_banner.xml (new): existing PocketSurf logo in a TV banner.

Tests:
- LinearUiScalingTest.kt (new): viewport/density/TV/font matrix, clipping, search/IME geometry, resize and remote confirmation.
- Chrome04RegressionTest.kt, Chrome05RegressionTest.kt, Focus103RegressionTest.kt, Polish101RegressionTest.kt, Release10RegressionTest.kt, SettingsDashboardTest.kt, Surf122RegressionTest.kt, Surf129RegressionTest.kt and Tabs102RegressionTest.kt: replace obsolete fixed-pixel expectations with native UI-context units while retaining behavior/clipping checks.
- SearchRefinementTest.kt: use an actually wide-enough scaled viewport to verify more than eight shortcuts fit; no arbitrary bubble limit is added.

Local validation helper: .local/scaling-device.init.gradle reuses the existing configured signing identity for a debug update over the installed app, without changing project signing settings or keys. The ordinary debug identity was rejected by Android; the compatible debug install succeeded without uninstalling or clearing data.

The user's existing 1.4.0 release metadata is retained. No unrelated release/changelog edits or new dependencies are introduced.

## Validation status

**Final build: successful (2m 47s). All 412 automated tests passed, with zero failures or ignored tests.** The final run included 17 native-graphics linear-scaling tests. Test report: `app/build/reports/tests/testDebugUnitTest/index.html`.

Executed with Java 21 and the Gradle wrapper:

```powershell
$env:JAVA_HOME='C:\Users\Silas\.jdks\jbr-21.0.11'
.\gradlew.bat --init-script .local/scaling-device.init.gradle assembleDebug testDebugUnitTest --console=plain
```

The debug APK was installed successfully over the existing Retroid Pocket 6 app; no uninstall, data clearing or signing-key changes were performed. Cold launch completed in 371 ms. This is a launch observation, not an animation-performance benchmark.

| Actual device app window | Real density | Logged scale | UI density | Observation |
|---|---:|---:|---:|---|
| 1920 × 1080 | 2.30625 | 1.0006254 | 2.3076923 | Reference-size chrome, circular icons, settings and bookmarks rendered correctly |
| 1024 × 768 (temporary override) | 2.30625 | 0.53366685 | 1.2307693 | Full 4:3 area used; search query retained; header 44 px high and favicon circles 64 × 64 px |
| 2560 × 1440 (temporary override) | 2.30625 | 1.3341671 | 3.0769231 | Header controls 111 px high; proportionally enlarged suggestions and circles; query retained |

With the keyboard hidden, the 1024 × 768 search showed twelve complete provider suggestions. Reopening Inlay reduced the measured search area to y=54…229 and two complete suggestion rows, with the shortcut row ending at y=178. The final APK logs showed only the expected viewport changes, with no IME-induced global-scale churn. Native Back dismissed the keyboard without closing Search Mode. Returning to browsing restored the chrome and fixed-position disabled L/R controls. Browsing Settings rendered its grouped controls, including adjacent Bookmarks bar/Clear bookmarks controls. Existing saved data remained present.

**Unresolved device observation:** after forced resolution changes, Inlay sometimes produced black regions in captures while the UI hierarchy still reported valid search controls and suggestion bounds. At 4:3 this obscured the suggestions while the keyboard was open; at the larger override it affected the header region. Dismissing the keyboard restored rendering. The source of this compositing behavior has not been established; those override/IME combinations are not certified as visually correct. The automated geometry checks do not prove compositor correctness.

The temporary `wm size` override was reset and confirmed absent (`Physical size: 1080x1920` only). No density override was applied. Physical TV/external-display acceptance, a full physical-controller matrix and frame-time profiling were not performed.

Debug output: app/build/outputs/apk/debug/app-debug.apk.

## Limits and references

Synthetic TV configuration is not physical TV acceptance or store certification. Robolectric verifies native layout, controller dispatch and animation states; Chromium behavior and actual frame-time smoothness require device observation. Rounding at fractional density is unavoidable for integer pixel layout dimensions. Only app-window measurements, not advertised panel resolution, determine the scale.

Primary Android references: [WindowMetrics](https://developer.android.com/reference/android/view/WindowMetrics), [configuration-aware contexts](https://developer.android.com/reference/android/content/Context#createConfigurationContext(android.content.res.Configuration)).
