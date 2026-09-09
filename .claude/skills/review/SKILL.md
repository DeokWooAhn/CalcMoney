---
name: review
description: CalcMoney 코드를 리뷰할 때 사용한다. "코드리뷰 해줘", "PR 검토해줘", "이 diff 봐줘", 완료된 구현을 검토해 달라는 요청이 해당된다. Android(Compose·Orbit)와 iOS(SwiftUI·자체 MVI) 양쪽 기준과 이 저장소에서 실제로 났던 사고 유형을 담고 있어 범용 코드리뷰 스킬보다 우선한다.
---

# CalcMoney 코드 리뷰

## 대상 범위

- 기본은 `git diff origin/master...HEAD`. 바뀐 코드와 그 코드가 직접 깨뜨릴 수 있는 곳만 본다.
- 변경과 무관한 기존 코드를 리팩터링 대상으로 지적하지 않는다.
- 한쪽 플랫폼만 바뀌었어도 반대쪽에 같은 코드가 있으면 함께 봐야 하는지 판단한다.

## 먼저 볼 것 — 이 저장소에서 실제로 났던 모양

- **조용히 실패하는 계약.** `SideEffectBus.send`가 이미 종료된 continuation에 `yield`하고
  이벤트를 버렸다(`e74de90`). 유닛 테스트도 린트도 초록이었고 다음 구독자만 못 받았다.
  큐에 넣고 반환하는 API는 실패를 알려주지 않는다. 전달 여부를 **결과값으로 확인**하는지 본다.
- **구독 시점이 곧 실행 시점.** Orbit `LazyCreateContainerDecorator`는 `stateFlow`/`sideEffectFlow`
  첫 구독에서 `onCreate`를 실행한다. `ExchangeViewModel`은 `container` `onCreate`에서
  `performLoadCurrencies()`를 부르므로, 상태·이펙트 수집 지점을 옮기면 데이터 로드 시점도 옮겨간다.
- **`@MainActor` 기본값과 UIKit 콜백.** Presentation 패키지가 `.defaultIsolation(MainActor.self)`라
  UIKit이 백그라운드에서 부르는 클로저에 `nonisolated`가 없으면 Swift 6 런타임이 SIGTRAP으로
  죽는다(`1b268fe`).
- **`@MainActor`만으로는 intent 직렬화가 안 된다.** `await` 지점에서 다른 intent가 끼어든다
  (`674116b`). 겹치면 안 되는 화면은 `CalculatorViewModel.enqueue(_:)` 패턴을 쓰는지 본다.
- **손으로 옮긴 매핑은 컴파일러가 안 잡는다.** 키→Intent처럼 값만 바뀌는 변환은 테스트로 덮는다.
  `÷`·`−`처럼 화면 표시 문자와 입력값이 다른 경우를 특히 확인한다.

## Android

- `Route`와 `Screen` 경계: `Route`는 ViewModel 배선·상태 수집·이펙트 처리, `Screen`은 순수 UI다.
  `Screen`과 재사용 컴포넌트에 `@Preview`가 있는지 본다.
- `collectSideEffect`의 `when` 분기를 유지했는지. 분기를 없애면 이펙트가 추가돼도 컴파일러가 잡지 못한다.
- Lazy 리스트의 `key`는 안정적인 식별자가 있을 때만 넣는다. `CurrencyPickerDialog`(`it.code`)와
  `FavoriteScreen`(`it.currency.code`)이 그 예다. `CalculatorHistory`처럼 값이 같은 항목이
  중복 저장될 수 있는 목록에 내용 기반 키를 주면 "Key was already used"로 죽는다.
  식별자가 없으면 키를 지어내지 말고 모델에 id를 넣는 쪽을 검토한다.
- 리소스 정리 경로. `AdMobBanner`의 `DisposableEffect`, `FavoriteViewModel`의 `loadJob?.cancel()`이
  기준이다. 리스너·플레이어·Job을 새로 잡으면 해제 경로가 있는지 본다.
- 로딩·빈 상태·에러·오프라인이 모두 표현되는지. 환율 실패는 `NotReady`, `NetworkUnavailable`,
  `RateNotFound`로 갈라진다.
- `:presentation`이 `:data`를 import하지 않는지. UI에서 Repository를 직접 부르지 않는지.
- 표시 문자열이 `strings.xml`에 있는지, 아이콘에 `contentDescription`이 있는지, 터치 영역이 48dp인지.

## iOS

- 레이어 방향: Domain에 `SwiftUI`/`UIKit`/`Data`/Firebase import가 없는지, Presentation이
  `import Data`를 하지 않는지.
- `SideEffectBus` 구독은 `.task { for await ... }`로 한다. `.onAppear` + `Task`는 취소가 붙지 않는다.
- UseCase는 `callAsFunction` struct 하나에 한 가지 일. Repository 프로토콜은 Domain, 구현은 Data.
- 유닛 테스트는 Swift Testing, UI 테스트만 XCTest. 테스트 이름은 한국어 + 언더스코어를 유지한다.
- 시간·난수·네트워크에 의존하는 테스트를 만들지 않았는지. 무한 대기 대신 실패하도록
  상한을 뒀는지(`SideEffectBusTests`의 시간 제한이 그 예다).

## 두 플랫폼 공통

- 캐시 TTL(12시간)과 stale 폴백 동작이 양쪽에서 어긋나지 않는지.
- `exchangeRates/latest` 문서 구조를 바꿨다면 `functions/`와 반대쪽 클라이언트도 함께 봤는지.
- 같은 화면·같은 개념을 한쪽만 고치고 끝내지 않았는지.
- 시크릿이 코드나 리소스로 새지 않았는지, 로그·Crashlytics에 사용자 입력값이 남지 않는지.

## 검증

지적하기 전에 현재 코드로 확인한다. 리뷰 도구가 준 지적도 그대로 믿지 않고 재현부터 한다.

```bash
./gradlew detekt test :app:assembleDebug
```

```bash
cd ios && swiftformat --lint . && swiftlint lint --strict
xcodebuild test -workspace CalcMoney.xcworkspace -scheme CalcMoney \
  -testPlan CalcMoney -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=latest'
```

리소스·매니페스트·Gradle을 건드렸으면 lint와 빌드까지 본다.

## 응답 형식

- 심각도 순으로 정렬한다. 동작이 깨지는 것 → 조용히 잘못되는 것 → 유지보수 문제 순.
- 각 항목에 `파일:줄`, 무엇이 문제인지, **어떤 입력이나 상황에서 터지는지**를 함께 적는다.
- 재현하거나 확인하지 못한 지적은 확인하지 못했다고 밝힌다. 추측을 단정으로 적지 않는다.
- detekt와 swiftlint가 통과한 포맷·스타일은 논의 대상이 아니다.
- 문제가 없으면 없다고 말한다. 채우기 위한 지적을 만들지 않는다.
