# Release signing

PocketSurf 1.2.7 uses versionCode 28 and a dedicated release identity. Retain the private key for future updates; never commit or upload it or its passwords.

| Environment variable | Value |
| --- | --- |
| POCKETSURF_KEYSTORE_PATH | Absolute path to release keystore |
| POCKETSURF_STORE_PASSWORD | Store password |
| POCKETSURF_KEY_ALIAS | Alias |
| POCKETSURF_KEY_PASSWORD | Key password |

Alternatively use ignored `.local/release-signing.properties` with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Relative storeFile resolves from the project root.

```sh
./gradlew testDebugUnitTest lintRelease assembleRelease bundleRelease
node --test app/src/test/js/navigation.test.cjs app/src/test/js/cookie-consent.test.cjs tools/test-generated-download.mjs
```

Without signing configuration the release verification output can be unsigned; do not distribute it as installable. Verify the signed APK with apksigner and bundle with jarsigner; record the certificate SHA-256 and calculate artifact checksums.

Publish reviewed source, tag v1.2.7 and attach PocketSurf-1.2.7.apk, PocketSurf-1.2.7.aab, PocketSurf-1.2.7-source.zip and SHA256SUMS.txt. Exclude keys, local configuration/caches/profiles and personal development/audit material.

Back up the key/passwords privately. Old development-signed installations require backup/uninstall before this release. JSON backup excludes cookies, history and open tabs.
