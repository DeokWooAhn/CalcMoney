---
name: mvi-screen
description: 새 화면 추가, ViewModel/Contract 작성, Orbit MVI 패턴을 따를 때 사용. UseCase·Repository·DI 배선과 Compose 작성 규칙(문자열·접근성·Preview·side-effect) 포함.
---

# 화면 / MVI / 레이어 배선 규칙

설명보다 아래 레퍼런스 파일을 먼저 읽고 그 구조를 그대로 따라할 것.

ViewModel·Contract·UseCase·Repository의 모양은 `:konsist` 테스트(`konsist/src/test/java/com/ahn/konsist/ArchitectureTest.kt`)가
검사한다(`./gradlew :konsist:test`). 규칙을 바꾸거나 예외를 더할 때는 그 파일의 예외 목록에 이유를 적고 이 문서도 함께 고친다.
예외가 규칙을 지키게 되면 목록에서 빼라고 테스트가 실패한다. Konsist는 0.18.1로 고정했다(1.0.0 정식이 나오면 올린다).

## 화면 하나 = 파일 3개 (`presentation/ui/screen/<feature>/`)

| 파일 | 내용 | 정석 레퍼런스 |
|---|---|---|
| `<Feature>Contract.kt` | `data class State`, `sealed interface Intent`, `sealed interface SideEffect` | `presentation/src/main/java/com/ahn/presentation/ui/screen/exchange/ExchangeContract.kt` |
| `<Feature>ViewModel.kt` | `ContainerHost<State, SideEffect>` | `presentation/src/main/java/com/ahn/presentation/ui/screen/exchange/ExchangeViewModel.kt` |
| `<Feature>Screen.kt` | `<Feature>Route()` 컴포저블 노출 | 같은 디렉토리의 `ExchangeScreen.kt` |

## ViewModel 내부 구조 (ExchangeViewModel이 정석)

- 진입점은 단일 `fun processIntent(intent: ...)` — when으로 private `handleX()`에 분배
- `handleX()`가 `intent { }` 블록을 열고, 실제 로직은 private `suspend fun Syntax<State, SideEffect>.performX()` 헬퍼로
- 순수 상태 계산이 복잡하면 별도 plain class로 추출 (예: `CalculatorExpressionReducer.kt`)
- **Orbit은 인텐트를 직렬화하지 않는다.** 받은 순서대로 시작만 하고, 한 인텐트가 `await`에서 멈추면 다음 인텐트가 끼어든다.
  저장·조회를 기다리는 인텐트가 겹치는 화면은 `CalculatorViewModel`처럼 일을 셋으로 나눈다(`docs/decisions/calculator-intent-ordering.md`).
  - 확정: 결과는 첫 `await` 전에 `reduce`로 반영한다. `reduce` 안에서는 순수 계산만 한다. `StateFlow.update`라 다시 실행될 수 있다
  - 저장: 로컬 저장은 `Mutex` 안에서. 상태를 확정한 뒤 다른 `await` 없이 바로 잠금을 요청해야 탭 순서가 지켜진다
  - 조회: 기다리지 않고 따로 돌리고, 요청 번호·취소·대상(통화쌍)을 확인한 응답만 반영한다
- 예외: `MainViewModel`과 `FavoriteViewModel`은 Orbit이 아닌 plain StateFlow — 새 화면에서 따라하지 말 것
  - `MainViewModel`: Activity 범위에서 테마·인앱 리뷰·전면 광고·크래시 단서를 다룬다. 화면의 Intent·SideEffect가 없다
  - `FavoriteViewModel`: 공유 `ExchangeViewModel`의 상태를 받아 즐겨찾기 환율 목록만 계산하는 보조 ViewModel이다.
    자체 Intent·SideEffect가 없어 `FavoriteContract`도 `State`만 둔다

## Compose 작성 규칙 (Screen 파일)

- 사용자에게 보이는 문자열은 `res/values/strings.xml`에 정의하고 `values-ko`를 함께 채운다.
  Composable이나 ViewModel에 표시 문자열을 직접 쓰지 않는다.
- 상호작용 요소는 최소 48dp 터치 영역을 확보한다. 아이콘·이미지에는 `contentDescription`을 주고
  장식용이면 `null`을 명시한다.
- `Screen` 컴포저블과 재사용 컴포넌트에는 `@Preview`를 최소 하나 둔다. ViewModel을 배선하는
  `Route`에는 두지 않는다.
- Composable 본문에서 상태를 바꾸거나 비즈니스 로직을 실행하지 않는다. `LaunchedEffect`,
  `DisposableEffect` 같은 side-effect API를 쓴다.
- `Context`를 ViewModel이나 깊은 컴포저블 계층으로 넘기지 않는다. 필요하면 그 자리에서
  `LocalContext.current`로 얻는다.

## UseCase 규칙 (`domain/<feature>/usecase/`)

- 단일 책임 + `operator fun invoke`. 예외: `ConvertExchangeAmountUseCase`(계산식 환산·단일 금액 환산 두 공개 함수)
- ViewModel에는 개별 주입하지 않고 **홀더 클래스로 묶어서 하나만 주입**: `ExchangeUseCases.kt`, `FavoriteUseCases.kt` 참조. 호출은 `exchangeUseCases.getExchangeRate(from, to)` 형태
- 도메인 에러는 sealed 계층: `domain/.../exchange/model/ExchangeRateException.kt` → UI 문자열 변환은 `presentation/.../util/ExchangeRateErrorMapper.kt`

## Data 레이어 (`data/<feature>/`)

- 구조: `local/{dao,entity,datasource}` + `remote/datasource` + `mapper/` + `repository/`
- 정석 레퍼런스: `data/src/main/java/com/ahn/data/exchange/repository/ExchangeRateRepositoryImpl.kt` (Mutex 가드, 12h TTL, 원격 실패 시 stale 캐시 폴백)
- 테스트 용이성 관례: `internal constructor(..., clock: Clock)` + `@Inject` 보조 생성자가 `Clock.systemDefaultZone()` 공급
- Room 스키마 변경 시: `CalcMoneyDatabase.kt` 버전 올리고 `MIGRATION_N_M` 추가, `data/schemas/` JSON 커밋. 마이그레이션이 빠져도 크래시 대신 캐시를 비우지만(`fallbackToDestructiveMigration`), 업데이트 직후 오프라인 사용자는 환율을 못 보니 마이그레이션은 계속 쓴다. 사용자 데이터를 이 DB에 넣게 되면 그 대체 동작부터 다시 검토

## DI 배선 (`data/src/main/java/com/ahn/data/di/`)

- 새 Repository → `RepositoryModule.kt`에 `@Binds` 추가
- 새 DataStore → `DataStoreQualifiers.kt`에 Qualifier 추가 후 `DataStoreModule.kt`에 등록

## 광고

- 배너 ID는 buildType별 `resValue` — debug는 구글 테스트 ID 하드코딩, release는 env(`ADMOB_*_BANNER_ID`). 새 배너 추가 시 `presentation/build.gradle.kts`와 CI 워크플로 env 둘 다 수정 필요.
