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
- 인텐트가 겹치는 순서 테스트는 `CalculatorViewModelOrderingTest.kt`를 본다. orbit-test에는 함정이 둘 있다
  - 컨테이너가 `backgroundScope`에서 돌아 `advanceUntilIdle()`이 지연을 흘리지 않는다. `advanceTimeBy` + `runCurrent`로 흘린다
  - `expectNoItems()`는 남은 항목이 있으면 그 항목을 꺼낸 채 실패한다. 남은 항목을 모을 때 끝 확인에 쓰지 않는다

## Compose UI 테스트 (Robolectric)

- `presentation/src/test`에 JUnit4 + `RobolectricTestRunner` + `createComposeRule`로 작성한다. 기존 `./gradlew test`가 함께 돌린다.
- Kotest 규칙의 예외다. Robolectric이 JUnit5를 지원하지 않아 vintage 엔진으로 JUnit5 플랫폼에서 돌린다.
  vintage 버전은 Kotest가 쓰는 JUnit Platform 버전에 맞추고, Kotest를 올릴 때 함께 확인한다.
- 상태를 받는 `Screen`·컴포넌트를 대상으로 한다. `Route`·Hilt·ViewModel을 끌어오지 않고 Contract의 `State`를 직접 만든다.
- 노드는 화면에 이미 붙은 `testTag`(Maestro와 같은 `영역.이름` 규칙)로 먼저 찾는다. 문구는 리소스에서 읽고 문자열을 테스트에 박지 않는다.
- 테스트 이름은 백틱 안 한국어 문장으로 쓴다. JVM 테스트라 공백을 쓸 수 있다.
- 대조 시나리오를 함께 둔다. 한쪽 단언만으로는 늘 같은 값을 내는 구현도 통과한다.
- 첫 실행 때 Robolectric이 Android 런타임 jar(약 225MB)를 `~/.m2`에 받는다.
- 테스트를 더할 때는 아래 "실행"의 커버리지 확인 절차에 따라 전후 수치와 새로 집계된 클래스를 확인한다.
- 레퍼런스:
  - `presentation/src/test/java/com/ahn/presentation/ui/component/LoadFailureMessageTest.kt`
  - `presentation/src/test/java/com/ahn/presentation/ui/screen/favorite/FavoriteScreenTest.kt`

## 실행

```bash
./gradlew test                        # 전체
./gradlew :domain:test                # 모듈별
./gradlew :koverHtmlReportUnit        # 병합 커버리지 (build/reports/kover/htmlUnit/index.html)
./gradlew :koverVerifyUnit            # CI 커버리지 게이트
```

- 커버리지는 Kover의 루트 `unit` variant로 본다. Hilt·Room·Compose 생성 코드, `data/di`, Room entity, `MainActivity`·광고·테마, `@Composable` 함수는 제외된다. 화면 흐름은 Maestro가 맡는다.
- `@Composable` 제외는 컴포저블 람다 본문을 빼지 못한다. Compose UI 테스트가 새 파일을 로드하면 그 파일의 람다 줄이 집계에 들어와 수치가 떨어진다. 그래서 UI 전용 파일 클래스는 `presentation/build.gradle.kts`의 `kover.currentProject.instrumentation.excludedClasses`에 둔다(화면은 `*ScreenKt*` 패턴, 컴포넌트는 파일별).
- 루트 리포트 필터(`reports.filters.excludes.classes`)로는 UI 전용 파일을 빼지 않는다. 로드된 클래스에 듣지 않기 때문이다.
- Compose UI 테스트를 더할 때는 전후로 `./gradlew :koverLogUnit`을 비교한다. 수치가 떨어지면 `./gradlew :koverXmlReportUnit` 리포트에서 새로 나타난 클래스를 찾고, UI 전용 파일이면 계측 제외(`instrumentation.excludedClasses`)에 더한다. 패턴 끝에 `*`를 붙여 중첩 클래스까지 덮는다. 일반 로직이 섞인 파일은 빼지 않는다.
- `*ScreenKt*` 패턴 때문에 `*Screen.kt` 파일의 코드는 커버리지에 잡히지 않는다. 유닛 테스트할 일반 로직은 화면 파일이 아니라 별도 파일(reducer·util 등)에 둔다.
- 하한은 루트 `build.gradle.kts`의 `minBound`. 테스트를 추가해 커버리지가 오르면 하한도 2%p 남짓 아래로 따라 올린다. 내리지 말 것.
- 새로 생긴 생성 클래스가 리포트에 보이면 루트 필터에 패턴을 추가한다. 패턴은 패키지를 포함한 전체 이름과 비교되므로 `Hilt_*`가 아니라 `*.Hilt_*`처럼 쓴다.

- JUnit5 플랫폼 필수 — 각 모듈 gradle에 이미 `useJUnitPlatform()` 설정돼 있음. 새 모듈 추가 시 빼먹지 말 것.
- DataStore 테스트 헬퍼: `data/src/test/java/com/ahn/data/common/datastore/TestPreferenceDataStore.kt`
- 계측 테스트(`src/androidTest`)는 두지 않는다. CI가 돌리지 않아 방치되던 DAO·Compose 테스트를 지웠다. Compose UI 테스트는 위 "Compose UI 테스트 (Robolectric)"에 적은 대로 `src/test`에 둔다. 화면 흐름은 Maestro가 맡고, Room DB는 환율 캐시뿐이라 마이그레이션이 빠져도 `DatabaseModule`의 `fallbackToDestructiveMigration`이 캐시를 비우고 다시 받는다.
