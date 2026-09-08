---
name: ios-screen
description: iOS(ios/Packages)에 새 화면 추가, ViewModel 작성, 레이어 배선을 할 때 사용. Domain/Data/Presentation 의존 규칙과 Orbit MVI를 흉내낸 자체 패턴 포함.
---

# iOS 화면 / 레이어 배선 규칙

설명보다 아래 레퍼런스 파일을 먼저 읽고 그 구조를 그대로 따라할 것. Android `mvi-screen` 스킬의 iOS 대응이며, 실제로 Android 코드를 Swift로 옮긴 프로젝트라 대부분의 개념이 1:1로 대응한다.

## 레이어 의존 방향 (절대 어기면 안 됨)

```
Presentation ──→ Domain ←── Data
```

- **Domain**은 Foundation 정도까지만 의존. `import SwiftUI`, `import UIKit`, `import Data`, Firebase 계열 import가 들어오면 안 됨.
- **Presentation은 `import Data`를 하지 않는다.** Domain에만 의존 — CodeRabbit도 이걸 자동으로 지적하도록 설정돼 있음(`.coderabbit.yaml`).
- Repository는 Domain에 프로토콜로만 선언, 구현은 Data에.

## 화면 하나 = 파일 여러 개 (`Packages/Presentation/Sources/Presentation/Screen/<Feature>/`)

정석 레퍼런스: `ios/Packages/Presentation/Sources/Presentation/Screen/Exchange/`

| 파일 | 내용|
|---|---|
| `<Feature>State.swift` | 화면 상태 (Android Contract의 State) |
| `<Feature>Intent.swift` | 사용자 액션 sealed 대응 (Swift `enum`) |
| `<Feature>SideEffect.swift` | 일회성 이벤트 (토스트, 네비게이션 등) |
| `<Feature>ViewModel.swift` | `@MainActor @Observable final class` |
| `<Feature>View.swift` | SwiftUI 뷰 |

간단한 화면(Favorite처럼 Intent/SideEffect 없이 State만 있는 경우)은 파일을 줄여도 됨 — `Screen/Favorite/`가 그 예시. Setting처럼 자체 ViewModel 없이 공유 ViewModel(`ExchangeViewModel`, `MainViewModel`)을 그대로 읽는 것도 허용 — Android SettingScreen과 같은 패턴.

## ViewModel 구조 (ExchangeViewModel / CalculatorViewModel이 정석)

```swift
@MainActor
@Observable
public final class <Feature>ViewModel {
    public private(set) var state = <Feature>State()
    public func sideEffects() -> AsyncStream<<Feature>SideEffect>
    public func send(_ intent: <Feature>Intent)
}
```

- 진입점은 단일 `send(_ intent:)` — Orbit `processIntent`의 대응
- 상태 변경은 `@Observable` 매크로로 (Combine의 `@Published`/`ObservableObject` 사용 금지)
- 백그라운드 Task나 UseCase 홀더처럼 관찰 대상이 아닌 저장 프로퍼티는 `@ObservationIgnored`
- one-shot 이벤트는 `SideEffectBus<Effect>` (`Util/SideEffectBus.swift`)로 내보내고, View에서 `.task { for await effect in viewModel.sideEffects() { ... } }`로 소비 (`.onAppear` + Task 금지, 취소가 자동으로 안 붙음)
- **비동기 intent가 겹치면 상태가 꼬이는 화면은 직렬화가 필요하다.** `@MainActor`만으로는 `await` 지점에서 다른 intent가 끼어드는 걸 막지 못함 — `CalculatorViewModel`의 `enqueue(_:)` continuation-체이닝 패턴 참조 (커밋 `674116b`가 이 문제를 실제로 겪고 고친 사례)
- 순수 상태 계산이 복잡하면 별도 struct로 추출 — `CalculatorExpressionReducer.swift` 참조

## UseCase 규칙 (`Packages/Domain/Sources/Domain/<feature>/UseCase/`)

- `callAsFunction`을 쓰는 struct, 한 UseCase는 한 가지 일만
- 생성자로 Repository 프로토콜을 주입받아 테스트 가능하게 유지
- ViewModel에는 개별 UseCase 대신 **홀더 struct로 묶어서 하나만 주입**: `ExchangeUseCases`, `FavoriteUseCases` 참조 (Android UseCases 홀더와 동일한 이유)
- 반응형 스트림은 `AsyncStream` 사용 (Kotlin `Flow` 대응) — `func histories() -> AsyncStream<[CalculatorHistory]>` 형태

## Data 레이어 (`Packages/Data/Sources/Data/<feature>/`)

- 구조: `Local/`(UserDefaults 또는 SwiftData datasource) + `Remote/`(Firestore) + `Mapper/` + `Repository/`
- 정석 레퍼런스: `Data/Exchange/Repository/ExchangeRateRepositoryImpl.swift` — actor 기반, 12h TTL(Android와 동일), 원격 실패 시 stale 캐시 폴백, 동시 refresh 요청은 in-flight Task 공유로 중복 방지
- Firestore 응답 파싱은 Firebase-free한 순수 파서로 분리 (`ExchangeRateDocumentParser` 참조) — Firebase 없이도 단위 테스트 가능하게
- Repository 구현은 Firestore 문서/DTO를 그대로 반환하지 말고 **반드시 Domain 타입으로 변환**해서 반환
- 새 UserDefaults 스토어가 필요하면 전용 suite로 분리: `UserDefaults.suite(named:)` → `com.ahn.CalcMoney.<name>` (Android DataStore Qualifier 분리와 같은 이유)
- SwiftData 모델 변경 시 기존 레코드 마이그레이션 고려 — Room처럼 자동 스키마 export는 없음
- `exchangeRates/latest` 문서 구조를 바꾸면 Cloud Functions(`functions/`)와 Android `:data` 모듈도 함께 확인

## DI (AppContainer, Hilt 없음)

- `ios/CalcMoney/CalcMoney/AppContainer.swift`가 유일한 배선 지점. Hilt 모듈 대응이지만 프레임워크 없이 수동 생성자 주입
- 새 Repository/UseCase를 추가하면 `AppContainer` 내부 `Repositories`/`UseCases` 조립 struct에 추가
- `ExchangeViewModel`은 `AppContainer`에서 한 번만 생성해서 Exchange/Favorite/Setting 탭에 공유 주입 — 화면별로 새로 만들지 말 것

## 동시성 주의사항

- Presentation 패키지 전체가 `.defaultIsolation(MainActor.self)` (Package.swift 설정) — 대부분의 타입이 기본적으로 MainActor. 불필요하게 `@MainActor`를 중복 표기하지 말 것
- **UIKit이 메인 스레드 밖에서 호출하는 클로저는 `nonisolated`를 명시해야 한다.** 예: `Color.dynamic(light:dark:)`의 `UIColor { traits in ... }` provider — SwiftUI의 `AsyncRenderer`가 백그라운드에서 호출하는데, MainActor 격리를 상속받으면 Swift 6 런타임이 SIGTRAP으로 크래시한다 (커밋 `1b268fe`가 실제 사례)
