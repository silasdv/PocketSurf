# Validation — PocketSurf 1.2.7

Executed 2 November 2026, versionCode 28.

- Android/JVM: **320 passed; 0 failures/errors/skipped**. Includes input/focus/tab/mouse/native UI/privacy/download/session cases.
- JavaScript navigation, cookie and generated downloads: **59 passed; 0 failed**.
- assembleRelease, bundleRelease and lintRelease completed. Lint: **0 errors; 20 warnings**.
- Release APK signature verified (APK v2); dedicated release identity, SHA-256 certificate **80e11dde6e842966afa7125e6f3e5cdf2380a46e36245ce79cca9f7e7ee96c1c**.
- Release App Bundle verified with jarsigner. Self-signed Android app certificates do not imply a public CA identity.
- APK metadata confirms applicationId org.pocketsurf.browser, versionName 1.2.7 and versionCode 28; release is not debuggable.
- Native launcher test renders the compiled adaptive resource, requires monochrome artwork and checks the P stem and transparent S counter.
- Theme regressions cover 56 base/night/accent combinations, native Android screens, platform accent, Auto mode, forms and IME recovery.

Prior isolated desktop Chrome checks confirmed real CSS hover/trusted click, exact 123456-byte generated-file transfer and live Twitch banner dismissal. Those are desktop evidence, not handheld acceptance or a tracking audit.

No comprehensive physical-device acceptance is claimed. Installed IME/firmware, real WebView behavior/previews, measured 120-Hz output, media/DRM, TalkBack, OS document/download providers and private-profile cleanup remain device checks. Known storage/website limits are documented in README.md and DEVICE-TEST.md.

The public source excludes local keys/passwords, SDK paths, caches, browser profiles and personal development/audit material. SHA256SUMS.txt covers the distributed APK/AAB/source ZIP.

1.2.1 adds launcher/re-entry, preserved-session/recreation, external-link and visible dim Share/Reload regression coverage. JavaScript and website-engine code are unchanged from 1.2.0.

1.2.2: native layout assertions for 5/6/7/8/12/30 tabs, fixed favicon/close insets, six titled tabs with and without touch comfort, and compact/overflow width budgets. Native mock WebView renderings are not device screenshots.

1.2.5 verifies exact total row-width allocation for 4–100 tabs at 412/540/640/760/960dp, and native final-control alignment for 4/5/6/7/8/9/12/30 tabs. Fixed-width compact allocation is replaced by responsive distribution, including integer remainders.

Mouse-click regression checks native tap down/up source, tool and coordinates, plus controller press/release activation and retention of mouse mode. Real-device website playback/interaction is not covered by Robolectric.

Mouse taps wait for the existing page-input prepareClick handshake, so the page keeps mouse mode; callbacks are discarded after tab/focus/navigation changes.

1.2.5 regressions cover APK/generic MIME, Content-Disposition and RFC5987 filenames, archive/ROM suffixes, extensionless download endpoints, automatic popup rejection, separate native popup tabs, and return to the original page. No exact ROM-site URL was provided, so site-specific redirection chains remain unverified.

1.2.6 adds eight native tests for Custom Tabs service discovery, real AndroidX Binder warmup/session calls, HTTP routing, invalid-scheme rejection, normal-session isolation, recreation and navigation callbacks. Optional prefetch, postMessage and origin validation return unsupported results. No physical-device Discord-provider selection is claimed.

Analog pointer regressions verify the deadzone, monotonically increasing speed, unchanged maximum speed, direction and diagonal normalization. The existing constant right-stick scrolling tests remain unchanged. JavaScript verifies accent selection and rejects invalid CSS color values. Native selection highlights use the selected theme overlay.

The 19 lint warnings include the intentionally exported Custom Tabs service and available dependency upgrades; no lint errors. Browser integration is basic and does not claim Trusted Web Activity/authentication support. Signed 1.2.6 artifacts were prepared locally and checked against the retained release identity.

Four additional native UI regressions cover address-hint visibility while focused/editing, controller submission, balanced mouse-switch insets and fully visible About layouts at 960×540 and 640×360. Native mock-WebView renderings were inspected; they are not physical-device screenshots.

1.2.7 adds eleven native regressions for ordered Surf Bar navigation, Y entry, address-boundary navigation, prior selection restoration, stale-return invalidation, native-screen return, B/Android Back versus X closing, pointer centering/handoff, active-only tab close targets, tab-width budgets and narrow-screen action/legend visibility. Four JavaScript regressions cover pointer-target adoption and explicit selection focus restoration. Earlier fixtures were updated to the new control contract. Native renderings were inspected at 960×540 and 412×800; mock WebView renderings do not prove real website behavior.
