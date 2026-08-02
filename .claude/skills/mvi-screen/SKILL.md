---
name: mvi-screen
description: 새 화면 추가, ViewModel/Contract 작성, Orbit MVI 패턴을 따를 때 사용. UseCase·Repository·DI 배선 규칙 포함.
---

# 화면 / MVI / 레이어 배선 규칙

설명보다 아래 레퍼런스 파일을 먼저 읽고 그 구조를 그대로 따라할 것.

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
- 예외: `MainViewModel`은 Orbit이 아닌 plain StateFlow — 새 화면에서 따라하지 말 것

## UseCase 규칙 (`domain/<feature>/usecase/`)

- 단일 책임 + `operator fun invoke`
- ViewModel에는 개별 주입하지 않고 **홀더 클래스로 묶어서 하나만 주입**: `ExchangeUseCases.kt`, `FavoriteUseCases.kt` 참조. 호출은 `exchangeUseCases.getExchangeRate(from, to)` 형태
- 도메인 에러는 sealed 계층: `domain/.../exchange/model/ExchangeRateException.kt` → UI 문자열 변환은 `presentation/.../util/ExchangeRateErrorMapper.kt`

## Data 레이어 (`data/<feature>/`)

- 구조: `local/{dao,entity,datasource}` + `remote/datasource` + `mapper/` + `repository/`
- 정석 레퍼런스: `data/src/main/java/com/ahn/data/exchange/repository/ExchangeRateRepositoryImpl.kt` (Mutex 가드, 12h TTL, 원격 실패 시 stale 캐시 폴백)
- 테스트 용이성 관례: `internal constructor(..., clock: Clock)` + `@Inject` 보조 생성자가 `Clock.systemDefaultZone()` 공급
- Room 스키마 변경 시: `CalcMoneyDatabase.kt` 버전 올리고 `MIGRATION_N_M` 추가, `data/schemas/` JSON 커밋

## DI 배선 (`data/src/main/java/com/ahn/data/di/`)

- 새 Repository → `RepositoryModule.kt`에 `@Binds` 추가
- 새 DataStore → `DataStoreQualifiers.kt`에 Qualifier 추가 후 `DataStoreModule.kt`에 등록

## 광고

- 배너 ID는 buildType별 `resValue` — debug는 구글 테스트 ID 하드코딩, release는 env(`ADMOB_*_BANNER_ID`). 새 배너 추가 시 `presentation/build.gradle.kts`와 CI 워크플로 env 둘 다 수정 필요.
