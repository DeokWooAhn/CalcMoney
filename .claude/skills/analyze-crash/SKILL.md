---
name: analyze-crash
description: Firebase Crashlytics 크래시를 분석하고 수정 PR까지 이어 갈 때 사용한다. "이 크래시 봐줘", "Crashlytics에 이런 게 떴어", 스택 트레이스·Crashlytics 이슈를 붙여 넣은 경우가 해당된다. 이 앱의 크래시 단서 형식, 유형별로 먼저 볼 코드, 재현·판정·수정 원칙을 담고 있다.
---

# 크래시 분석 (CalcMoney)

크래시 하나를 볼 때마다 같은 순서로 확인하고, 확인한 근거를 결과에 남긴다. 결론만 내지 않는다.
결과를 받은 사람이 "무엇을 보고 그렇게 판단했는지" 따라갈 수 있어야 검증할 수 있다.

## 1. 먼저 모을 것

사용자에게 없으면 요청한다. Crashlytics 콘솔의 이슈 화면에 모두 있다.

| 항목 | 어디서 | 왜 |
|---|---|---|
| 예외·메시지·스택 | 이슈의 스택 트레이스 | 무엇이 어디서 터졌나 |
| 앱 버전 | versionName·versionCode(Android), 버전·빌드(iOS) | 재현할 커밋을 정한다. Android는 `android-v<versionName>` 태그 |
| 기기·OS | 이벤트 상세 | OS별 동작 차이, 특정 기기 문제 |
| 발생 건수·영향 사용자 | 이슈 요약 | 우선순위 |
| 키 `screen` | 이벤트의 Keys 탭 | 크래시 순간 보던 탭 |
| 로그 | 이벤트의 Logs 탭 | 크래시 직전 행동 (아래 표) |

이 앱이 남기는 단서(`CrashBreadcrumbs`, Android domain·iOS Domain)는 아래가 전부다. 계산식·금액 같은 입력값은 없다.

| 로그 | 뜻 |
|---|---|
| `screen: <탭>` | 하단 탭 이동. Android `calculator`·`exchange`·`favorite`·`settings`, iOS는 설정이 `setting` |
| `rates: refreshed N from remote` | 캐시가 만료돼 Firestore에서 환율 N개를 받음 |
| `rates: remote failed (원인), using N cached` / `no cache` | 원격 실패. 저장된 환율로 넘어갔는지, 저장된 것도 없었는지 |
| `rates: manual refresh N` / `manual refresh failed (원인)` | 환율 화면의 새로고침 |
| `ads: canRequestAds=true/false` | 광고 동의 결과로 광고를 요청할 수 있는지(바뀔 때만) |
| `ads: interstitial shown` | 계산기를 떠날 때 전면 광고가 떴음 |
| `review: in-app review requested` | 시스템 인앱 리뷰 창을 요청함 |

원인은 Android에서는 앱 예외의 고정 메시지나 클래스 전체 이름이고, iOS에서는 `ExchangeRateError`의 케이스 이름이나 NSError의 domain과 code다.

## 2. 유형을 정하고, 유형마다 먼저 볼 곳

| 유형 | 이런 예외일 때 | 먼저 볼 곳 |
|---|---|---|
| T1 null·타입·파싱 | NPE, `!!`, `lateinit`, iOS 강제 언래핑, `NumberFormatException` | null이 어디서 왔나. 계산식 파서(`CalculateExpressionUseCase`, E 표기·0으로 나누기는 #85), 서버 문서 필드(`ExchangeRateRemoteDataSource.requireFetchedAt`), 통화 매핑(`ExchangeRateMapper`·`CurrencyInfoMapper`) |
| T2 생명주기(Android) | `IllegalStateException`(onSaveInstanceState 뒤, 해제된 launcher) | 작업을 띄운 트리거와 그때 화면 상태. side effect·Flow 수집 범위(`collectAsStateWithLifecycle`, `repeatOnLifecycle` 단계), 결과가 화면을 떠난 뒤 도착하는지. 광고 표시는 `MainActivity.showInterstitialAdIfDue`처럼 RESUMED를 다시 확인하는지 |
| T3 동시성·격리 | iOS `EXC_BREAKPOINT`/SIGTRAP, `CalledFromWrongThreadException`, 경쟁 상태 | iOS Presentation은 기본 격리가 MainActor다. UIKit이 백그라운드에서 부르는 클로저(동적 색상 등)는 `nonisolated`여야 한다(`1b268fe`). actor 재진입(`ExchangeRateRepositoryImpl.inFlightRefresh`), Android `refreshMutex` |
| T4 ANR·메인 스레드 차단 | ANR, iOS watchdog 종료(0x8badf00d) | 메인 스레드를 막은 작업. `MobileAds.initialize`는 IO에서, Room·DataStore·SwiftData를 메인에서 동기로 부르지 않는지 |
| T5 외부 SDK | 스택 상단이 AdMob·UMP·Firebase·androidx 내부 | SDK 버전과 알려진 이슈. 버전은 `app/dependencies/releaseRuntimeClasspath.txt`, iOS `ios/**/Package.resolved`. 광고는 `InterstitialAdController`·`InterstitialAdPresenter`, 동의는 `AdConsentManager` |
| T6 저장·마이그레이션 | Room `IllegalStateException`(migration), SwiftData 오류 | Room 스키마 `data/schemas/`와 `DatabaseModule`의 `fallbackToDestructiveMigration`(#93), DataStore 4개, iOS `ExchangeRateLocalDataSource.make`(실패하면 메모리 저장소, 그마저 실패하면 `fatalError`) |
| T7 비동기 uncaught | 스택에 앱 프레임이 하나도 없음(콜백 스레드, `Tasks.await`) | 스택 위치를 믿지 말고 이 예외를 받은 호출부를 역추적한다. Firestore `get().await()`, `CancellationException` 처리, iOS `Task` 안에서 던진 오류 |

## 3. 확인 순서

1. **시작점:** 크래시를 일으킨 작업을 누가 시작했나(intent, UseCase 호출, SDK 콜백).
2. **결과 전달 경로:** 결과가 어떻게 화면이나 상태에 도착하나(Flow, side effect, 콜백).
3. **조건:** 그때 생명주기, 스레드·격리, 동의·광고 상태는 어땠나. `screen` 키와 로그로 시각을 맞춰 본다.
4. **재현:** 크래시가 난 버전에서 같은 조건으로 재현한다.
   - Android는 `git switch --detach android-v<versionName>`. iOS는 그 빌드의 커밋에서.
   - 단위 테스트(Kotest, Swift Testing)로 재현하는 게 가장 좋다. 화면 흐름이면 실기기·시뮬레이터와 Maestro로.
   - **재현된 경로만 원인으로 채택한다.** 재현하지 못한 가설은 "미검증"으로 적는다. AI가 찾은 원인은 재현 전까지 추측이다.

## 4. 결과 형식

답변과 PR 설명에 같은 순서로 남긴다.

1. **한 줄 요약:** 어디서 터지는지와 영향 규모
2. **흐름:** 사용자 행동을 단계별로, 로그 시각과 함께
3. **원인:** 코드로 확인한 이유, `파일:줄` 근거
4. **막는 방법:** 방향 1~3개와 추천
5. **판정**
   - `FIXABLE`: 앱 코드에 고칠 곳이 있다 → 수정 PR
   - `ESCALATE-방어`: 원인은 밖에 있지만 앱에서 막을 수 있다 → 방어 PR
   - `ESCALATE-불가`: 앱에서 손댈 곳이 없다(SDK·OS 버그) → 이슈 링크와 함께 기록만
6. **미검증:** 확인하지 못한 것

## 5. 수정 원칙

- **작게 먼저.** 확신 있는 최소 수정으로 시작한다. 다만 try-catch·guard로 덮은 것을 근본 해결이라고 쓰지 않는다. 방어 코드면 "방어"라고 적고 근본 수정 방향을 함께 남긴다.
- **재현 테스트는 회귀 테스트로 남긴다.** Android `testing`, iOS `ios-testing` 스킬 규칙을 따른다.
- **구조를 바꾸는 판단은 사람에게.** 수명 맞추기, 화면·팝업 구조 변경 같은 선택지를 보류할 때는 "원래 불가능"인지 "지금 구조에서만 불가능"인지 근거를 남기고 묻는다.
- **사용 흐름을 따로 확인한다.** 크래시는 막았는데 기다림이 길어지거나, 화면이 깜빡이거나, 광고가 엉뚱한 때 뜨지 않는지 확인한다. 화면을 바꿨으면 Maestro Flow를 돌린다.
- **양쪽 플랫폼을 함께 본다.** 같은 로직이 반대쪽에도 있으면 함께 고치거나 왜 아닌지 적는다.
- **단서가 모자랐다면 단서를 더한다.** 다음 크래시를 위해 필요한 로그를 `CrashBreadcrumbs`에 더하는 것도 수정에 포함한다. 입력값은 넣지 않는다.
- PR은 브랜치를 따로 만들고 auto-fix를 켠다. 커밋 메시지·PR 설명은 한국어. 배포한 뒤 같은 크래시가 다시 나면 같은 PR·이슈 기록에 이어서 무엇이 남았는지 본다.
