---
name: ios-testing
description: iOS(ios/Packages, ios/CalcMoney) 유닛/UI 테스트를 작성하거나 수정할 때 사용. Swift Testing 컨벤션과 한국어 테스트 이름 규칙.
---

# iOS 테스트 작성 규칙

## 프레임워크 구분 — 반드시 지킬 것

- **유닛 테스트는 Swift Testing** (`import Testing`, `@Suite("...")`, `@Test("...")`, `#expect(...)`) — `DomainTests`, `DataTests`, `PresentationTests`, `CalcMoneyTests` 전부 해당
- **UI 테스트만 XCTest** (`XCTestCase`, `XCUIApplication`) — `CalcMoneyUITests`. Swift Testing은 UI 자동화를 지원하지 않기 때문에 의도적으로 다름
- 새 유닛 테스트에 XCTest를 섞지 말 것. 반대로 UI 테스트를 Swift Testing으로 옮기려 하지 말 것

## 테스트 이름: 한국어 + 언더스코어 (이 프로젝트 고유 컨벤션)

```swift
@Test("등호 연타는 기록을 중복 저장하지 않는다")
func 등호_연타는_기록을_중복_저장하지_않는다() async throws { ... }
```

- `@Test`/`@Suite`의 display name과 함수명 모두 한국어
- **영어로 바꾸거나 raw identifier(백틱)로 바꾸지 말 것** — `ios/.swiftformat`의 `--test-case-name-format preserve`, `ios/.swiftlint.yml`의 `identifier_name.validates_start_with_lowercase: off`가 이 컨벤션을 보존하려고 명시적으로 설정된 것
- 정석 레퍼런스: `ios/Packages/Domain/Tests/DomainTests/Favorite/ToggleFavoriteCurrencyUseCaseTests.swift`

## 검증 패턴

- `#expect(...)`로 무엇을 검증하는지 명확하게. 단언 없는 테스트는 만들지 말 것
- 실패 가능 경로(빈 입력, 0, 음수, 무한대, 통화 코드 없음 등)를 빠뜨리지 말 것
- 테스트 간 공유 가변 상태 금지 — `@Suite`는 인스턴스가 테스트마다 새로 만들어지므로 그 특성을 활용
- 시간·난수·네트워크에 의존하는 비결정적 테스트를 만들지 말고 주입으로 해결
  - Repository 테스트는 `now:` 같은 Clock 파라미터 주입 — `DataTests/Exchange/ExchangeRateRepositoryImplTests.swift` 참조 (in-memory SwiftData store + 주입된 clock으로 TTL 만료 테스트)
  - Repository/UseCase mock은 Android MockK 대신 **손으로 만든 Stub 클래스**(`@unchecked Sendable`)를 사용 — `DomainTests`의 Stub 패턴 참조. 외부 모킹 라이브러리 없음
- 동시성 버그(레이스, 직렬화 누락)를 재현하는 테스트는 `Task.sleep`이나 지연 fake로 타이밍을 강제 — `PresentationTests/Screen/Calculator/CalculatorTestDoubles.swift`, `CalculatorViewModelCalculateTests.swift` 참조
- 새 UseCase나 Repository를 추가했는데 대응 테스트가 없으면 안 됨

## ViewModel 테스트

- `@MainActor` 컨텍스트에서 실행 (ViewModel 자체가 `@MainActor`이므로)
- `AsyncStream` 기반 side effect는 `for await`로 순회하며 검증
- Intent 직렬화가 필요한 화면(`ios-screen` 스킬 참조)은 겹치는 intent를 연속 발행해서 순서·중복 방지를 검증 — `CalculatorViewModelCalculateTests.swift`가 실제로 등호 연타 중복 방지를 이렇게 검증함

## 실행 (모두 `ios/`에서)

```bash
swiftformat --lint .        # 포맷 검사만, 자동 수정은 --lint 없이
swiftlint lint --strict     # 경고도 실패 처리
xcodebuild test \
  -workspace CalcMoney.xcworkspace \
  -scheme CalcMoney \
  -testPlan CalcMoney \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

- **`.xcodeproj`만 열어서 테스트를 돌리면 안 된다.** `Domain`/`Data`/`Presentation` 패키지 테스트가 test plan에서 해석되지 않고 빠진다 — 반드시 `.xcworkspace` + `-testPlan CalcMoney` 조합으로 실행
- `CalcMoneyUITests/testLaunch()`와 `testLaunchPerformance()`는 test plan에서 의도적으로 skip 처리돼 있음(CI 불안정으로 제외, 커밋 `7b5d8c0`) — 다시 활성화하지 말 것
- CI는 `macos-26` 고정 — swift-tools 6.2와 `.defaultIsolation(MainActor.self)`가 Xcode 26 이상을 요구하기 때문에 로컬 재현도 이 버전 이상에서
