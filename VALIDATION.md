# Validation — PocketSurf 1.2.1

Executed 1 November 2026, versionCode 22.

- Android/JVM: **282 passed; 0 failures/errors/skipped**. Includes input/focus/tab/mouse/native UI/privacy/download/session cases.
- JavaScript navigation, cookie and generated downloads: **54 passed; 0 failed**.
- assembleRelease, bundleRelease and lintRelease completed. Lint: **0 errors; 16 warnings**.
- Release APK signature verified (APK v2); dedicated release identity, SHA-256 certificate **80e11dde6e842966afa7125e6f3e5cdf2380a46e36245ce79cca9f7e7ee96c1c**.
- Release App Bundle verified with jarsigner. Self-signed Android app certificates do not imply a public CA identity.
- APK metadata confirms applicationId org.pocketsurf.browser, versionName 1.2.0 and versionCode 22; release is not debuggable.
- Native launcher test renders the compiled adaptive resource, requires monochrome artwork and checks the P stem and transparent S counter.
- Theme regressions cover 56 base/night/accent combinations, native Android screens, platform accent, Auto mode, forms and IME recovery.

Prior isolated desktop Chrome checks confirmed real CSS hover/trusted click, exact 123456-byte generated-file transfer and live Twitch banner dismissal. Those are desktop evidence, not handheld acceptance or a tracking audit.

No comprehensive physical-device acceptance is claimed. Installed IME/firmware, real WebView behavior/previews, measured 120-Hz output, media/DRM, TalkBack, OS document/download providers and private-profile cleanup remain device checks. Known storage/website limits are documented in README.md and DEVICE-TEST.md.

The public source excludes local keys/passwords, SDK paths, caches, browser profiles and personal development/audit material. SHA256SUMS.txt covers the distributed APK/AAB/source ZIP.

1.2.1 adds launcher/re-entry, preserved-session/recreation, external-link and visible dim Share/Reload regression coverage. JavaScript and website-engine code are unchanged from 1.2.0.
