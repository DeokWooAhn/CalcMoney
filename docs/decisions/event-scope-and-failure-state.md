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
- iOS — 2026-11-15 적용함
  - `ExchangeState.failure`와 `ExchangeFailure(scope:message:)`. `Scope`는 `currencies`와 `rate`
  - `ExchangeViewModel`: `performLoadCurrencies`·`performFetchExchangeRate` 실패가 상태로 간다.
    `performRefreshExchangeRates`와 `handleToggleFavorite`은 스낵바 그대로다
  - `Component/LoadFailureMessage.swift`: 문구·아이콘·다시 시도를 묶은 공통 표시
  - `ExchangeView`: `currencies`면 화면 전체, `rate`면 환율 정보 자리. 당겨서 새로고침은 그대로 둔다
  - `FavoriteView`: 추측으로 실패 문구를 띄우던 마지막 분기 앞에 실제 실패 분기를 뒀다
  - `SettingView`: 환율 일회성 이벤트 구독을 지웠다
  - `CalculatorState.exchangeFailure`와 `CalculatorIntent.retryExchange`
  - `SideEffectBus.swift`는 그대로다. 버스의 동작이 아니라 무엇을 버스에 보낼지를 바꿨다
  - 오류 문구 두 개에 줄바꿈을 넣었다(`Localizable.xcstrings`의 키도 함께 바뀜)

  iOS에는 Android에 없는 것이 둘 있어 그대로 살렸다. `ExchangeView`의 당겨서 새로고침(`.refreshable`)과
  `isLoading` 동안 도는 `ProgressView`다. 다만 그 `ProgressView`는 환율 화면에만 있어서,
  Android에서 본 "다시 시도를 눌러도 눌린 티가 안 난다"는 문제가 iOS에도 있었다(2026-11-17 확인).
  즐겨찾기 화면은 눌러도 아무 변화가 없었고, `send`가 Task만 띄우고 돌아오므로 연타하면 새로고침이 여러 번 나갔다.
  환율 화면도 오른쪽 위 표시만 돌 뿐 버튼은 켜져 있었다.

  2026-11-17 Android와 같게 `LoadFailureMessage`에 `isRetrying`을 더했다. 진행 중에는 버튼을 끄고
  글자 앞에 강조색 진행 표시를 둔다. 환율 화면 두 곳과 즐겨찾기 화면이 `ExchangeState.isLoading`을 넘긴다.
  즐겨찾기 화면은 새로고침 동안 `FavoriteRateInputSnapshot`이 바뀌지 않아 실패 분기가 끝까지 유지된다.
  `ExchangeViewModelRefreshTests`가 이것을 확인한다.
  계산기는 `CalculatorState`에 로딩 값이 없어 그대로다(Android도 같다).
  진행 표시는 버튼 밖에 둔다. 버튼 안에 두면 비활성 버튼의 회색을 따라가 `tint`가 먹지 않는다.
  이때 호출부의 `accessibilityIdentifier`(`favorite.failure` 등)가 안쪽 `failure.retry`를 덮어
  Maestro가 다시 시도 버튼을 찾지 못하던 것도 함께 고쳤다(`accessibilityElement(children: .contain)`).

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
    (2026-11-17 추가: `FavoriteScreenTest`가 상태를 주입해 이 분기를 확인한다. 눈으로 본 모양이 아니라 시맨틱 수준의 검사다)
  - 좁은 폭, 글자 크게 설정, 다크 모드에서의 모양
  - iOS 화면은 눈으로 확인하지 않았다. 시뮬레이터로 돌려 보지 않았다

- **스낵바를 상태로 바꾸면 요청 순서 검사가 더 중요해진다** (2026-11-15, CodeRabbit이 찾음)
  환율 조회의 성공 경로에는 "응답이 왔을 때 통화쌍이 이미 바뀌었으면 반영하지 않는다"는 검사가 있었지만
  실패 경로에는 없었다. 스낵바일 때는 지난 요청의 오류가 잠깐 떴다 사라질 뿐이었는데,
  상태로 바꾸자 성공한 화면 위에 실패 표시가 **남는** 문제가 됐다.
  두 플랫폼의 환율 조회 catch에 성공 경로와 같은 가드를 넣었다.
  계산기의 통화 목록 로드는 요청 번호가 없어, 목록을 이미 받았으면 늦은 실패를 반영하지 않는 것으로 갈음했다.

  2026-11-15 두 플랫폼에 이 시나리오를 고정하는 테스트를 넣었다.
  Android `ExchangeStaleResponseTest`, iOS `ExchangeViewModelStaleResponseTests`다.
  각각 가드를 지우면 실패하는 것까지 확인했고, 통화쌍이 그대로일 때는 실패가 보여야 한다는
  대조 시나리오를 함께 둬서 가드가 정상 실패까지 막지 않는지도 본다.
  iOS 계산기 쪽은 `CalculatorViewModelOrderingTests`의 4-2가 `exchangeFailure == nil`까지 본다.

  iOS 쪽은 처음에 가드를 지워도 통과했다. 지난 요청이 무시되면 상태가 바뀌지 않아
  "바뀌지 않음"을 기다릴 수 없어서다. 처음에는 `performFetchExchangeRate`의
  `defer { isLoading = false }`가 가드로 빠져나간 뒤에도 도는 것을 처리 완료 신호로 삼았는데,
  **그것 자체가 버그였다**(CodeRabbit이 찾음). 지난 응답이 그 사이 시작된 요청의 스피너까지 꺼 버린다.
  신호로 쓰면 버그를 정답으로 고정하게 된다.

  "일어나지 않아야 한다"는 상한 있는 대기로 직접 검사한다
  (`ObservationWait.swift`의 `waitUntil(_:within:)`). 관찰 추적 대신 폴링하는데,
  일어나지 않을 변화에는 깨울 알림이 없기 때문이다.

- **지난 응답이 현재 요청의 로딩 표시를 끄던 것을 고쳤다** (2026-11-15, iOS만)
  `performFetchExchangeRate`가 `defer { state.isLoading = false }`로 해제를 한 곳에 모았는데,
  가드로 빠져나간 지난 응답도 그 `defer`를 지난다. 그래서 통화를 바꿔 새 조회가 도는 중에
  지난 응답이 도착하면 스피너가 일찍 사라졌다. 초기 로드가 환율 조회를 품고 있어 중첩 호출도 같았다.

  진행 중인 요청을 세어 모두 끝났을 때만 끈다(`beginLoading`·`endLoading`).
  통화쌍만 보는 조건으로는 중첩 호출을 못 잡는다.

  **Android에는 이 문제가 없다.** 통화 목록을 받은 직후 로딩을 끄고 환율 조회가 다시 켜는 구조라
  중첩이 겹치지 않고, 가드로 `return`하면 `isLoading`을 건드리지 않는다. 고친 iOS가 Android와 같아졌다.

- **남은 빚**: `CalculatorViewModel.swift`(iOS)가 SwiftLint 한도인 400줄에 딱 닿아 있다.
  이번에 빈 줄을 줄여 맞췄다. 다음에 손댈 때는 환율 조회 흐름을 별도 파일로 빼는 편이 좋다.

- **계산기도 같은 기준으로 고쳤다** (2026-11-15)
  에뮬레이터 확인에서 계산기 탭에만 스낵바가 남아 있는 것을 보고 함께 적용했다.
  계산기 실패 표시도 눈으로 확인했다. 통화 선택 줄 아래에만 뜨고 숫자판은 그대로 쓸 수 있다.

- **다시 시도를 눌렀을 때 눌린 티가 나지 않는다** (2026-11-15 확인)
  실패한 상태에서 다시 눌러도 또 실패하면 화면이 그대로라, 사용자는 눌렸는지 알기 어렵다.
  logcat에는 누를 때마다 요청과 실패가 찍히므로 동작 자체는 한다.
  원인은 `ExchangeContract.State.isLoading`을 환율 화면이 읽지 않는 것이다(이 작업 전부터 그랬다).
  로딩 표시를 붙일지는 따로 정한다.

  실패한 재시도에 스낵바를 띄우지 않는 것은 의도다. 화면에 이미 같은 문구가 떠 있어 두 번 말하게 된다.
