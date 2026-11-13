# CalcMoney (심플 환율 계산기) 작업 지침

ExchangeRate-API의 원화 기준 환율(160여 개 통화)을 보여주는 앱이다. Android(Kotlin, 루트 Gradle 모듈)와 iOS(Swift, `ios/`) 두 클라이언트가 같은 백엔드를 공유한다.
Claude Code는 `CLAUDE.md`를 통해 이 파일을 읽고, Codex 같은 다른 에이전트와 CodeRabbit은 이 파일을 직접 읽는다.

코드를 쓰기 전에 이 파일의 규칙을 따른다. 세부 절차는 스킬(`.claude/skills/`)에, 코드에 남지 않는 결정은 `docs/decisions/`에 있다.

## 작업 방식

- 기본적으로 질문·검토·설명 모드로 동작한다. 사용자가 "수정해줘", "만들어줘", "배포해줘"처럼 직접 변경을 요청한 경우에만 파일·Git·외부 시스템을 바꾼다.
- 여러 파일을 건드리는 작업은 바꿀 파일과 설계를 먼저 요약해 보여주고 확인을 받는다.
- 관련 없는 사용자 변경은 보존하고, 커밋·푸시 범위에 넣지 않는다.
- 새 의존성을 `gradle/libs.versions.toml`이나 각 `Package.swift`에 임의로 추가하지 않는다. 목적과 대안, 앱 크기·빌드 시간 영향을 설명하고 승인을 받은 뒤 추가한다.
- 주석·KDoc·테스트 설명·커밋 메시지는 한국어, 코드 식별자와 로그는 영어로 쓴다.
- **실행하지 않은 검증을 통과했다고 말하지 않는다.** 돌리지 못했으면 무엇을 왜 못 돌렸는지와 돌려야 할 명령을 적는다. "통과할 것으로 보인다"고 쓰지 않는다.
- **코드에 남지 않는 결정은 `docs/decisions/`에 남긴다.** 스토어 이미지·등록 문구·소개 글을 만들거나, 버그처럼 보이는 동작을 고치기 전에 `docs/decisions/README.md`의 목록을 먼저 본다. 대화에서 정책·문구 방향·하지 않기로 한 것을 정했으면 그 작업을 마치기 전에 기록을 더하거나 있는 기록을 고친다.

## 작업 시작 메모

코드 변경을 요청받으면 관련 코드와 지침을 먼저 읽고, 구현 전에 다음을 짧게 정리한다.

- **목표:** 해결할 문제와 기대 동작
- **수정 범위:** 먼저 읽을 파일, 수정할 파일, 이번 작업에서 제외할 내용
- **검증:** 확인할 시나리오와 실행할 명령
- **완료 조건:** 어떤 결과가 확인되면 끝낼 수 있는지
- **불확실한 점:** 아직 확인하지 못한 원인이나 결정

작은 작업은 채팅에 5~10줄로 작성한다. 여러 계층에 걸친 작업이나 다른 채팅으로 넘길 작업은 작업별 메모 파일로 남기고 경로를 알린다.
기존 지침은 복사하지 않고 필요한 스킬·코드·문서 경로를 참조한다.
문제를 재현하거나 코드를 확인하기 전에 원인을 단정하지 않는다. 작업 중 범위나 검증 방법이 달라지면 메모도 갱신한다.
메모를 작성한 뒤에는 이 파일의 기존 확인·승인 규칙에 따라 진행한다. 이미 확인받은 계획이 있으면 메모에 반영하고 같은 내용을 다시 확인받지 않는다.
질문·설명·검토만 요청받은 경우에는 구현용 메모를 만들지 않는다.

## 브랜치·커밋·PR

- 기본 브랜치는 `main`이 아니라 **`master`**다. `master`에 직접 커밋·푸시하지 않고, 작업마다 브랜치를 만들어 PR로 합친다.
  - 브랜치 이름: `feature/…`, `fix/…`, `docs/…`, `chore/…`, `build/…` + 영어 소문자 하이픈 (`feature/konsist-architecture-rules`)
  - PR은 squash merge한다. PR 제목이 그대로 `master`의 커밋 제목이 된다.
- 커밋 메시지와 PR 제목은 무엇을 했는지 한국어 문장으로 쓴다. 마침표는 붙이지 않는다.

```text
iOS Firebase SDK를 12.19.3으로 올려 유출된 인증 기관을 신뢰 목록에서 뺀다

- 필요하면 빈 줄 뒤에 바뀐 내용을 항목으로 적는다
```

- 커밋·PR에 AI 도구의 공동 작성자 표기(`Co-Authored-By` 등)를 붙이지 않는다.

## 안전장치

- **`master` 보호** (GitHub ruleset "master 보호", 2026-10-21): PR로만 합칠 수 있고, 브랜치 삭제와 강제 푸시가 막힌다. **필수 CI 검사는 걸려 있지 않다.** CI가 실패해도 병합할 수 있으니 병합 전에 결과를 직접 확인한다.
- **Claude Code 플러그인** (`.claude/settings.json`, ahn-cc-kit의 `maestro-e2e`): 화면을 고친 턴이 끝날 때 Maestro 실행 결과(또는 건너뛴 이유)가 있는지 확인한다. 실행 방법은 `.claude/skills/maestro/SKILL.md`.
- 훅이 막으면 우회하지 말고 무엇이 왜 막혔는지 사용자에게 알린다.

## 보안·공개 저장소

- **공개 저장소다.** API 키, 키스토어 비밀번호, 토큰을 코드나 저장소 파일에 두지 않는다.
  - AdMob·서명 시크릿은 Android는 CI 환경 변수로, iOS는 git이 무시하는 `ios/CalcMoney/Config/AdMob.xcconfig`로만 관리한다(아래 "iOS 릴리스").
  - 환율 API(ExchangeRate-API) 키는 Cloud Functions의 Firebase secret `EXCHANGE_RATE_API_KEY` 하나뿐이다. 이 키는 요청 URL 경로에 들어가므로 URL이나 원본 네트워크 오류를 로그·`lastError`(공개 읽기 문서)에 남기지 않는다.
  - Firebase 설정 파일(`app/google-services.json`, `GoogleService-Info.plist`)은 앱에 그대로 들어가는 클라이언트 설정이라 커밋한다. 데이터는 Firestore 규칙과 App Check로 지킨다.
- Firestore 규칙을 넓히지 않는다. `exchangeRates/latest` 공개 읽기 외에는 열지 않고 앱 쓰기도 허용하지 않는다.
- 로그와 Crashlytics에 개인 식별 정보나 사용자 입력값을 남기지 않는다. 계산식·금액처럼 사용자가 입력한 값도 내보내지 않는다.
- **크래시 단서는 `CrashBreadcrumbs`(Android domain·iOS Domain)로만 남긴다.** 하단 탭 이동이 Crashlytics 키 `screen`으로, 환율 원격 조회 결과·광고 동의·전면 광고 표시·인앱 리뷰 요청이 로그로 남는다. 입력값을 남기지 않는 이유는 App Store 개인정보 신고의 "진단" 범위를 넘지 않기 위해서다. iOS는 Firebase가 설정되지 않은 빌드에서 `NoOpCrashBreadcrumbs`를 쓴다. 크래시를 분석할 때는 `analyze-crash` 스킬을 따른다.
- **App Check는 provider가 빌드 타입별로 갈린다.** 디버그는 debug provider, 릴리스는 Play Integrity(Android)·App Attest(iOS)다. Android는 `app/src/debug`·`app/src/release`의 `AppCheckInstaller.kt` 두 파일로, iOS는 `FirebaseBootstrap.swift`의 `#if DEBUG`로 분기한다. iOS는 반드시 `FirebaseApp.configure()` **이전에** factory를 지정한다.

## 프로젝트 구조

Cloud Functions가 매일 환율을 받아 Firestore `exchangeRates/latest` 문서 하나에 쓰고, 두 앱 모두 그 문서를 읽어 로컬에 캐시(12시간 TTL)한다. 앱은 환율 API를 직접 호출하지 않는다. Retrofit도 URLSession 직접 호출도 없고, Firestore가 곧 네트워크 레이어다.

### Android (Clean Architecture)

```
:app          -> :data, :domain, :presentation  # Hilt 조립 전용 셸 (Kotlin 파일 1개)
:data         -> :domain                         # Room, Firestore, DataStore, Repository 구현
:presentation -> :domain                         # Compose(Material3), Orbit MVI (:data를 모름)
:domain                                          # 순수 Kotlin JVM: 모델, Repository 인터페이스, UseCase
:konsist                                         # 테스트 전용. 위 모듈의 소스를 읽어 구조 규칙을 검사 (Konsist)
```

- 화면은 Calculator, Exchange, Favorite, Settings 네 개다(`presentation/src/main/java/com/ahn/presentation/main/Route.kt`).
- `MainActivity`는 `:app`이 아니라 `:presentation`에 있다.
- Settings와 Favorite는 activity-scoped `ExchangeViewModel`을 공유한다(`MainNavGraph.kt`의 `hiltViewModel(sharedOwner)`). 이 owner를 바꾸면 화면 간 상태 공유가 조용히 깨진다.

기술 스택: Kotlin 2.2 / JVM 17 / compileSdk 36 · Compose(Material3) · Hilt+KSP(kapt 없음) · Orbit MVI · Room(스키마 `data/schemas/`) · DataStore(Qualifier로 분리된 4개 스토어) · Firebase(Analytics·Crashlytics·Firestore) · AdMob+UMP · Kotest+MockK

### iOS (SPM 로컬 패키지, Android와 같은 레이어 방향)

```
ios/CalcMoney/              # 앱 타깃: AppContainer.swift(수동 DI, Hilt 모듈 대응) + CalcMoneyApp.swift만 있음
ios/Packages/Domain         # 의존성 0개. 모델, Repository 프로토콜, UseCase
ios/Packages/Data           -> Domain, Firebase(Firestore)   # Repository 구현, UserDefaults, SwiftData
ios/Packages/Presentation   -> Domain                         # SwiftUI, Observation 기반 자체 MVI (Data를 모름)
```

- 화면은 Android와 같은 Calculator, Exchange, Favorite, Setting 네 개다.
- `AppContainer`가 만드는 `ExchangeViewModel`을 Exchange/Favorite/Setting 탭이 공유한다(Android의 activity-scoped ViewModel과 같은 역할). 탭마다 새로 만들지 않는다.

기술 스택: Swift 6.2 tools / iOS 17+ · SwiftUI · Observation(`@Observable`, Combine·TCA 없음) · `AsyncStream`(Orbit `Flow`/side-effect 대응, Orbit 라이브러리 없이 직접 구현) · SwiftData(로컬 캐시) · Firebase(Firestore·Analytics·Crashlytics) · AdMob+UMP · Swift Testing(유닛) + XCTest(UI)

### 환율 데이터와 통화 목록 (두 플랫폼 공통)

- 환율 값은 **외화 1단위당 원**(`baseRate`)이다. API는 1원당 외화로 주므로 서버가 역수로 저장한다. 앱 쪽에서 다시 뒤집지 않는다.
- `exchangeRates/latest` 문서 구조를 바꾸면 Cloud Functions(`functions/`)와 Android `:data`, iOS `Data` 패키지를 함께 확인한다.
- **통화 목록을 앱에 하드코딩하지 않는다.** 목록은 서버가 정하고, 순서(즐겨찾기 → 통화 코드 알파벳순)·국기·이름은 앱이 만든다. 오른쪽 ★·A~Z 빠른 이동 인덱스가 이 순서에 의존하고, 서버 이름(늘 한국어)은 한국어 화면에서만 쓴다. 통화 정렬·국기·이름 코드를 고치거나 서버가 새 통화를 주면 `docs/decisions/currency-list-and-names.md`부터 읽는다(이름 순서, 예비 이름 12개, 국기 예외, 코드 위치).
- 캐시 TTL·오프라인 폴백(12시간 TTL, stale 캐시 폴백)은 두 플랫폼이 어긋나지 않게 유지한다.

## Android에서 쓰지 않는 것

아래는 현재 코드베이스에 위반이 없다. 새로 들이지 않는다.

- 레거시 비동기: `AsyncTask`, 직접 만든 `Thread`·`java.util.Timer`·`Handler`. 코루틴을 쓴다.
- 레이아웃 XML, `LayoutInflater`, `findViewById`. 화면은 Compose로만 만든다. `res/drawable`, `res/values-*` 같은 리소스 XML은 해당하지 않는다.
- `SharedPreferences`. 키-값 저장은 DataStore를 쓴다.
- HTTP 클라이언트(Retrofit, Ktor, Volley, `HttpURLConnection`)와 JSON 파서(Gson, Moshi). 이 앱은 환율 API를 직접 호출하지 않고 Firestore가 네트워크 레이어다. 필요해 보이면 먼저 묻는다.

## 두 플랫폼 공통 화면 규칙

- **인앱 리뷰는 OS·스토어의 시스템 창만 쓴다.** "앱이 어떤가요?"처럼 먼저 묻는 창을 직접 만들면 Play 정책(카드 전후 질문 금지)과 App Store 심사 지침 5.6.1 위반이다. 요청 조건은 domain `ReviewPromptPolicy`(Android·iOS 같은 값)에 있고, 계산기에서 계산에 성공한 직후에만 요청한다. `UI_TESTING` 실행에서는 띄우지 않는다. iOS 개발 빌드는 요청할 때마다 창이 떠서 테스트를 막는다.
- **테스트 id(`testTag`·`accessibilityIdentifier`)를 바꿨으면 `.maestro/` Flow도 함께 고친다.** 두 플랫폼이 같은 id로 Flow 하나를 돌린다. CI `e2e_smoke`가 Android 태그 릴리스의 관문이라 Flow가 깨지면 릴리스가 막힌다.

## Android 검증

테스트 작성법(Kotest·MockK·ViewModel 패턴)은 `testing` 스킬을 따른다. 여기서는 명령과 게이트만 둔다.

- 필요한 범위의 검증을 실행한다.

```bash
./gradlew detekt            # 린트 (ktlint 포함, autoCorrect 켜짐)
./gradlew test              # 전체 유닛 테스트 (:konsist 구조 규칙 포함)
./gradlew :konsist:test     # 구조 규칙만: ViewModel·Contract·UseCase·Repository의 모양
./gradlew :app:assembleDebug
./gradlew :koverHtmlReportUnit   # 병합 커버리지 → build/reports/kover/htmlUnit/index.html
./gradlew :koverVerifyUnit       # CI 커버리지 게이트와 같은 검사
./gradlew :app:dependencyGuard :app:manifestShield                   # 의존성·Merged Manifest를 baseline과 비교 (CI와 같음)
./gradlew :app:dependencyGuardBaseline :app:manifestShieldBaseline   # 의도한 변경이면 baseline 다시 만들기

./gradlew :domain:test           # 모듈 하나만 볼 때는 앞에 모듈을 붙인다
./gradlew :presentation:detekt
./gradlew detekt test :app:assembleDebug :koverVerifyUnit   # PR 전 확인 (CI와 같은 순서)
```

- detekt는 `dev.detekt` 2.0 알파다(구 `io.gitlab.arturbosch.detekt` 아님). 모듈별 `detekt-baseline.xml`이 있다.
- **Kover는 루트의 `unit` variant만 쓴다.** 각 모듈의 debug(domain은 jvm)를 루트가 병합하고, 제외 필터와 하한(`minBound`)은 루트 `build.gradle.kts`에만 있다. 태스크 앞에 `:`를 빼면 필터 없는 모듈별 리포트까지 만들고, `koverHtmlReport`(total)는 release까지 돌며, `*Release` variant 태스크는 시크릿 검증에 걸린다.
- **라이브러리 버전을 올리면 baseline도 함께 다시 만들어 PR에 커밋한다.** `app/dependencies/releaseRuntimeClasspath.txt`는 출시 앱의 런타임 의존성(dependency-guard), `app/manifestShield/`는 Merged Manifest의 권한·exported 컴포넌트와 그걸 넣은 라이브러리(manifest-shield)다. 앱이 직접 선언한 권한은 `INTERNET`뿐이고 `AD_ID`·`FOREGROUND_SERVICE` 등은 AdMob·Firebase·WorkManager가 넣는다. 새 권한·exported 컴포넌트가 생기면 출처(`.sources.txt`)를 보고 Play Console 신고(광고 ID, 데이터 보안) 영향을 확인한다. CI는 비교만 하고 baseline을 고치지 않는다.

## iOS 검증

테스트 작성법(Swift Testing·한글 테스트 이름·주입)은 `ios-testing` 스킬을 따른다. 여기서는 명령만 둔다.

- 모든 명령은 `ios/` 디렉터리에서 실행한다.

```bash
swiftformat --lint .        # 포맷 검사
swiftlint lint --strict     # 린트
xcodebuild test -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -testPlan CalcMoney -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

## 릴리스·배포

### Android

1. `master`에 병합한다.
2. `android-v*` 태그를 푸시하면 CI가 Maestro smoke(`e2e_smoke`)를 통과시킨 뒤 서명된 AAB를 Google Play internal 트랙에 올린다. smoke가 실패하면 빌드와 업로드가 멈춘다.

- **`versionCode`를 손으로 고치지 않는다.** CI가 `10000 + GITHUB_RUN_NUMBER`로 정하고, 태그의 `android-v`를 뺀 값이 `versionName`이 된다. 로컬 기본값은 `app/build.gradle.kts`의 `DEFAULT_VERSION_CODE`다.
- 프로덕션 승급 전에는 내부 테스트판을 손으로 확인한다. 내부 테스트판에는 Maestro를 돌리지 않는다(실제 광고가 뜬다).
- Firebase App Distribution은 workflow dispatch로만 실행한다.

### iOS

- **Release 빌드에는 저장소 밖의 `ios/CalcMoney/Config/AdMob.xcconfig`가 필요하다.** 실제 AdMob ID(앱 ID와 광고 단위 4개)는 git이 무시하는 이 파일에만 둔다(견본 `AdMob.example.xcconfig`). 앱 타깃 Release 구성의 `Config/Release.xcconfig`가 이 파일을 읽고, 값은 Info.plist를 거쳐 `AdConfig.swift`가 읽는다. 파일이 없거나 테스트 ID면 앱 타깃의 "Check AdMob IDs" 빌드 단계가 Release 빌드를 멈춘다(Android 릴리스 시크릿 검증과 같은 역할).
- 앱 타깃의 Release 빌드 설정에 `GAD_APPLICATION_IDENTIFIER`를 다시 넣지 않는다. 타깃 설정이 xcconfig 값을 덮어쓴다.
- 배포 자동화는 없다. CI는 Debug만 빌드한다. Xcode Archive로 올리고, 빌드 번호(앱 타깃의 `CURRENT_PROJECT_VERSION`)는 올릴 때마다 손으로 1씩 올린다. App Store Connect 입력값은 `store-assets/app-store-ko/app-store-connect.md`.

### Firebase Functions·Firestore

- `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경은 `master` 푸시 뒤 Firebase 배포를 일으킨다.
- 상세 배포 절차와 시크릿 목록은 `docs/continuous-deployment.md`를 따른다.

## 환경 함정

코드만 봐서는 놓치기 쉬운 것들이다.

- **`./gradlew build`는 로컬에서 실패한다.** 태스크 이름에 Release/assemble/bundle/build가 들어가면 AdMob·키스토어 시크릿 9개를 강제 검증(`error()`)하기 때문이다. 일반 검증에는 `assembleDebug`, `test`, `detekt`를 쓴다.
- **Android Studio는 Rabbit 2(2026.2.2) 이상이 필요하다.** AGP 9.5를 여는 첫 버전이다. "Migrate to Gradle Daemon toolchain" 알림은 무시한다. 받아들이면 `gradle/gradle-daemon-jvm.properties`가 생겨 CI의 JDK 17보다 우선한다. 경위는 `docs/agp-9-upgrade-plan.md`.
- **iOS CI는 `macos-26`에 고정돼 있다.** `swift-tools 6.2` + `.defaultIsolation(MainActor.self)`가 Xcode 26 이상을 요구해 `macos-latest`나 macOS 15 이미지에서는 빌드가 깨진다.
- **iOS는 `CalcMoney.xcworkspace` + `CalcMoney.xctestplan`으로 연다.** `.xcodeproj`만 열면 `Domain`/`Data`/`Presentation` 패키지 테스트가 test plan에서 빠진다.
- **iOS의 경고→오류 승격은 커맨드라인이 아니라 Xcode 프로젝트 설정에 있다.** `xcodebuild` 인자로 넘기면 Firebase·GoogleMobileAds SPM 의존성 타깃까지 전파돼 컴파일이 깨진다.
- **로컬에서 Firestore를 읽으려면 App Check 디버그 토큰 등록이 필요하다.** 앱을 처음 디버그로 띄우면 Logcat/Xcode 콘솔에 디버그 토큰이 찍히는데, 이걸 Firebase 콘솔 App Check에 등록한다. 등록 전에는 enforce가 켜진 뒤부터 읽기가 거부된다.
- `local.properties`에는 `sdk.dir`만 있으면 된다. 앱은 환율 API 키가 필요 없다.

## 세부 문서

Claude Code는 아래 스킬을 필요할 때 알아서 불러온다. 스킬 자동 로드가 없는 에이전트는 해당 파일을 직접 읽는다.

| 상황 | 읽을 파일 |
| --- | --- |
| 코드 리뷰, PR·diff 검토 | `.claude/skills/review/SKILL.md` |
| Crashlytics 크래시 분석·수정 | `.claude/skills/analyze-crash/SKILL.md` |
| Android 새 화면/ViewModel 추가, MVI 패턴, 레이어 배선, Compose 작성 규칙 | `.claude/skills/mvi-screen/SKILL.md` |
| Android 테스트 작성 | `.claude/skills/testing/SKILL.md` |
| Android 릴리스·배포·CI, Firebase Functions 배포 | `.claude/skills/release-deploy/SKILL.md` |
| iOS 새 화면/ViewModel 추가, 레이어별 배선, iPad 방향·ATT 제약 | `.claude/skills/ios-screen/SKILL.md` |
| iOS 테스트 작성 | `.claude/skills/ios-testing/SKILL.md` |
| iOS 린트·빌드·CI | `.claude/skills/ios-ci/SKILL.md` |
| Maestro E2E Flow 작성·실행·리뷰, 테스트 id 부여 | `maestro-e2e` 플러그인의 `flows` 스킬 + `.claude/skills/maestro/SKILL.md`(이 저장소의 APP_ID·id 표·제약) |
| 코드에 남지 않는 결정(스토어 문구·이미지, 통화 목록, 하지 않기로 한 것) | `docs/decisions/README.md` |
| 배포 절차와 시크릿 목록 | `docs/continuous-deployment.md` |
| AGP 9 업그레이드 경위와 남은 확인 | `docs/agp-9-upgrade-plan.md` |
| Cloud Functions와 Firestore 데이터 구조 | `functions/README.md` |
| 사람용 소개, 개발 환경 준비 | `README.md` |
