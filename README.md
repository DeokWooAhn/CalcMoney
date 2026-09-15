# CalcMoney (심플 환율 계산기)

한국수출입은행 환율을 바탕으로 금액을 빠르게 환산하는 모바일 앱입니다. 계산기 수식 환산, 통화 변환, 즐겨찾기 통화 비교를 지원합니다.

**Android**와 **iOS** 두 클라이언트가 하나의 저장소에 있으며 같은 백엔드(Firebase)를 공유합니다. 두 앱은 화면 구성과 아키텍처 레이어를 동일하게 맞춰 두었습니다.

| 클라이언트 | 위치 | 최소 지원 |
| --- | --- | --- |
| Android | 저장소 루트 Gradle 모듈 (`app`, `data`, `domain`, `presentation`) | Android 9 (API 28) |
| iOS | `ios/` | iOS 17 |
| 백엔드 | `functions/` (Cloud Functions), Firestore | — |

## 주요 기능

두 플랫폼 모두 동일한 기능을 제공합니다.

- 계산기 수식 입력 및 환율 환산
- 기준 통화와 대상 통화 간 환율 변환
- 자주 쓰는 통화를 즐겨찾기에 등록해 한눈에 비교
- 환율 정보의 로컬 캐시(12시간 TTL) 및 만료 후 갱신, 만료 시 stale 캐시 폴백
- 시스템 설정을 따르는 라이트·다크 테마
- 한국어·영어 지원
- AdMob 광고 동의 관리(UMP) 및 배너 광고

## 아키텍처

### 데이터 흐름

앱은 외부 환율 API를 직접 호출하지 않습니다. Cloud Functions가 매일 한국수출입은행 API에서 환율을 받아 Firestore의 `exchangeRates/latest` 문서 하나를 갱신하고, 두 앱은 그 문서만 읽어 로컬에 12시간 TTL로 캐시합니다.

```text
한국수출입은행 Open API
        │  (Cloud Functions: 매일 11:10 KST, 실패 시 12:30 재시도)
        ▼
Firestore  exchangeRates/latest
        │  (읽기 전용)
        ├──────────────► Android 앱 ── Room 캐시 (12h TTL)
        └──────────────► iOS 앱     ── SwiftData 캐시 (12h TTL)
```

수출입은행 API 키는 Cloud Functions의 Firebase secret `KOREA_EXIM_API_KEY` 하나뿐이며, 클라이언트에는 들어가지 않습니다. 자세한 문서 스키마와 스케줄은 [functions/README.md](functions/README.md)를 참고하세요.

### Android 모듈 구성

Clean Architecture 기반의 멀티 모듈 프로젝트입니다.

```text
:app          → :data, :domain, :presentation  # Hilt 조립 및 DI 진입점
:data         → :domain                         # Room, Firestore, DataStore, Repository 구현
:presentation → :domain                         # Compose UI 및 Orbit MVI (:data를 모름)
:domain                                         # 순수 Kotlin: 모델, Repository 인터페이스, UseCase
```

- 화면은 Calculator / Exchange / Favorite / Settings 네 개입니다.
- `MainActivity`는 `:app`이 아니라 `:presentation` 모듈에 있습니다.
- Settings·Favorite 화면은 activity-scoped `ExchangeViewModel`을 공유합니다.

### iOS 패키지 구성

SPM 로컬 패키지로 Android와 같은 레이어 방향을 유지합니다.

```text
ios/CalcMoney/             # 앱 타깃: AppContainer(수동 DI) + CalcMoneyApp
ios/Packages/Domain        # 순수 Swift, 의존성 0개: 모델, Repository 프로토콜, UseCase
ios/Packages/Data          → Domain, Firebase  # Repository 구현, SwiftData, UserDefaults
ios/Packages/Presentation  → Domain            # SwiftUI + Observation 기반 자체 MVI (Data를 모름)
```

- 화면은 Android와 동일하게 Calculator / Exchange / Favorite / Setting 네 개입니다.
- `AppContainer`가 만드는 `ExchangeViewModel`을 Exchange/Favorite/Setting 탭이 공유합니다. Android의 activity-scoped ViewModel과 같은 역할입니다.
- Orbit MVI의 side effect에 대응하는 one-shot 이벤트는 `AsyncStream` 기반 `SideEffectBus`로 직접 구현했습니다.

## 기술 구성

| 영역 | Android | iOS |
| --- | --- | --- |
| 언어 | Kotlin 2.2 (JVM 17) | Swift (swift-tools 6.2) |
| UI | Jetpack Compose, Material 3 | SwiftUI |
| 상태 관리 | Orbit MVI | Observation(`@Observable`) 기반 자체 MVI |
| 의존성 주입 | Hilt, KSP | `AppContainer` 수동 DI |
| 로컬 저장소 | Room, DataStore | SwiftData, UserDefaults |
| 원격 데이터 | Firebase Firestore | Firebase Firestore |
| 분석·안정성 | Firebase Analytics, Crashlytics | Firebase Analytics, Crashlytics |
| 광고 | Google Mobile Ads, UMP | Google Mobile Ads, UMP |
| 테스트 | Kotest, MockK, Orbit Test | Swift Testing(유닛), XCTest(UI) |
| 정적 분석 | Detekt, Android Lint | SwiftLint, SwiftFormat |

백엔드는 Node.js 22 기반 Cloud Functions(CommonJS)와 Firestore 규칙으로 구성됩니다.

## 시작하기

```bash
git clone https://github.com/DeokWooAhn/CalcMoney.git
cd CalcMoney
```

Firebase 설정 파일(`app/google-services.json`, `ios/CalcMoney/CalcMoney/GoogleService-Info.plist`)은 저장소에 포함되어 있습니다. 릴리스용 키스토어와 실제 AdMob ID는 저장소에 넣지 않습니다.

### Android

요구 사항

- Android Studio 최신 안정 버전
- JDK 17
- Android SDK 36
- Android 9(API 28) 이상 기기 또는 에뮬레이터

설정 및 실행

1. Android SDK 경로를 `local.properties`에 설정합니다. 이 파일에 필요한 값은 `sdk.dir` 하나뿐입니다.

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

2. 디버그 앱을 빌드하거나 Android Studio에서 실행합니다.

   ```bash
   ./gradlew :app:assembleDebug
   ```

디버그 빌드는 Google이 제공하는 AdMob 테스트 ID를 사용합니다.

### iOS

요구 사항

- Xcode 26 이상 — 로컬 패키지가 `swift-tools-version: 6.2`와 `.defaultIsolation(MainActor.self)`를 사용합니다
- iOS 17 이상 시뮬레이터 또는 기기
- 로컬 검증용 SwiftLint, SwiftFormat (`brew install swiftlint swiftformat`)

설정 및 실행

1. **`ios/CalcMoney.xcworkspace`를 엽니다.** `ios/CalcMoney/CalcMoney.xcodeproj`만 열면 앱은 빌드되지만 `Domain`/`Data`/`Presentation` 패키지 테스트가 테스트 플랜에서 빠집니다.
2. `CalcMoney` 스킴으로 실행합니다. SPM 의존성은 Xcode가 자동으로 내려받습니다.

현재 iOS는 `Info.plist`의 `GADApplicationIdentifier`가 Google 테스트 AdMob ID입니다. 스토어 배포 전에 실제 ID로 교체해야 합니다.

## 검증

### Android

```bash
./gradlew detekt              # 정적 분석 (ktlint 포함)
./gradlew test                # 전체 단위 테스트
./gradlew :app:assembleDebug  # 디버그 APK 빌드
```

모듈 단위 검증도 가능합니다.

```bash
./gradlew :domain:test
./gradlew :presentation:detekt
./gradlew :data:connectedDebugAndroidTest  # 에뮬레이터 또는 기기 필요
```

> `build`, `bundleRelease` 같은 릴리스 계열 태스크는 서명·AdMob 환경 변수를 강제 검증하므로 로컬에서 실패합니다. 일반적인 로컬 검증에는 위의 `detekt`, `test`, `assembleDebug`를 사용하세요.

### iOS

모든 명령은 `ios/` 디렉터리에서 실행합니다.

```bash
swiftformat --lint .
swiftlint lint --strict

xcodebuild test -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -testPlan CalcMoney -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

`CalcMoney.xctestplan` 하나가 앱 타깃 테스트 2개(`CalcMoneyTests`, `CalcMoneyUITests`)와 패키지 테스트 3개(`DomainTests`, `DataTests`, `PresentationTests`)를 모두 포함합니다. 워크스페이스로 실행해야 패키지 테스트가 플랜에서 해석됩니다.

> 경고를 오류로 승격하는 설정은 `CalcMoney.xcodeproj`의 프로젝트 빌드 설정에 있습니다. `xcodebuild` 인자로 넘기면 Firebase·GoogleMobileAds 같은 SPM 의존성 타깃까지 전파되어 컴파일이 깨집니다.

## CI/CD와 배포

워크플로는 경로 기준으로 분리되어 있어, 한쪽 플랫폼 변경이 다른 쪽 파이프라인을 돌리지 않습니다.

| 워크플로 | 러너 | 실행 조건 |
| --- | --- | --- |
| `android-ci.yml` | `ubuntu-latest` | `ios/**` **밖을** 건드린 PR, `master` 푸시, `android-v*` 태그 |
| `ios-ci.yml` | `macos-26` | `ios/**`를 건드린 PR과 `master` 푸시 |
| `firebase-deploy.yml` | `ubuntu-latest` | `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc`를 건드린 `master` 푸시 |

Android CI는 detekt → 단위 테스트 → Android Lint → 디버그 APK 빌드 순으로 실행합니다. iOS CI는 SwiftFormat·SwiftLint 검사와 `xcodebuild analyze` + 테스트 플랜 실행으로 구성되며, 시뮬레이터만 사용하므로 서명 시크릿이 필요 없습니다.

### Android 릴리스

릴리스 태그는 플랫폼별로 접두사를 붙입니다. `master`에 병합한 커밋에 `android-v*` 태그를 푸시하면 서명된 AAB가 Google Play 내부 테스트 트랙에 업로드됩니다.

```bash
git tag android-v1.0.2
git push origin android-v1.0.2
```

- 태그에서 `android-v`를 뺀 값이 앱의 `versionName`이 됩니다.
- `versionCode`는 `10000 + GitHub Actions 실행 번호`로 자동 계산됩니다. 직접 올리지 마세요.
- 배포 전 GitHub Actions Secrets에 키스토어, AdMob, Google Play 서비스 계정 값을 등록해야 합니다.
- Firebase App Distribution 배포는 `workflow_dispatch` 수동 실행으로만 가능합니다.

> 접두사 없는 `v*` 태그는 더 이상 아무 것도 트리거하지 않습니다. 기존 `v1.0.1` 태그는 이력 보존용입니다.

### iOS 릴리스

아직 App Store / TestFlight 배포 파이프라인은 없습니다. CI는 검증까지만 수행합니다. 추가할 때는 `ios-v*` 태그를 트리거로 쓰고 App Store Connect 서명 시크릿이 필요합니다.

### Firebase

Firebase Functions와 Firestore 규칙은 `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경이 `master`에 반영되면 자동 배포됩니다.

자세한 시크릿 목록과 배포 절차는 [지속적 배포 문서](docs/continuous-deployment.md)를 참고하세요.

## 프로젝트 문서

- [에이전트 작업 지침](AGENTS.md)
- [프로젝트 개요와 개발 메모](CLAUDE.md)
- [지속적 배포 안내](docs/continuous-deployment.md)
- [Cloud Functions와 Firestore 데이터 구조](functions/README.md)
- [AGP 9 업그레이드 계획](docs/agp-9-upgrade-plan.md)
- [개인정보처리방침](docs/privacy-policy.html)

## 라이선스

라이선스는 아직 정의되지 않았습니다.
