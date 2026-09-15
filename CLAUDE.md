# CalcMoney (심플 환율 계산기)

한국수출입은행 환율을 보여주는 앱. **Android(Kotlin, 루트 Gradle 모듈)와 iOS(Swift, `ios/`) 두 클라이언트**가 같은 백엔드를 공유한다. Cloud Functions가 매일 환율을 받아 Firestore `exchangeRates/latest` 문서 하나에 쓰고, 두 앱 모두 그 문서를 읽어 로컬에 캐시(12h TTL)한다. Retrofit도 URLSession 직접 호출도 없음 — Firestore가 곧 네트워크 레이어.

**이 문서는 두 플랫폼 공통 배경만 다룬다.** 플랫폼별 세부 규칙은 각 스킬을 참조.

## Android — 모듈 구조 (Clean Architecture)

```
:app  ──→ :data, :domain, :presentation   Hilt 조립 전용 셸 (Kotlin 파일 1개)
:data ──→ :domain                          Repository 구현, Room, Firestore, DataStore
:presentation ──→ :domain                  Compose UI + Orbit MVI (:data를 모름)
:domain                                    순수 Kotlin JVM. model / repository 인터페이스 / usecase
```

- 화면 4개: Calculator / Exchange / Favorite / Settings — `presentation/src/main/java/com/ahn/presentation/main/Route.kt`
- MainActivity는 `:app`이 아니라 `:presentation`에 있음
- Settings·Favorite 화면은 activity-scoped `ExchangeViewModel`을 공유함 (`MainNavGraph.kt`의 `hiltViewModel(sharedOwner)`) — 이 owner를 바꾸면 화면 간 상태 공유가 조용히 깨진다

### Android 기술 스택

Kotlin 2.2 / JVM 17 / compileSdk 36 · Compose(Material3) · Hilt+KSP(kapt 없음) · Orbit MVI · Room(스키마 `data/schemas/`) · DataStore(Qualifier로 분리된 4개 스토어) · Firebase(Analytics·Crashlytics·Firestore) · AdMob+UMP · Kotest+MockK

### Android 자주 쓰는 명령

```bash
./gradlew detekt            # 린트 (ktlint 포함, autoCorrect 켜짐)
./gradlew test              # 전체 유닛 테스트
./gradlew :app:assembleDebug
```

### Android 주의사항 (Gotchas)

- **`./gradlew build` 는 로컬에서 실패한다.** 태스크 이름에 Release/assemble/bundle/build가 들어가면 AdMob·키스토어 시크릿 8개를 강제 검증(`error()`)하기 때문. 항상 `assembleDebug` / `test` / `detekt`를 쓸 것.
- **versionCode를 손으로 고치지 말 것.** 릴리스는 `v*` 태그 push → CI가 `10000 + GITHUB_RUN_NUMBER`로 계산. 로컬 기본값은 `app/build.gradle.kts`의 `DEFAULT_VERSION_CODE`.
- detekt는 `dev.detekt` 2.0 알파 (구 `io.gitlab.arturbosch.detekt` 아님). 모듈별 `detekt-baseline.xml` 존재.
- 테스트는 JUnit5 플랫폼(`useJUnitPlatform()`) 위의 Kotest — JUnit4 러너로 돌리면 안 됨.
- 앱은 환율 API를 직접 호출하지 않는다. 수출입은행 키는 Cloud Functions의 Firebase secret `KOREA_EXIM_API_KEY` 하나뿐 (`local.properties`에는 `sdk.dir`만 있으면 됨).

## iOS — 패키지 구조 (SPM 로컬 패키지, Android와 같은 레이어 방향)

```
ios/CalcMoney/          앱 타깃. AppContainer.swift(수동 DI, Hilt 모듈 대응) + CalcMoneyApp.swift만 있음
ios/Packages/Domain/    순수 Swift, 의존성 0개. model / repository 프로토콜 / usecase
ios/Packages/Data/      Domain + Firebase(Firestore) SPM 의존. Repository 구현, UserDefaults, SwiftData
ios/Packages/Presentation/  Domain에만 의존(Data 모름). SwiftUI + Observation 기반 자체 MVI
```

- 화면 4개는 Android와 동일: Calculator / Exchange / Favorite / Setting
- `AppContainer`가 만드는 `ExchangeViewModel`은 Exchange/Favorite/Setting 탭이 공유 — Android의 activity-scoped `hiltViewModel(sharedOwner)`와 같은 역할
- 워크스페이스(`CalcMoney.xcworkspace`) + `CalcMoney.xctestplan`으로 열어야 패키지 테스트 3개가 잡힘. `.xcodeproj`만 열면 빠짐

### iOS 기술 스택

Swift 6.2 tools / iOS 17+ · SwiftUI · Observation(`@Observable`, Combine·TCA 없음) · `AsyncStream`(Orbit `Flow`/side-effect 대응, Orbit 라이브러리 자체는 없고 직접 구현) · SwiftData(로컬 캐시) · Firebase(Firestore·Analytics·Crashlytics) · AdMob+UMP · Swift Testing(유닛) + XCTest(UI)

### iOS 자주 쓰는 명령 (모두 `ios/`에서 실행)

```bash
swiftformat --lint .        # 포맷 검사
swiftlint lint --strict     # 린트
xcodebuild test -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -testPlan CalcMoney -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

### iOS 주의사항 (Gotchas)

- **CI는 `macos-26` 고정.** `swift-tools 6.2` + `.defaultIsolation(MainActor.self)`가 Xcode 26 이상을 요구 — `macos-latest`나 macOS 15 이미지에서는 빌드가 깨진다.
- **경고→오류 승격은 커맨드라인이 아니라 Xcode 프로젝트 설정에 있음.** `xcodebuild` 인자로 넘기면 Firebase·GoogleMobileAds SPM 의존성 타깃까지 전파돼 컴파일이 깨진다.
- Presentation 패키지 전체가 `.defaultIsolation(MainActor.self)`라 기본이 MainActor. **동적 색상 클로저(`Color.dynamic` 등)처럼 UIKit이 백그라운드 스레드에서 부르는 클로저는 반드시 `nonisolated`** — 안 그러면 Swift 6 런타임이 SIGTRAP으로 크래시한다 (`1b268fe` 참조).
- 테스트 함수명은 **한글 + 언더스코어**가 이 프로젝트 컨벤션 (`func 등호_연타는_기록을_중복_저장하지_않는다()`). 영어나 raw identifier로 바꾸지 말 것 — `.swiftformat`/`.swiftlint.yml`이 이를 보존하도록 이미 설정돼 있음.
- 앱은 환율 API를 직접 호출하지 않는다 (Android와 동일). Firestore `exchangeRates/latest` 문서 구조를 바꾸면 Cloud Functions와 Android `:data` 모듈도 함께 확인할 것.

## 공통 주의사항

- 기본 브랜치는 `main`이 아니라 **`master`**.
- 주석·KDoc·테스트 설명·커밋 메시지는 **한국어**, 코드 식별자·로그는 영어.

## 세부 규칙 (필요할 때 해당 스킬 참조)

| 작업 | 스킬 |
|---|---|
| 코드 리뷰, PR·diff 검토 | `.claude/skills/review/SKILL.md` |
| Android 새 화면/ViewModel 추가, MVI 패턴 | `.claude/skills/mvi-screen/SKILL.md` |
| Android 테스트 작성 | `.claude/skills/testing/SKILL.md` |
| Android 릴리스·배포·CI | `.claude/skills/release-deploy/SKILL.md` |
| iOS 새 화면/ViewModel 추가, 레이어별 배선 | `.claude/skills/ios-screen/SKILL.md` |
| iOS 테스트 작성 | `.claude/skills/ios-testing/SKILL.md` |
| iOS 린트·빌드·CI | `.claude/skills/ios-ci/SKILL.md` |
