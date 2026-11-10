## 1.4.2 · 10 November 2026

- Show recent searches immediately when opening an empty address field, and restore them when clearing a typed query.
- Discard autocomplete responses whose query no longer matches the address field.
- Enable first-party and third-party cookies in browser WebViews. Third-party cookies also allow embedded services to store cross-site identifiers; this changes the previous cookie policy.
- Flush cookies to storage when the activity pauses.
- Add regression coverage for recent-search display and cookie acceptance.

Existing display-scaling limitations documented in 1.4.1 remain applicable.

Validation: 413 Android tests and 60 JavaScript tests passed. Signed release APK installed over the existing Retroid Pocket 6 app; cold launch and automatic keyboard opening passed.
