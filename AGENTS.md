# CalcMoney 작업 지침

## 작업 방식

- 기본적으로 질문·검토·설명 모드로 동작한다.
- 사용자가 "수정해줘", "만들어줘", "배포해줘"처럼 직접 변경을 요청한 경우에만 파일·Git·외부 시스템을 변경한다.
- 관련 없는 사용자 변경은 보존하고, 커밋·푸시 범위에 포함하지 않는다.
- 주석, KDoc, 테스트 설명, 커밋 메시지는 한국어로 작성한다. 코드 식별자와 로그 메시지는 영어로 작성한다.
- 새 의존성을 `gradle/libs.versions.toml`이나 각 `Package.swift`에 임의로 추가하지 않는다.
  목적과 대안, 앱 크기·빌드 시간 영향을 설명하고 승인을 받은 뒤 추가한다.
- 여러 파일을 건드리는 작업은 변경할 파일과 설계를 먼저 요약해 보여주고 확인을 받는다.

## 보안·프라이버시

- API 키, 키스토어 비밀번호, 토큰을 코드나 저장소 파일에 두지 않는다. AdMob·서명 시크릿은 CI
  환경 변수로, 수출입은행 키는 Cloud Functions의 Firebase secret으로만 관리한다.
- 로그와 Crashlytics에 개인 식별 정보나 사용자 입력값을 남기지 않는다. 계산식·금액처럼 사용자가
  입력한 값도 로그로 내보내지 않는다.
- Firestore 규칙을 넓히지 않는다. `exchangeRates/latest` 공개 읽기 외에는 열지 않고 앱 쓰기도
  허용하지 않는다.

## 프로젝트 구조

한국수출입은행 환율을 보여주는 앱이다. **Android(루트 Gradle 모듈)와 iOS(`ios/`) 두 클라이언트**가
같은 백엔드를 공유한다. Cloud Functions가 Firestore의 `exchangeRates/latest` 문서를 갱신하고,
두 앱 모두 이를 로컬에 12시간 TTL로 캐시한다. 앱은 환율 API를 직접 호출하지 않는다.

### Android

```
:app          -> :data, :domain, :presentation  # Hilt 조립 전용 셸
:data         -> :domain                         # Room, Firestore, DataStore, Repository 구현
:presentation -> :domain                         # Compose(Material3), Orbit MVI
:domain                                       # 순수 Kotlin: 모델, Repository 인터페이스, UseCase
```

- 화면은 Calculator, Exchange, Favorite, Settings 네 개다.
- `MainActivity`는 `:presentation` 모듈에 있다.
- Settings와 Favorite는 activity-scoped `ExchangeViewModel`을 공유한다. `MainNavGraph.kt`의
  `hiltViewModel(sharedOwner)`를 바꾸지 않는다.

### iOS

```
ios/CalcMoney/              # 앱 타깃: AppContainer(수동 DI) + CalcMoneyApp만 있음
ios/Packages/Domain         # 의존성 0개. 모델, Repository 프로토콜, UseCase
ios/Packages/Data           -> Domain, Firebase(Firestore)   # Repository 구현, UserDefaults, SwiftData
ios/Packages/Presentation   -> Domain                         # SwiftUI, Observation 기반 자체 MVI
```

- 화면은 Android와 동일하게 Calculator, Exchange, Favorite, Setting 네 개다.
- `AppContainer`가 만드는 `ExchangeViewModel`을 Exchange/Favorite/Setting 탭이 공유한다
  (Android의 activity-scoped ViewModel과 같은 역할). 각 탭마다 새로 만들지 않는다.
- `CalcMoney.xcworkspace` + `CalcMoney.xctestplan`으로 열어야 `Domain`/`Data`/`Presentation`
  패키지 테스트가 test plan에 잡힌다. `.xcodeproj`만 열면 빠진다.

## Android에서 쓰지 않는 것

아래는 현재 코드베이스에 위반이 없다. 새로 들이지 않는다.

- 레거시 비동기: `AsyncTask`, 직접 만든 `Thread`·`java.util.Timer`·`Handler`. 코루틴을 쓴다.
- 레이아웃 XML, `LayoutInflater`, `findViewById`. 화면은 Compose로만 만든다.
  `res/drawable`, `res/values-*` 같은 리소스 XML은 해당하지 않는다.
- `SharedPreferences`. 키-값 저장은 DataStore를 쓴다.
- HTTP 클라이언트(Retrofit, Ktor, Volley, `HttpURLConnection`)와 JSON 파서(Gson, Moshi).
  이 앱은 환율 API를 직접 호출하지 않고 Firestore가 네트워크 레이어다. 필요해 보이면 먼저 묻는다.

## Android 화면·MVI·데이터 작업

- 새 화면은 `presentation/ui/screen/<feature>/`에 `Contract`, `ViewModel`, `Screen` 세 파일로 구성한다.
  `ExchangeContract.kt`, `ExchangeViewModel.kt`, `ExchangeScreen.kt`을 기준으로 삼는다.
- ViewModel은 단일 `processIntent()` 진입점에서 intent를 private handler로 분배한다.
  Orbit `intent {}`의 실제 로직은 `suspend fun Syntax<State, SideEffect>.performX()` 형태의 private helper에 둔다.
- UseCase는 단일 책임의 `operator fun invoke`를 사용한다. ViewModel에는 개별 UseCase 대신
  `ExchangeUseCases`, `FavoriteUseCases` 같은 홀더를 하나만 주입한다.
- 도메인 오류는 sealed 계층으로 표현하고 UI 문자열 변환은 presentation 레이어에서 한다.
- Repository 구현은 `data/<feature>/local`, `remote`, `mapper`, `repository` 구조를 따른다.
- Room 스키마를 바꾸면 DB 버전과 migration을 추가하고 `data/schemas/` JSON도 커밋한다.
- 새 Repository·DataStore는 `data/di/`의 Module과 Qualifier까지 함께 배선한다.
- 새 AdMob 배너를 추가하면 `presentation/build.gradle.kts`의 buildType별 리소스와 CI 환경 변수를 함께 수정한다.
- 사용자에게 보이는 문자열은 `res/values/strings.xml`에 정의하고 `values-ko`를 함께 채운다.
  Composable이나 ViewModel에 표시 문자열을 직접 쓰지 않는다.
- 상호작용 요소는 최소 48dp 터치 영역을 확보한다. 아이콘·이미지에는 `contentDescription`을 주고
  장식용이면 `null`을 명시한다.
- `Screen` 컴포저블과 재사용 컴포넌트에는 `@Preview`를 최소 하나 둔다. ViewModel을 배선하는
  `Route`에는 두지 않는다.
- Composable 본문에서 상태를 바꾸거나 비즈니스 로직을 실행하지 않는다. `LaunchedEffect`,
  `DisposableEffect` 같은 side-effect API를 쓴다.
- `Context`를 ViewModel이나 깊은 컴포저블 계층으로 넘기지 않는다. 필요하면 그 자리에서
  `LocalContext.current`로 얻는다.

## iOS 화면·레이어 작업

- 새 화면은 `Presentation/Sources/Presentation/Screen/<Feature>/`에 State/Intent/SideEffect 파일과
  `<Feature>ViewModel`, `<Feature>View`로 구성한다. `Screen/Exchange/`를 기준으로 삼는다.
- ViewModel은 `@MainActor @Observable final class`이고 `send(_ intent:)` 단일 진입점에서 분배한다.
  Orbit `postSideEffect`에 대응하는 one-shot 이벤트는 `SideEffectBus`로 내보내고 View는
  `.task { for await effect in viewModel.sideEffects() { ... } }`로 소비한다.
- Async intent 처리가 겹치면 안 되는 화면은 `CalculatorViewModel`의 `enqueue(_:)` 직렬화 패턴을
  따른다. `@MainActor`만으로는 `await` 지점에서 다른 intent가 끼어드는 걸 막지 못한다.
- UseCase는 `callAsFunction`을 쓰는 struct 하나에 한 가지 일만 담는다. Repository는 프로토콜로만
  Domain에 두고 구현은 Data에 둔다.
- Domain은 Foundation 이상을 의존하지 않는다. `import SwiftUI`, `import UIKit`, `import Data`,
  Firebase 관련 import가 Domain에 들어가면 안 된다.
- Presentation은 `import Data`를 하지 않는다. Domain에만 의존한다.
- Data의 Repository 구현은 Firestore 문서/DTO를 그대로 반환하지 않고 Domain 타입으로 변환해서
  반환한다. `exchangeRates/latest` 문서 구조를 바꾸면 Cloud Functions(`functions/`)와 Android
  `:data` 모듈도 함께 확인한다.
- 캐시 TTL·오프라인 폴백은 Android 구현(12h TTL, stale 캐시 폴백)과 어긋나지 않게 유지한다.
- UIKit이 메인 스레드 밖에서 호출할 수 있는 클로저(동적 색상 provider 등)는 `nonisolated`를 명시한다.
  Presentation 패키지 전체가 `.defaultIsolation(MainActor.self)`라 빠뜨리면 Swift 6 런타임이
  SIGTRAP으로 크래시한다.

## Android 테스트와 검증

- 새 테스트는 Kotest `BehaviorSpec`과 한국어 Given/When/Then 설명을 사용한다.
  `isolationMode = IsolationMode.InstancePerRoot`와 MockK를 사용하며 Mockito는 사용하지 않는다.
- ViewModel 테스트는 `StandardTestDispatcher`, `Dispatchers.setMain/resetMain`, `runTest`,
  `advanceUntilIdle`, `orbit-test` 패턴을 따른다.
- 필요한 범위의 검증을 실행한다.

```bash
./gradlew :domain:test
./gradlew :presentation:detekt
./gradlew detekt test :app:assembleDebug
```

- `./gradlew build`와 release/bundle 계열 태스크는 로컬 시크릿 검증 때문에 실패할 수 있다.
  일반 검증에는 `assembleDebug`, `test`, `detekt`를 사용한다.

## iOS 테스트와 검증

- 새 유닛 테스트는 Swift Testing(`@Test`, `@Suite`, `#expect`)을 사용한다. XCTest는 UI 테스트
  (`CalcMoneyUITests`)에만 쓴다.
- 테스트 함수 이름은 한국어 + 언더스코어를 그대로 쓴다 (`func 등호_연타는_기록을_중복_저장하지_않는다()`).
  영어로 바꾸거나 raw identifier로 바꾸지 않는다.
- 새 UseCase나 Repository를 추가하면 대응하는 테스트도 함께 추가한다. 시간·난수·네트워크에
  의존하는 비결정적 테스트는 만들지 말고 `Clock`이나 fake 데이터소스를 주입한다.
- 모든 명령은 `ios/` 디렉터리에서 실행한다.

```bash
swiftformat --lint .
swiftlint lint --strict
xcodebuild test -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -testPlan CalcMoney -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

- `.xcodeproj`만 열어서 테스트를 돌리면 `Domain`/`Data`/`Presentation` 패키지 테스트가
  test plan에서 빠진다. 반드시 `.xcworkspace` + `-testPlan CalcMoney`로 실행한다.
- CI는 `macos-26`에 고정돼 있다 (`swift-tools 6.2` + `.defaultIsolation(MainActor.self)`가
  Xcode 26 이상을 요구). 로컬 재현 시에도 이 요구사항을 벗어나면 안 된다.

## Android 릴리스·배포

- 기본 브랜치는 `master`다.
- `versionCode`는 수동으로 변경하지 않는다. `v*` 태그 푸시 시 CI가
  `10000 + GITHUB_RUN_NUMBER`로 정하고, 태그의 `v`를 뺀 값이 `versionName`이 된다.
- 앱 릴리스는 `master` 병합 후 `v*` 태그를 푸시하면 signed AAB가 Google Play internal 트랙에 업로드된다.
- Firebase App Distribution은 workflow dispatch로만 실행한다.
- `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경은 `master` 푸시 후 Firebase 배포를 유발한다.
  Firestore는 `exchangeRates/latest`만 공개 읽기를 허용하고 앱 쓰기는 허용하지 않는다.

상세 배포 절차와 시크릿 목록은 `docs/continuous-deployment.md`를 따른다.

## 상세 참고 문서

- 코드 리뷰: `.claude/skills/review/SKILL.md`
- Android 화면/MVI/레이어 배선: `.claude/skills/mvi-screen/SKILL.md`
- Android 테스트: `.claude/skills/testing/SKILL.md`
- Android 릴리스·CI·Firebase 배포: `.claude/skills/release-deploy/SKILL.md`
- iOS 화면/레이어 배선: `.claude/skills/ios-screen/SKILL.md`
- iOS 테스트: `.claude/skills/ios-testing/SKILL.md`
- iOS 린트·빌드·CI: `.claude/skills/ios-ci/SKILL.md`
- 프로젝트 개요: `CLAUDE.md`
