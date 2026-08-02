# CalcMoney 작업 지침

## 작업 방식

- 기본적으로 질문·검토·설명 모드로 동작한다.
- 사용자가 "수정해줘", "만들어줘", "배포해줘"처럼 직접 변경을 요청한 경우에만 파일·Git·외부 시스템을 변경한다.
- 관련 없는 사용자 변경은 보존하고, 커밋·푸시 범위에 포함하지 않는다.
- 주석, KDoc, 테스트 설명, 커밋 메시지는 한국어로 작성한다. 코드 식별자와 로그 메시지는 영어로 작성한다.

## 프로젝트 구조

한국수출입은행 환율을 보여주는 Android 앱이다. Cloud Functions가 Firestore의
`exchangeRates/latest` 문서를 갱신하고, 앱은 이를 Room에 12시간 TTL로 캐시한다.
앱은 환율 API를 직접 호출하지 않는다.

```
:app          -> :data, :domain, :presentation  # Hilt 조립 전용 셸
:data         -> :domain                         # Room, Firestore, DataStore, Repository 구현
:presentation -> :domain                         # Compose(Material3), Orbit MVI
:domain                                       # 순수 Kotlin: 모델, Repository 인터페이스, UseCase
```

- 화면은 Calculator, Exchange, Favorite, Settings 네 개다.
- `MainActivity`는 `:presentation` 모듈에 있다.
- Settings와 Favorite는 activity-scoped `ExchangeViewModel`을 공유한다. `MainNavGraph.kt`의
  `hiltViewModel(sharedOwner)`를 바꾸지 않는다.

## 화면·MVI·데이터 작업

- 새 화면은 `presentation/ui/screen/<feature>/`에 `Contract`, `ViewModel`, `Screen` 세 파일로 구성한다.
  `ExchangeContract.kt`, `ExchangeViewModel.kt`, `ExchangeScreen.kt`을 기준으로 삼는다.
- ViewModel은 단일 `processIntent()` 진입점에서 intent를 private handler로 분배한다.
  Orbit `intent {}`의 실제 로직은 `suspend fun Syntax<State, SideEffect>.performX()` 형태의 private helper에 둔다.
- UseCase는 단일 책임의 `operator fun invoke`를 사용한다. ViewModel에는 개별 UseCase 대신
  `ExchangeUseCases`, `FavoriteUseCases` 같은 홀더를 하나만 주입한다.
- 도메인 오류는 sealed 계층으로 표현하고 UI 문자열 변환은 presentation 레이어에서 한다.
- Repository 구현은 `data/<feature>/local`, `remote`, `mapper`, `repository` 구조를 따른다.
- Room 스키마를 바꾸면 DB 버전과 migration을 추가하고 `data/schemas/` JSON도 커밋한다.
- 새 Repository·DataStore는 `data/di/`의 Module과 Qualifier까지 함께 배선한다.
- 새 AdMob 배너를 추가하면 `presentation/build.gradle.kts`의 buildType별 리소스와 CI 환경 변수를 함께 수정한다.

## 테스트와 검증

- 새 테스트는 Kotest `BehaviorSpec`과 한국어 Given/When/Then 설명을 사용한다.
  `isolationMode = IsolationMode.InstancePerRoot`와 MockK를 사용하며 Mockito는 사용하지 않는다.
- ViewModel 테스트는 `StandardTestDispatcher`, `Dispatchers.setMain/resetMain`, `runTest`,
  `advanceUntilIdle`, `orbit-test` 패턴을 따른다.
- 필요한 범위의 검증을 실행한다.

```bash
./gradlew :domain:test
./gradlew :presentation:detekt
./gradlew detekt test :app:assembleDebug
```

- `./gradlew build`와 release/bundle 계열 태스크는 로컬 시크릿 검증 때문에 실패할 수 있다.
  일반 검증에는 `assembleDebug`, `test`, `detekt`를 사용한다.

## 릴리스·배포

- 기본 브랜치는 `master`다.
- `versionCode`는 수동으로 변경하지 않는다. `v*` 태그 푸시 시 CI가
  `10000 + GITHUB_RUN_NUMBER`로 정하고, 태그의 `v`를 뺀 값이 `versionName`이 된다.
- 앱 릴리스는 `master` 병합 후 `v*` 태그를 푸시하면 signed AAB가 Google Play internal 트랙에 업로드된다.
- Firebase App Distribution은 workflow dispatch로만 실행한다.
- `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경은 `master` 푸시 후 Firebase 배포를 유발한다.
  Firestore는 `exchangeRates/latest`만 공개 읽기를 허용하고 앱 쓰기는 허용하지 않는다.

상세 배포 절차와 시크릿 목록은 `docs/continuous-deployment.md`를 따른다.

## 상세 참고 문서

- 화면/MVI/레이어 배선: `.claude/skills/mvi-screen/SKILL.md`
- 테스트: `.claude/skills/testing/SKILL.md`
- 릴리스·CI·Firebase 배포: `.claude/skills/release-deploy/SKILL.md`
- 프로젝트 개요: `CLAUDE.md`
