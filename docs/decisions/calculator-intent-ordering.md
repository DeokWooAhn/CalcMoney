# 두 플랫폼 계산기는 인텐트가 겹쳐도 같은 순서로 동작한다

- 정한 날: 2026-11-14
- 근거: 2026-11-14 대화(iOS 구조 개선 검토), Orbit 9.0.0 `RealContainer.kt` 소스, 아래 "확인 방법"의 테스트
- 적용: Android·iOS 계산기 ViewModel. 다른 화면에 기다리는 인텐트(저장·조회)를 더할 때도 같은 원칙을 쓴다.

## 규칙

두 플랫폼은 State·Action·단방향 흐름과 동작을 맞추고, 화면 수명과 상태 연결은 각 플랫폼 도구에 맞춘다.
Android는 Orbit·AndroidX, iOS는 SwiftUI·Observation이다. MVI를 버리거나 모든 Intent를 Binding으로 바꾸지 않는다.

계산기의 기다리는 일은 셋으로 나눈다.

- **확정**: 상태의 주인은 ViewModel이다. 계산 결과·통화 선택은 첫 `await` 전에 상태에 반영한다.
  다음 인텐트는 반영된 상태를 읽는다. Android는 `reduce` 안에서 순수 계산만 한다. `reduce`는 `StateFlow.update`라
  다른 변경과 겹치면 블록을 다시 실행하므로, 저장·이벤트는 블록 밖에서 실제로 반영된 실행의 결과로 처리한다.
- **저장**: 로컬 저장은 탭한 순서대로 하나씩 처리한다. 기록 추가·삭제, 통화 선택, 즐겨찾기, 리뷰·광고 횟수가 여기에 든다.
  Android는 `Mutex`를 쓰고, 인텐트가 상태를 확정한 뒤 다른 중단 없이 바로 잠금을 요청한다.
  iOS는 저장만 이어 붙이는 Task 사슬(`enqueuePersistence`)을 쓴다. 범용 인텐트 큐는 두지 않는다.
- **조회**: 환율 조회는 저장 순서에 넣지 않고 기다리지도 않는다. 새 조회를 시작할 때 이전 조회를 취소하고 요청 번호를 올린다.
  응답이 오면 요청 번호, 취소 여부, 통화쌍을 모두 확인한 뒤에만 반영한다. 오류 스낵바도 같은 조건에서만 띄운다.

계산 결과는 저장을 기다리지 않고 바로 보여 준다. 기록 목록에는 저장에 성공한 뒤에만 넣고, 실패하면 스낵바를 띄운다.

두 플랫폼은 아래 시나리오를 같은 테스트로 지킨다. 저장·조회에 지연을 넣어 인텐트가 겹치게 만든다.

| # | 상황 | 기대 동작 |
| --- | --- | --- |
| 1 | `1+2` 뒤 "=" 연타, 기록 저장 지연 | 저장 `[1+2, 3+2]`, 화면 `5` |
| 2 | "=" → 기록 삭제 → "=", 기록 저장 지연 | 저장 순서 추가·삭제·추가, 남는 기록 `[3+2]` |
| 3 | 통화 USD(저장 느림) → EUR(저장 빠름) | 저장 `[USD, EUR]`, 화면 EUR |
| 4-1 | USD → EUR → USD, 첫 USD 응답이 가장 늦음 | 마지막 요청의 환율만 남는다 |
| 4-2 | 4-1에서 첫 USD 요청이 늦게 실패 | 지난 요청의 오류 스낵바를 띄우지 않는다 |
| 5 | 시작 로드가 끝나기 전에 "=" | 결과가 바로 나온다 |
| 6 | 통화를 바꾼 뒤 조회가 끝나기 전에 "=" | 결과가 바로 나온다 |
| 7 | 기록 저장 실패 | 결과는 보이고, 기록 목록에는 없고, 저장 실패 스낵바가 뜬다 |
| 8 | 저장하는 동안 기록 흐름이 기존 기록을 늦게 내보냄 | 기존 기록이 화면 목록에서 빠지지 않는다 |

환율 가짜 저장소는 실제 Firestore 요청처럼 취소돼도 끝까지 응답한다. 그래야 4-1·4-2가 취소가 아니라 요청 번호 확인을 검증한다.
기록 목록은 저장하는 동안 목록이 그대로였을 때만 새 기록을 붙이고, 그사이 저장소 흐름이 바꿨으면 흐름을 따른다.

## 이유

- Orbit 9는 인텐트를 받은 순서대로 **시작**만 하고 각자 코루틴으로 돌린다. 한 인텐트가 `await`에서 멈추면 다음 인텐트가 끼어든다.
  iOS 큐(커밋 `674116b`)의 주석은 "Android는 Orbit이 직렬화해 준다"고 적었지만 사실이 아니었다.
- 2026-11-14 위 시나리오를 고치기 전 코드로 돌린 결과 두 플랫폼이 서로 다른 곳에서 틀렸다.
  - Android는 1·2·3·4-1·4-2가 실패했다. "=" 연타에 `1+2`가 두 번 저장됐다.
  - iOS는 4-2·5·6이 실패했다. 범용 큐가 "="를 네트워크 조회 뒤에 세웠다.
- "Android와 같다"는 변경을 미룰 근거가 아니라, 양쪽의 기대 동작을 함께 검증할 이유다.
- 범용 큐 하나로 모든 인텐트를 줄 세우면 순서는 지켜도 계산이 네트워크를 기다린다.
  여행지 로밍처럼 느린 네트워크에서 쓰는 앱이라 이 대기가 체감된다.

## 코드

- Android: `presentation/.../calculator/CalculatorViewModel.kt`, `CalculatorExpressionReducer.calculate`
- iOS: `ios/Packages/Presentation/Sources/Presentation/Screen/Calculator/CalculatorViewModel.swift`

## 확인 방법

- Android: `presentation/src/test/.../calculator/CalculatorViewModelOrderingTest.kt`
- iOS: `ios/Packages/Presentation/Tests/PresentationTests/Screen/Calculator/CalculatorViewModelOrderingTests.swift`.
  1은 `CalculatorViewModelCalculateTests`, 3은 `CalculatorViewModelCurrencyTests`가 맡는다.
  iOS 가짜 환율 저장소는 지연을 `Task.detached`에서 기다려 취소를 무시한다.
