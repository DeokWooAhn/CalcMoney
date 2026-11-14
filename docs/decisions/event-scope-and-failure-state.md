# 자동으로 돈 일의 실패는 상태로 보여 주고, 스낵바는 사용자가 한 동작에만 쓴다

- 정한 날: 2026-11-15
- 근거: 2026-11-14~15 대화(화면 구조 개선 작업 A), Orbit 9.0.0 `RealContainer.kt`·`ContainerHostExtensions.kt` 소스,
  아래 "확인 방법"의 재현 테스트와 그 교차 리뷰
- 적용: Android·iOS 계산기·환율·즐겨찾기·설정 화면. 다른 화면에 일회성 이벤트를 더할 때도 같은 기준을 쓴다.

## 규칙

일회성 이벤트(스낵바)와 지속 상태를 가른다.

- **사용자가 한 동작의 결과는 스낵바로 띄운다.** 새로고침 성공·실패, 즐겨찾기 추가·제거가 여기에 든다.
  그 동작을 한 화면이 떠 있으므로 그 화면이 받는다.
- **자동으로 돌아간 일의 실패는 상태로 보여 준다.** 시작 로드 실패, 통화·금액이 바뀌며 딸려 도는 환율 조회 실패가 여기에 든다.
  환율·즐겨찾기 화면에 "불러오지 못함"과 다시 시도를 둔다.
- 설정 화면은 환율 일회성 이벤트를 구독하지 않는다. 환율 기준일 표시만 쓴다.

State가 실패를 담아야 한다. `isLoading`만으로는 화면이 "데이터 없음"과 "불러오지 못함"을 구분하지 못한다.

**실패 표시는 그 화면에서 잃는 것만큼 자리를 차지한다.**
환율 화면은 통화 목록을 못 받으면 쓸 수 없으므로 화면 전체를 실패로 두고,
환율만 못 받으면 통화는 고를 수 있으므로 환율이 있던 자리에만 둔다.
계산기는 환율이 없어도 계산이 되므로 화면을 덮지 않고 통화 선택 줄 아래에만 둔다.

**실패를 색으로 알리지 않는다.** 빨강을 쓰면 계산기 AC·⌫의 `CalculatorDestructive`와 뜻이 겹친다.
같은 빨강이 "지우기"와 "실패"를 함께 뜻하게 두지 않는다.
실패는 문구와 아이콘, 그리고 다시 시도 버튼의 존재로 드러낸다. 강조색(`Orange`)은 사용자가 누르는 것에만 쓴다.
빈 상태와 실패 상태는 **다시 시도 버튼의 유무**로 갈린다.

## 이유

- **두 플랫폼의 전달 방식이 다르다.** Android Orbit은 `Channel(BUFFERED, 64)` + `receiveAsFlow()`라
  한 이벤트를 구독자 **하나**에게만 준다(fan-out). iOS `SideEffectBus.send`는 살아 있는 구독자 **전부**에 보낸다(브로드캐스트).
  일회성 이벤트에 기대어 "어느 화면에 띄울지"를 풀면 이 차이를 플랫폼마다 따로 메워야 한다.
- **상태에는 그 차이가 없다.** StateFlow와 Observation 모두 구독자 전부에게 최신값을 준다.
- **구독 공백이 실재한다.** Android `collectSideEffect`는 `LaunchedEffect` + `repeatOnLifecycle(STARTED)`라
  탭 이동과 백그라운드 전환에서 끊긴다. 그 사이 난 오류는 버려지지 않고 쌓였다가 다음에 연 화면에 뜬다.
  iOS `SideEffectBus`는 구독자가 없으면 64개까지 쌓았다가 다음 구독자에게 보낸다. 모양은 달라도 결과가 같다.
- **출처 화면을 이벤트에 붙이는 방법은 Android에서 위험하다.** fan-out이라 탭 전환 중 두 화면이 잠깐 같이 구성되면
  출처가 아닌 화면이 먼저 받아 버리고, 정작 그 화면은 영영 못 본다.
- **버리는 방법은 실패를 어디서도 못 보게 한다.** State에 실패 필드가 없어서 빈 화면만 남는다.
  Android에서 `repeatOnSubscription`으로 구현하면 orbit-test 하네스가 `AlwaysSubscribedCounter`를 강제해
  그 하네스로는 검증할 수 없다는 문제도 있다.

## 아직 확인하지 않은 것

규칙을 넓게 읽지 않도록 적어 둔다.

- Android: 배달 도중 구독이 끊길 때의 유실(`receiveAsFlow()`는 그 순간의 항목을 잃을 수 있다),
  버퍼 한도 64를 넘길 때의 동작(`postSideEffect`가 suspend해서 그 intent가 멈춘다),
  탭 전환 시 `LaunchedEffect` 취소 시점
- 두 플랫폼: 사용자 눈에 스낵바가 어느 화면에 뜨는지. 계측 테스트가 없어 유닛 테스트로는 못 본다.
  Maestro나 손으로 확인할 일이다.

## 코드

- Android — 2026-11-15 적용함
  - `ExchangeContract.State.failure`와 `Failure(scope, message)`. `Scope`는 `CURRENCIES`와 `RATE`
  - `ExchangeViewModel`: `performLoadCurrencies`·`performFetchExchangeRate`의 실패가 상태로 간다.
    `performRefreshExchangeRates`와 `handleToggleFavorite`은 스낵바 그대로다. 성공하면 `failure`를 지운다
  - `ui/component/LoadFailureMessage.kt`: 문구·아이콘·다시 시도를 묶은 공통 표시
  - `ExchangeScreen`: `CURRENCIES`면 화면 전체, `RATE`면 환율 정보 자리
  - `FavoriteScreen`: 추측으로 실패 문구를 띄우던 `else` 앞에 실제 실패 분기를 뒀다
  - `SettingScreen`: 환율 일회성 이벤트 구독을 지웠다
  - `CalculatorContract.State.exchangeFailure`와 `Intent.RetryExchange`.
    `CalculatorViewModel`의 `performLoadCurrencies`·`performFetchExchangeRate` 실패가 상태로 간다.
    계산·저장·즐겨찾기 실패는 스낵바 그대로다
- iOS: `ios/Packages/Presentation/Sources/Presentation/Util/SideEffectBus.swift` — 아직 고치지 않았다

## 확인 방법

- Android
  - `ExchangeSideEffectScopeTest.kt` — 자동으로 돈 일의 실패가 side effect로 나가지 않고
    사용자가 누른 새로고침 실패만 나가는 것을 고정한다. 그 위에서 구독 공백 버퍼링과 fan-out도 본다
  - `ExchangeViewModelTest.kt` — "통화 목록 실패 상태가 되고 스낵바는 뜨지 않아야 한다",
    "환율 실패 상태가 되고 스낵바는 뜨지 않아야 한다"
- **에뮬레이터로 눈으로 확인함** (2026-11-15, Pixel_9_Pro, 비행기 모드로 재현)
  - 환율 화면: 통화 목록 실패 시 화면 전체가 아이콘·문구·`다시 시도`로 바뀐다. 스낵바는 뜨지 않는다
  - 설정 화면: 같은 실패 상황에서 스낵바가 뜨지 않는다. "환율 정보: 정보 없음"만 조용히 보인다
  - `다시 시도`: 네트워크를 되살리고 누르면 환율이 들어오고 실패 상태가 풀린다
  - 한국어 문구가 2줄로 적절히 끊긴다. 영어는 둘째 줄에 "again."만 남지만 읽기에 지장은 없다

- **확인하지 않은 것**
  - 즐겨찾기 화면의 실패 분기. 즐겨찾기가 하나도 없으면 "즐겨찾기 없음"이 먼저 걸려서,
    즐겨찾기 코드는 있는데 환율을 못 받은 상태를 에뮬레이터에서 만들지 못했다
  - 좁은 폭, 글자 크게 설정, 다크 모드에서의 모양
  - iOS는 손대지 않았다

- **계산기도 같은 기준으로 고쳤다** (2026-11-15)
  에뮬레이터 확인에서 계산기 탭에만 스낵바가 남아 있는 것을 보고 함께 적용했다.
  계산기의 실패 표시는 아직 눈으로 확인하지 않았다.
