---
name: testing
description: 유닛 테스트·ViewModel 테스트·DAO 테스트를 작성하거나 수정할 때 사용. Kotest BehaviorSpec 한국어 컨벤션과 MockK 규칙.
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
./gradlew :data:connectedDebugAndroidTest   # DAO 등 instrumented (에뮬레이터 필요)
```

- JUnit5 플랫폼 필수 — 각 모듈 gradle에 이미 `useJUnitPlatform()` 설정돼 있음. 새 모듈 추가 시 빼먹지 말 것.
- DataStore 테스트 헬퍼: `data/src/test/java/com/ahn/data/common/datastore/TestPreferenceDataStore.kt`
- `:data` mapper/datasource 일부는 레거시 JUnit4 `@Test` 스타일 — 새 테스트는 BehaviorSpec으로 통일
