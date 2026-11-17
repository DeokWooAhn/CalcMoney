# Compose UI 테스트는 Robolectric으로 src/test에 두고 계측 테스트는 두지 않는다

- 정한 날: 2026-11-17
- 근거: 2026-11-17 대화, 도입 전 시험 실행(아래 "이유"),
  `.claude/skills/testing/SKILL.md`, `presentation/build.gradle.kts`, 아래 관련 코드
- 적용: Android `presentation` 모듈의 Compose 컴포넌트·화면 테스트와 Android CI

## 규칙

- Compose UI 테스트는 Robolectric 위에서 `createComposeRule`을 쓰고 `presentation/src/test`에 둔다.
  기존 `./gradlew test`와 CI가 함께 돌린다. 계측 테스트(`src/androidTest`)는 두지 않는다.
- 상태를 받는 `Screen`·컴포넌트에 Contract의 상태를 직접 주입한다. `Route`·Hilt·ViewModel은 끌어오지 않는다.
- Kover의 `@Composable` 함수 제외는 유지하고, Compose UI 테스트가 로드하는 UI 전용 파일 클래스는 `presentation`의 Kover 계측(`instrumentation.excludedClasses`)에서 뺀다.
- 스크린샷 테스트(Roborazzi)는 다음 단계로 미룬다.

## 이유

2026-11-17 의존성을 붙여 직접 돌려 본 결과와 그때 확인한 버전이다.

- 기존 테스트는 Kotest 6.1.7이며 `useJUnitPlatform()`을 쓴다. Kotest의 JUnit Platform은 1.13.4다.
  Robolectric 4.18은 JUnit5를 지원하지 않아 JUnit4 테스트를 vintage 5.13.4로 같은 플랫폼에서 돌린다.
  vintage 6.x는 플랫폼 버전이 어긋나므로 쓰지 않는다.
- 도입 전 시험에서 이 의존성 구성으로 Robolectric JUnit4 테스트 1개와 기존 Kotest 153개,
  합계 154개가 `:presentation:testDebugUnitTest`에서 성공했다. 첫 Robolectric 테스트는 기동을 포함해 약 11초였다.
- Compose BOM이 고른 `ui-test-junit4`는 1.9.0, JUnit4는 4.13.2였다.
  `ui-test-manifest`를 빼면 실패했고 `testImplementation`으로 넣으면 성공했다.
  테스트용 `ComponentActivity`는 필요하지만 앱 디버그 APK에 넣을 필요는 없다.
- SDK 36의 `android-all-instrumented/16-robolectric-13921718-2-i7` 런타임 jar 약 225MB를
  첫 실행 때 `~/.m2`에 받았다. CI에서는 `~/.m2/repository/org/robolectric`을 캐시한다.
- `LoadFailureMessage`는 재시도 중 버튼 비활성·진행 표시를 제공한다.
  `FavoriteScreen`은 상태를 받는 공개 함수이고 `canRequestAds = false`로 광고 요청 없이 그릴 수 있다.
  따라서 기기 없이 실패 분기와 재시도 동작을 시맨틱 수준에서 검사할 수 있다.
- 기본 문자열은 영어이고 한국어는 `values-ko`에 있다. 테스트는 문자열을 박지 않고 리소스에서 읽는다.
- 2026-11-17 `./gradlew :koverLogUnit --console=plain`의 줄 커버리지와
  `./gradlew :koverXmlReportUnit --console=plain`의 클래스별 리포트를 비교했다.
  아래 줄 수는 미실행·실행 순서다.

  | 설정 | 줄 커버리지 | master 리포트와 클래스별 비교 |
  | --- | ---: | --- |
  | master(`942738a`) | 79.4731% | 기준 |
  | Compose UI 테스트 추가, UI 전용 파일 제외 없음 | 76.8949% | `FavoriteScreenKt` 50·8, `AdMobBannerKt` 18·3, `LoadFailureMessageKt` 0·11, `CustomSnackbarKt` 1·0이 새로 집계됐다 |
  | 루트 리포트 필터에 UI 전용 파일 클래스 이름 제외 | 73.584% | `FavoriteScreenKt` 130·85, `AdMobBannerKt` 31·12가 남고 컴포저블 함수 본체까지 집계됐다. 로드되지 않은 `CalculatorScreenKt` 등은 빠졌지만 `CalculatorScreenKt$CalculatorHistoryPanel$2$1` 등 중첩 클래스 12개가 새로 나타났다 |
  | `presentation`의 Kover 계측에서 UI 전용 파일 제외, 루트는 master 설정 유지 | 79.528% | master와 같고 `UiText$StringResource`의 실행 줄만 1줄 늘었다 |

  제외하지 않은 설정에서 새로 집계된 줄은 컴포저블 람다 본문이었다. 바이트코드의
  `FavoriteScreen$lambda$1`·`$3` 등 람다 메서드에 `@Composable`이 붙어 있는데도
  Kover의 `annotatedBy`가 제외하지 못했다. 루트 리포트 필터 설정으로 테스트를 `--rerun`으로 두 번 다시 돌려도
  값은 같았고, 루트 설정만 원래대로 돌리면 76.8949%로 돌아왔다.
  계측에서 빼면 그 클래스에 실행 기록이 생기지 않아 Kover가 master에서처럼 로드되지 않은 클래스로 다룬다.

## 고르지 않은 것

- 계측 테스트: 에뮬레이터가 필요하고 CI가 돌리지 않아 방치된 전례가 있다. 화면 흐름은 Maestro와 겹친다.
- Paparazzi: 2026-11-17 기준 2.0은 `2.0.0-alpha05.1`이고 AGP 9 문제 이력이 있다.
  상호작용을 테스트할 수 없다.
- Google Compose Preview Screenshot Testing: 2026-11-17 기준 `0.0.1-alpha16`이며 상호작용을 테스트할 수 없다.
- Kover를 그대로 두고 수치가 떨어지는 것을 받아들이기: 제외 전 76.8949%는 하한 76%까지 약 0.9%p만 남아 다음 UI 테스트가 CI 게이트에 걸릴 수 있다.
- 루트 리포트 필터로 클래스째 제외: 로드된 클래스에 듣지 않고 수치가 73.584%로 더 떨어졌다.
- Robolectric 테스트를 Kover 계측이 없는 별도 Gradle Test 태스크로 분리: AGP가 단위 테스트 태스크에 거는 설정을 손으로 옮겨야 해 깨지기 쉽다.

## 확인하지 않은 것

- Kover(0.9.11)가 로드된 클래스에 리포트 필터를 적용하지 못하는 원인.
- 즐겨찾기 재시도 중 표시: `FavoriteScreen.kt`의 `FavoriteLoadFailure`는 `LoadFailureMessage`에
  `isRetrying`을 넘기지 않아 다시 시도를 누른 뒤에도 버튼이 켜져 있고 진행 표시가 없다.
  환율 화면은 `isRetrying = state.isLoading`을 넘긴다. `FavoriteScreenTest`는 재시도 중
  (`exchangeState.isLoading = true`) 상태를 다루지 않는다. 고칠지는 별도 작업에서 정한다.
- 픽셀 모양·색: 이번 테스트는 시맨틱만 본다. 비활성 버튼과 진행 표시가 눈에 어떻게 보이는지는 확인하지 않는다.
- 스낵바가 어느 화면에 뜨는지: 탭 이동이 필요해 이번 범위 밖이다.
- CI 소요 시간 변화와 캐시 적중: PR의 CI 로그에서 확인해야 한다.

## 코드

- `presentation/src/test/java/com/ahn/presentation/ui/component/LoadFailureMessageTest.kt`
- `presentation/src/test/java/com/ahn/presentation/ui/screen/favorite/FavoriteScreenTest.kt`
- `presentation/build.gradle.kts`, `gradle/libs.versions.toml`, `.github/workflows/android-ci.yml`
