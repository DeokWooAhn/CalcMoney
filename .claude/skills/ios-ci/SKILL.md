---
name: ios-ci
description: iOS 린트(SwiftLint/SwiftFormat), 빌드, CI 워크플로 작업 시 사용. 아직 App Store/TestFlight 배포 파이프라인은 없음 — CI는 검증까지만.
---

# iOS 린트 / 빌드 / CI 규칙

Android와 달리 **iOS는 아직 릴리스 자동화가 없다.** CI(`​.github/workflows/ios-ci.yml`)는 lint + test까지만 하고, App Store/TestFlight 업로드 파이프라인은 구축돼 있지 않음. 이 스킬은 그 범위(린트·빌드·CI)만 다룬다.

## 로컬 검증 명령 (모두 `ios/`에서 실행)

```bash
swiftformat --lint .        # 포맷 검사 (CI와 동일)
swiftformat .                # 실제로 포맷 적용하고 싶을 때
swiftlint lint --strict     # 린트, 경고도 실패로 취급
xcodebuild analyze \
  -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
xcodebuild test \
  -workspace CalcMoney.xcworkspace -scheme CalcMoney -testPlan CalcMoney \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

PR 올리기 전 위 4개를 순서대로 로컬에서 통과시킬 것 — CI 파이프라인(`lint` job → `test` job)과 동일한 순서.

## CI 관련 주의사항 (Gotchas)

- **`runs-on: macos-26` 고정, `macos-latest` 아님.** `Package.swift`들이 `swift-tools-version: 6.2`와 `.defaultIsolation(MainActor.self)`를 쓰는데, 이건 Xcode 26 이상이 있어야 빌드된다. `macos-latest`는 이미지 전환기에 조용히 바뀔 수 있어서 버전을 고정해둔 것 — 낮추지 말 것.
- **SwiftLint 버전도 고정** (`SWIFTLINT_VERSION: 0.65.0`, `ios-ci.yml`). 러너 이미지에 SwiftLint가 기본 포함되어 있지 않아서 매번 다운로드하는데, 버전을 안 박으면 새 규칙이 추가될 때 코드 변경 없이 CI가 깨질 수 있다.
- **경고→오류 승격은 `xcodebuild` 커맨드라인 인자로 넘기지 않는다.** `SWIFT_TREAT_WARNINGS_AS_ERRORS = YES`는 `CalcMoney.xcodeproj`의 프로젝트 레벨 빌드 설정에 있음(커밋 `4199252`). 커맨드라인으로 넘기면 Firebase·GoogleMobileAds 같은 SPM 의존성 타깃까지 전파되고, 그 패키지들이 쓰는 `-suppress-warnings`와 충돌해서 컴파일이 깨진다. 새로 경고 억제를 추가하고 싶으면 반드시 프로젝트 설정 쪽에서.
- **워크스페이스로만 테스트를 실행할 것.** `CalcMoney.xctestplan` 하나가 앱 타깃 2개(`CalcMoneyTests`, `CalcMoneyUITests`) + 패키지 테스트 3개(`DomainTests`, `DataTests`, `PresentationTests`)를 전부 포함하는데, `.xcodeproj`만 열면 패키지 테스트가 test plan에서 해석되지 않고 빠진다.
- `.swiftlint.yml`은 레포 루트가 아니라 `ios/.swiftlint.yml`에 있음. CodeRabbit 설정(`.coderabbit.yaml`)도 `config_file: ios/.swiftlint.yml`로 명시적으로 경로를 지정해둠 — 새 도구를 추가할 때 이 경로 기준을 유지할 것.

## 린트/포맷 설정에서 의도적으로 다른 부분 (기본값이 아니라고 "고치지" 말 것)

- `disabled_rules: [trailing_comma]` — Swift 6 트레일링 콤마를 코드 전반(Package.swift 포함)에서 쓰기 때문에 의도적으로 끔
- `identifier_name`/`type_name`의 `validates_start_with_lowercase: off` — 한국어 식별자(테스트 함수명)에는 대소문자 개념이 없어서 끔
- `--test-case-name-format preserve` (swiftformat) — 한국어+언더스코어 테스트 이름을 raw identifier로 바꾸지 않도록
- `--ranges no-space` — `startPos..<pos`처럼 공백 없는 레인지 표기가 이 코드베이스(재귀 하강 파서)의 관례
- `--disable wrapIfStatementBodies`, `--disable wrapLoopBodies`, `--disable wrapPropertyBodies` — 한 줄짜리 `if`/computed property가 의도적으로 많음 (파서 코드, `var id: String { rawValue }` 같은 것들)

## 외부 SPM 의존성 추가 시

- `Package.swift` 변경 시 `Package.resolved`도 함께 커밋 (Android `libs.versions.toml` lockfile 개념과 동일하게 취급)
- 의존 방향 재확인: Domain은 아무것도 의존하면 안 되고, Data/Presentation은 Domain에만 의존. 새 패키지를 추가하면서 Presentation이 Data를 의존하게 만들면 즉시 아키텍처 위반
- 라이선스·유지보수 상태·앱 크기 영향을 PR 설명에 남길 것 (CodeRabbit도 이 항목을 요구하도록 설정돼 있음)
