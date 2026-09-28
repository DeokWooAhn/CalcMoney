# 지속적 배포

이 저장소는 Android 앱(루트의 Gradle 모듈)과 iOS 앱(`ios/`)을 함께 담고 있습니다. 워크플로는 경로 기준으로 분리되어 있어, 한쪽 플랫폼 변경이 다른 쪽 파이프라인을 실행시키지 않습니다.

| 워크플로 | 러너 | 실행 범위 |
| --- | --- | --- |
| `android-ci.yml` | `ubuntu-latest` | `ios/**` **밖을** 건드린 PR, `master`로의 모든 푸시, `android-v*` 태그 |
| `ios-ci.yml` | `macos-26` | `ios/**`를 건드린 PR과 `master` 푸시 |
| `firebase-deploy.yml` | `ubuntu-latest` | `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc`를 건드린 `master` 푸시 |

릴리스 태그는 플랫폼별로 접두사를 붙입니다. Play 스토어는 `android-v*`, App Store는 `ios-v*`를 예약해 두었습니다. 접두사 없는 `v*` 태그는 더 이상 아무 것도 트리거하지 않으며, `android-v` 도입 이전의 `v1.0.1` 태그는 이력 보존용으로만 남겨 둡니다.

## Android — Google Play 내부 테스트 배포

`android-v`로 시작하는 태그를 푸시하면 서명된 AAB를 빌드해 Google Play 내부 테스트 트랙에 업로드합니다.

```bash
git tag android-v1.0.2
git push origin android-v1.0.2
```

GitHub Actions 실행 번호를 `10000 + 실행 번호` 규칙으로 변환해 Android `versionCode`로 사용하므로, 자동 릴리스는 항상 이전보다 높은 버전 코드를 갖습니다. 태그 이름에서 앞의 `android-v`를 뺀 값이 `versionName`이 됩니다.

태그를 푸시하기 전에 다음 저장소 시크릿을 등록해야 합니다.

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `ADMOB_APP_ID`
- `ADMOB_EXCHANGE_BANNER_ID`
- `ADMOB_FAVORITE_BANNER_ID`
- `ADMOB_SETTINGS_BANNER_ID`
- `PLAY_SERVICE_ACCOUNT_JSON`: Play Console의 API 액세스 페이지에 등록되어 있고, 내부 테스트 트랙에 앱을 릴리스할 권한을 가진 서비스 계정의 JSON 키입니다.

앱은 Play Console에 한 번 생성해 두어야 하며, 패키지명은 `com.ahn.calcmoney`로 고정입니다.

## iOS 지속적 통합

`ios/CalcMoney/CalcMoney.xcodeproj`가 **아니라** `ios/CalcMoney.xcworkspace`를 열어야 합니다. 워크스페이스가 앱 프로젝트와 로컬 패키지 3개를 함께 묶습니다. 프로젝트만 열어도 앱 빌드와 실행은 되지만, 패키지 테스트 타깃이 테스트 플랜에서 보이지 않습니다.

`ios-ci.yml`에는 job이 두 개 있고 둘 다 `macos-26`에서 실행됩니다. 모두 시뮬레이터를 대상으로 하므로 시크릿이나 서명이 필요 없습니다.

### `lint`

| 도구 | 버전 | 설정 파일 | 명령 |
| --- | --- | --- | --- |
| SwiftFormat | 러너 이미지에 기본 설치됨 | `ios/.swiftformat` | `swiftformat --lint .` |
| SwiftLint | 워크플로의 `SWIFTLINT_VERSION`으로 고정 | `ios/.swiftlint.yml` | `swiftlint lint --strict` |

SwiftLint는 러너 이미지에 없어서 워크플로가 고정된 버전의 `portable_swiftlint.zip`을 내려받습니다. 버전을 고정하지 않으면 새 규칙이 추가된 SwiftLint 릴리스가 나올 때 코드 변경 없이 CI가 깨집니다. SwiftFormat은 기본 설치되어 있어서 고정하지 *않았습니다*. 러너 이미지가 올라가면 동작이 바뀔 수 있으니, 문제가 생기면 같은 방식으로 고정하세요.

`--strict`는 경고를 오류로 승격시키므로 트리를 항상 깨끗하게 유지해야 합니다. 규칙을 전역으로 약화시키는 대신 두 곳만 인라인 예외로 기록해 두었습니다. `XCTestCase`의 class var 오버라이드에 대한 `static_over_final_class`, 그리고 `CalculateExpressionUseCase`의 재귀 하향 파서에 대한 `function_body_length`입니다.

푸시 전에 두 가지를 로컬에서 실행하세요.

```bash
cd ios && swiftformat --lint . && swiftlint lint --strict
```

### `test`

`analyze`가 먼저 실행되며 `SWIFT_TREAT_WARNINGS_AS_ERRORS=YES`와 `GCC_TREAT_WARNINGS_AS_ERRORS=YES`가 적용됩니다. Xcode의 정적 분석기는 clang 분석기라서 순수 Swift 코드에서는 사실상 아무것도 잡아내지 못합니다. 실제로 이 단계를 게이트 역할을 하게 만드는 것은 경고를 오류로 승격시키는 이 설정입니다.

테스트는 `ios/CalcMoney/CalcMoney.xctestplan` 하나로 한 번에 실행되며, 테스트 타깃 5개(`CalcMoneyTests`, `CalcMoneyUITests`, `DomainTests`, `DataTests`, `PresentationTests`)를 모두 포함합니다. 이 플랜은 반드시 워크스페이스를 통해 실행해야 합니다. 프로젝트만으로 실행하면 패키지 타깃 3개가 아무 경고 없이 플랜에서 빠집니다.

테스트 플랜에서 UI 테스트 2개를 스킵합니다. `CalcMoneyUITests/testLaunchPerformance`와 `CalcMoneyUITestsLaunchTests/testLaunch`로, 둘 다 앱을 다섯 번 재실행하는 `measure` 블록이라 공유 러너에서는 느리고 불안정합니다.

러너는 `macos-latest`가 아니라 `macos-26`으로 고정했습니다. 로컬 패키지가 `swift-tools-version: 6.2`를 선언하고 `.defaultIsolation(MainActor.self)`를 사용하는데, 둘 다 Xcode 26 이상을 요구합니다. `macos-15` 이미지는 Xcode 16.x를 담고 있어서 빌드할 수 없습니다.

iOS 릴리스 파이프라인은 아직 없습니다. 추가할 때는 `ios-v*` 태그를 트리거로 쓰고 App Store Connect 서명 시크릿이 필요합니다.

## Firebase 배포

`master`에 병합된 변경 중 `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc`에 해당하는 것이 있으면 Firebase Functions와 Firestore 규칙이 자동 배포됩니다.

저장소 시크릿을 하나 더 추가해야 합니다.

- `FIREBASE_SERVICE_ACCOUNT`: `calculator-money-6ebb9` Firebase 프로젝트에 배포할 권한을 가진 서비스 계정의 JSON 키입니다.

워크플로는 잠금 파일 기준으로 Functions 의존성을 설치하고, `functions/index.js`와 `functions/exchangeRates.js`를 검사한 뒤 Firebase CLI로 배포합니다. Actions 탭에서 수동으로 실행할 수도 있습니다.

### 배포용 서비스 계정

배포 전용 서비스 계정 `github-firebase-deploy@calculator-money-6ebb9.iam.gserviceaccount.com`을 씁니다. 이 계정에 [IAM](https://console.cloud.google.com/iam-admin/iam?project=calculator-money-6ebb9)에서 아래 역할을 줍니다.

| 역할 | 필요한 이유 |
| --- | --- |
| Firebase Develop Admin (`roles/firebase.developAdmin`) | Firebase 프로젝트 정보(`adminSdkConfig`) 조회, 함수·Firestore 규칙 배포 |
| Service Account User (`roles/iam.serviceAccountUser`) | 함수를 실행 계정(`881712070348-compute@developer.gserviceaccount.com`)으로 배포 |
| Secret Manager Admin (`roles/secretmanager.admin`) | 함수에 `EXCHANGE_RATE_API_KEY` 접근 권한 연결 |
| Cloud Scheduler Admin (`roles/cloudscheduler.admin`) | 11:10·12:30 예약 작업 생성·수정 |

프로젝트에서 **Cloud Billing API**(`cloudbilling.googleapis.com`)가 켜져 있어야 합니다. Firebase CLI가 배포 전에 결제 상태를 확인하는데, 꺼져 있으면 직접 켜려다가 `Permissions denied enabling cloudbilling.googleapis.com`으로 실패합니다. 배포 계정에 API를 켜는 권한을 주는 대신 [콘솔](https://console.cloud.google.com/apis/library/cloudbilling.googleapis.com?project=calculator-money-6ebb9)에서 한 번 켜 둡니다.

키를 새로 발급하거나 교체할 때 주의할 점:

- **반드시 위 배포 계정의 키**를 씁니다. Firebase 콘솔의 "새 비공개 키 생성"으로 받는 `firebase-adminsdk-…` 계정의 키는 앱 서버용 권한만 있어서, 배포하면 `adminSdkConfig`에서 403으로 실패합니다.
- 키는 콘솔의 서비스 계정 → 키 → 키 추가 → JSON으로 만들고, 아래처럼 시크릿을 바꾼 뒤 내려받은 파일은 지웁니다.

  ```bash
  gh secret set FIREBASE_SERVICE_ACCOUNT --repo DeokWooAhn/CalcMoney < ~/Downloads/<키-파일>.json
  ```

- 교체한 뒤 쓰지 않는 예전 키는 서비스 계정의 키 탭에서 지웁니다.

### 함수에서 쓰는 Firebase secret

환율 API 키는 GitHub이 아니라 Firebase(Secret Manager)의 `EXCHANGE_RATE_API_KEY`에 둡니다.

- 배포된 함수는 배포 시점의 secret **버전에 고정**됩니다. 값을 바꾼 뒤에는 `firebase deploy --only functions`로 다시 배포해야 반영됩니다.
- 값을 입력할 때 프롬프트에 붙여넣으면 터미널에 따라 두 번 들어가거나 줄바꿈이 섞일 수 있습니다. 키를 클립보드에 복사한 뒤 아래 명령을 **복사하지 말고 직접 실행**합니다. 명령을 복사하면 클립보드의 키가 명령어로 덮어써집니다.

  ```bash
  pbpaste | tr -d '[:space:]' | firebase functions:secrets:set EXCHANGE_RATE_API_KEY --project calculator-money-6ebb9 --data-file=-
  ```

- 키가 틀리면 함수는 실패해도 이전 환율을 유지하고 정상 종료하므로, Cloud Scheduler에는 "성공"으로 보입니다. 실제 결과는 Firestore `exchangeRates/latest`의 `status`(`FRESH`/`STALE`/`ERROR`)와 `lastError`로 확인합니다.
