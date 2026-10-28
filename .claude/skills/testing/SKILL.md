---
name: testing
description: 유닛 테스트·ViewModel 테스트를 작성하거나 수정할 때 사용. Kotest BehaviorSpec 한국어 컨벤션과 MockK 규칙.
---

# 테스트 작성 규칙

새 테스트는 아래 정석 레퍼런스의 구조를 복사해서 시작할 것.

## 기본 스타일: Kotest BehaviorSpec + 한국어 Given/When/Then

```kotlin
Given("선택한 통화가 즐겨찾기에 없는 상태에서") {
    When("즐겨찾기를 토글하면") {
        Then("해당 통화를 즐겨찾기에 추가해야 한다") { ... }
    }
}
```

- 정석 레퍼런스: `domain/src/test/java/com/ahn/domain/favorite/usecase/ToggleFavoriteCurrencyUseCaseTest.kt`
- 모든 BehaviorSpec 최상단에 `isolationMode = IsolationMode.InstancePerRoot`
- `beforeEach { clearMocks(...) }` 또는 `clearAllMocks()`
- 모킹은 **MockK만** (`mockk<T>()`, `coEvery`, `coVerify(exactly = n)`) — Mockito 금지
- Turbine은 카탈로그에 있지만 실제 사용처 0 — 새로 쓰지 말 것

## ViewModel 테스트

- 정석 레퍼런스: `presentation/src/test/java/com/ahn/presentation/ui/screen/favorite/FavoriteViewModelTest.kt`
- `StandardTestDispatcher` + `Dispatchers.setMain/resetMain`을 `beforeEach`/`afterEach`에서
- `runTest(testDispatcher)` + `advanceUntilIdle()`
- UseCase 홀더는 mock하지 말고 **실제로 생성하되 멤버 UseCase만 mock** (`createViewModel()` 헬퍼 패턴)
- Orbit 검증에는 `orbit-test` 사용 (`ExchangeViewModelTest.kt` 참조)

## 실행

```bash
./gradlew test                        # 전체
./gradlew :domain:test                # 모듈별
./gradlew :koverHtmlReportUnit        # 병합 커버리지 (build/reports/kover/htmlUnit/index.html)
./gradlew :koverVerifyUnit            # CI 커버리지 게이트
```

- 커버리지는 Kover의 루트 `unit` variant로 본다. Hilt·Room·Compose 생성 코드, `data/di`, Room entity, `MainActivity`·광고·테마, `@Composable` 함수는 제외된다 — 화면은 Maestro가 맡는다.
- 하한은 루트 `build.gradle.kts`의 `minBound`. 테스트를 추가해 커버리지가 오르면 하한도 2%p 남짓 아래로 따라 올린다. 내리지 말 것.
- 새로 생긴 생성 클래스가 리포트에 보이면 루트 필터에 패턴을 추가한다. 패턴은 패키지를 포함한 전체 이름과 비교되므로 `Hilt_*`가 아니라 `*.Hilt_*`처럼 쓴다.

- JUnit5 플랫폼 필수 — 각 모듈 gradle에 이미 `useJUnitPlatform()` 설정돼 있음. 새 모듈 추가 시 빼먹지 말 것.
- DataStore 테스트 헬퍼: `data/src/test/java/com/ahn/data/common/datastore/TestPreferenceDataStore.kt`
- 계측 테스트(`src/androidTest`)는 두지 않는다. CI가 돌리지 않아 방치되던 DAO·Compose 테스트를 지웠다. 화면은 Maestro가 맡고, Room DB는 환율 캐시뿐이라 마이그레이션이 빠져도 `DatabaseModule`의 `fallbackToDestructiveMigration`이 캐시를 비우고 다시 받는다.
