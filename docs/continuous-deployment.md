# Continuous deployment

## Android release to Google Play internal testing

Pushing a tag that starts with `v` builds a signed AAB and uploads it to the Google Play internal testing track.

```bash
git tag v1.0.1
git push origin v1.0.1
```

The GitHub Actions run number is converted to an Android `versionCode` using `10000 + run number`, so each automated release has a higher version code. The tag name without its leading `v` is used as `versionName`.

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

## Firebase deployment

Changes merged into `master` that affect `functions/**`, `firebase.json`, `firestore.rules`, or `.firebaserc` automatically deploy Firebase Functions and Firestore rules.

Add one additional repository secret:

- `FIREBASE_SERVICE_ACCOUNT`: JSON key for a service account authorized to deploy to the `calculator-money-6ebb9` Firebase project.

The workflow installs the locked Functions dependencies, checks `functions/index.js`, then deploys with the Firebase CLI. You can also run it manually from the Actions tab.
