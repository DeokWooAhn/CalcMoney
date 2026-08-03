# Continuous deployment

This repository holds both the Android app (Gradle modules at the root) and the iOS app (`ios/`). Workflows are scoped by path so that a change to one platform does not run the other platform's pipeline.

| Workflow | Runs on | Scope |
| --- | --- | --- |
| `android-ci.yml` | `ubuntu-latest` | PRs that touch anything **outside** `ios/**`; every push to `master`; `android-v*` tags |
| `ios-ci.yml` | `macos-26` | PRs and `master` pushes that touch `ios/**` |
| `firebase-deploy.yml` | `ubuntu-latest` | `master` pushes that touch `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` |

Release tags are namespaced per platform: `android-v*` for the Play Store, `ios-v*` reserved for the App Store. A bare `v*` tag no longer triggers anything — the pre-`android-v` tag `v1.0.1` is kept for history only.

## Android release to Google Play internal testing

Pushing a tag that starts with `android-v` builds a signed AAB and uploads it to the Google Play internal testing track.

```bash
git tag android-v1.0.2
git push origin android-v1.0.2
```

The GitHub Actions run number is converted to an Android `versionCode` using `10000 + run number`, so each automated release has a higher version code. The tag name without its leading `android-v` is used as `versionName`.

Set the following repository secrets before publishing a tag:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `ADMOB_APP_ID`
- `ADMOB_EXCHANGE_BANNER_ID`
- `ADMOB_FAVORITE_BANNER_ID`
- `ADMOB_SETTINGS_BANNER_ID`
- `PLAY_SERVICE_ACCOUNT_JSON`: JSON key for a service account that has been added in Play Console's API access page with permission to release the app to internal testing.

The app must be created once in Play Console and its package name must remain `com.ahn.calcmoney`.

## iOS continuous integration

`ios-ci.yml` runs the `Domain`, `Data`, and `Presentation` package tests plus the app target's tests on an iPhone 17 Pro simulator. No secrets or signing are required — everything builds for the simulator.

The runner is pinned to `macos-26` rather than `macos-latest`. The local packages declare `swift-tools-version: 6.2` and use `.defaultIsolation(MainActor.self)`, both of which need Xcode 26 or newer; the `macos-15` image ships Xcode 16.x and cannot build them.

`CalcMoneyUITests/testLaunchPerformance` is skipped in CI. It is a `measure` block that relaunches the app five times, which is slow and noisy on a shared runner.

There is no iOS release pipeline yet. When one is added it should trigger on `ios-v*` tags and will need App Store Connect signing secrets.

## Firebase deployment

Changes merged into `master` that affect `functions/**`, `firebase.json`, `firestore.rules`, or `.firebaserc` automatically deploy Firebase Functions and Firestore rules.

Add one additional repository secret:

- `FIREBASE_SERVICE_ACCOUNT`: JSON key for a service account authorized to deploy to the `calculator-money-6ebb9` Firebase project.

The workflow installs the locked Functions dependencies, checks `functions/index.js`, then deploys with the Firebase CLI. You can also run it manually from the Actions tab.
