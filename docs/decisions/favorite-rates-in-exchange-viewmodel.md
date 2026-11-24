# 즐겨찾기 탭은 환율 입력의 다른 보기이고, 즐겨찾기 카드는 ExchangeViewModel이 만든다

- 정한 날: 2026-11-24
- 근거: 2026-11-24 대화(화면 구조 개선 작업 B), Android #132, iOS #135, 두 PR의 교차 리뷰(codex)와 CodeRabbit 지적
- 적용: Android·iOS 즐겨찾기 화면과 공유 `ExchangeViewModel`. 화면 두 개가 같은 입력을 보여 줄 때도 같은 기준을 쓴다.

## 규칙

- **즐겨찾기 탭은 환율 화면 입력의 다른 보기다.** 기준 통화·금액이 환율 화면과 같은 값이고, 화면도 `ExchangeViewModel`에 Intent를 보낸다.
  그래서 즐겨찾기 카드도 그 입력을 가진 `ExchangeViewModel`이 만든다. 즐겨찾기 전용 ViewModel을 두지 않는다.
- **화면은 ViewModel 사이를 잇지 않는다.** 상태를 그리고 Intent를 보내기만 한다.
  언제 다시 모을지는 ViewModel 안에서 정한다. 화면의 `LaunchedEffect`·`.onChange`로 다른 ViewModel에 값을 옮기지 않는다.
- 조회 로직은 상태 없는 `FavoriteRatesLoader`에 두고, ViewModel에는 트리거와 상태 갱신만 둔다.
- 다시 모으는 규칙(두 플랫폼 같음)
  - 입력(기준 통화, 즐겨찾기, 통화 목록, 환율 기준일·받은 시각, 수동 새로고침 성공 횟수)이 바뀔 때만 다시 모은다. 입력이 바뀌면 앞 조회를 취소한다
  - 금액만 바뀌면 다시 조회하지 않고, **마지막으로 조회를 마친 (입력, 환율) 묶음**으로 카드만 다시 만든다. 재조회 중이어도 새 입력과 옛 환율을 섞지 않는다
  - 카드를 비울 때는 묶음도 비운다. 조회를 마치면 묶음을 카드보다 먼저 바꾼다
  - 수동 새로고침이 성공하면 같은 서버 문서여도 다시 모은다(`ratesRefreshCount`). 실패하면 입력이 그대로라 즐겨찾기 화면의 실패 분기가 끝까지 유지된다
  - 새로고침 중에도 이전 카드는 지우지 않는다

## 이유

- 두 ViewModel을 화면이 이어 주던 구조(2026-11-24 이전)는 같은 입력이 화면을 다시 그릴 때마다 또 들어와, 이를 무시하는 검사(`currentRateInput`)가 따로 필요했다.
  입력을 가진 쪽이 카드를 만들면 그런 검사가 필요 없다.
- 데이터 계층은 이미 하나로 공유되고 있었다. 통화 목록 원격 조회는 저장소가 합치고(Android `refreshMutex`, iOS actor + `inFlightRefresh`),
  즐겨찾기 목록도 저장소의 `Flow`/`AsyncStream` 하나를 구독한다. 문제는 화면 계층에만 있었다.
- 플랫폼별 트리거는 각 도구에 맞췄다.
  - Android: `container.stateFlow.map { 입력 }.distinctUntilChanged().collectLatest { }`. `collectLatest`가 앞 조회 취소를 맡는다
  - iOS: 대상이 iOS 17이라 `Observations`(iOS 26+)를 쓸 수 없다. `state`의 `didSet`에서 입력·금액이 바뀌었는지 보고 조회 Task를 다시 시작한다
- 재조회 중 금액 변경 규칙은 Android 교차 리뷰에서 나온 회귀(재조회 중 금액이 카드에 반영되지 않음)를 고치며 정했다.
  옛 `FavoriteViewModel`은 기준 통화를 바꾼 재조회 중에 새 기준 통화와 옛 환율을 섞은 틀린 카드를 만들 수 있었다.
- `ratesRefreshCount`는 Android CodeRabbit 지적에서 나왔다. 환율 날짜·받은 시각은 서버 문서 값이라, 같은 문서를 다시 받으면 바뀌지 않아 재조회 신호가 되지 못한다.

## 하지 않기로 한 것

| 안 | 이유 |
| --- | --- |
| 통화 목록·즐겨찾기 공유 저장소(작업 메모의 처음 방향) | 즐겨찾기 화면은 기준 통화·금액을 여전히 환율 쪽에서 받아야 해 동기화가 남는다. 원격 조회는 이미 합쳐져 있어 얻는 것이 로컬 캐시 읽기 한 번뿐이다 |
| 환율 입력 전체를 공유 세션 저장소로 빼고 두 ViewModel이 구독 | `ExchangeViewModel`의 요청 순서 검사까지 옮겨야 한다. 바꾸는 양에 비해 얻는 것이 위 규칙과 같다 |
| `FavoriteViewModel`이 `ExchangeViewModel`을 직접 구독 | ViewModel이 ViewModel에 기댄다. Android는 Hilt로 주입하기도 어렵다 |
| iOS 즐겨찾기 화면을 `.task(id:)`로 바꾸기(구조 개선안 1의 남은 부분) | 화면의 동기화 코드가 사라져 할 일이 없어졌다 |

## 받아들인 대가

- **탭에 들어오는 것만으로는 다시 조회하지 않는다.** 예전에는 카드가 비어 있고 로딩 중이 아니면 탭 진입이 같은 입력으로 다시 조회했다.
  다시 시도는 새로고침이 맡는다. 이 차이를 되살리려고 화면에 트리거를 다시 넣지 않는다.
- **즐겨찾기 탭을 열지 않아도 앱을 켜면 즐겨찾기 환율을 모은다.** 캐시가 신선하거나 원격 조회가 성공하면 원격 조회는 늘지 않는다(즐겨찾기 수만큼 로컬 조회).
  캐시가 만료되고 원격이 계속 실패하는 경우는 원격 재시도 쿨다운(`exchange-rate-remote-retry-cooldown.md`)이 묶는다.
- `ExchangeViewModel`의 책임이 하나 늘었다(환율 화면 입력 + 그 입력의 즐겨찾기 보기).

## 범위 밖

- 계산기의 통화 목록 로드는 그대로다. 다른 탭에서 새로고침해도 계산기의 통화 목록은 다시 열 때까지 옛 목록이다.
  서버가 통화를 더하는 일이 드물어 지금은 문제 삼지 않는다. 필요해지면 그때 공유 저장소를 다시 본다.
- 설정 화면은 환율 기준일만 읽어 바뀌지 않았다.

## 코드

- Android: `presentation/.../exchange/ExchangeViewModel.kt`(`observeFavoriteRates`), `FavoriteRatesLoader.kt`, `ExchangeContract.kt`(`FavoriteRates`, `ratesRefreshCount`)
- iOS: `Presentation/Screen/Exchange/ExchangeViewModel+FavoriteRates.swift`, `FavoriteRatesLoader.swift`, `ExchangeState.swift`

## 확인 방법

- Android: `FavoriteRatesLoaderTest`, `ExchangeFavoriteRatesTest`. iOS: `FavoriteRatesLoaderTests`, `ExchangeViewModelFavoriteRatesTests`, `ExchangeViewModelRefreshTests`
- 두 플랫폼 모두 결함을 일부러 넣어 테스트가 실패하는 것을 확인했다(같은 서버 문서 새로고침, 입력이 그대로여도 다시 모으기, 취소된 앞 조회가 카드 쓰기)
- Maestro `smoke_exchange_favorite`: Android 실기기 통과(2026-11-24). iOS는 로컬(Xcode 27, iOS 26.0 시뮬레이터)에서 환율 화면의 `hideKeyboard`가
  master 빌드에서도 실패해, 키보드 닫기만 `swipe`로 바꾼 사본으로 즐겨찾기 카드까지 확인했다. CI(#135 마지막 커밋)의 iOS smoke는 통과했다
