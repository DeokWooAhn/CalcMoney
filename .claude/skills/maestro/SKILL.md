---
name: maestro
description: 이 저장소의 Maestro E2E 설정. Maestro Flow(.maestro/)를 작성·수정·실행·리뷰하거나 화면에 테스트 id를 붙일 때 사용. 플랫폼별 APP_ID, iOS 빌드 명령, id 표, smoke·release 기준, 이 앱에서만 생기는 Flow 함정, CI·App Check 제약.
---

# Maestro E2E — CalcMoney

공통 절차(프로젝트 분석, smoke 만들기, 기기 고르기, MCP로 Flow 만들기, id·작성 규칙, 리뷰, 실패 판정)는
`maestro-e2e` 플러그인의 `flows` 스킬을 따른다. 여기에는 **이 저장소에만 해당하는 것**만 둔다. 둘이 다르면 여기가 우선이다.

Flow 하나로 Android와 iOS를 함께 돌린다. 두 플랫폼이 **같은 id**를 쓴다.

## 구조

```
.maestro/
  config.yaml                  # flows/* 만 테스트로 잡는다
  subflows/launch_clean.yaml   # 데이터 초기화 + UI_TESTING 실행 + 계산기 화면 대기
  flows/smoke_*.yaml           # [smoke] 네트워크 없이 통과해야 한다
  flows/release_*.yaml         # [release] Firestore에서 환율을 받아야 통과한다
```

## 실행

`APP_ID`는 플랫폼마다 다르다(대소문자까지). 저장소 루트에서 실행한다.

```bash
# Android (debug 빌드 설치 후)
maestro --device <serial> test .maestro -e APP_ID=com.ahn.calcmoney
# iOS 시뮬레이터 (debug 빌드 설치 후)
maestro --device <udid> test .maestro -e APP_ID=com.ahn.CalcMoney
```

- iOS 빌드: `ios/`에서 `xcodebuild build -workspace CalcMoney.xcworkspace -scheme CalcMoney -destination 'platform=iOS Simulator,id=<udid>' -derivedDataPath <dir>` 후 `xcrun simctl install`.
- `UI_TESTING` 인자가 광고 동의 창과 광고 로드, 인앱 리뷰 창을 끈다. Android는 디버그 빌드에서만 받는다.
  iOS 개발 빌드는 리뷰를 요청할 때마다 창을 띄우므로, 이 인자 없이 계산을 반복하는 Flow는 리뷰 창에 막힐 수 있다.

## id 표

| 대상 | id | Android | iOS |
|---|---|---|---|
| 계산기 키 | `keypad.7`, `keypad.plus`, `keypad.equals` … | `CalculatorKey.testTag` | `CalculatorKey.testID` |
| 하단 탭 | `tab.calculator` `tab.exchange` `tab.favorite` `tab.setting` | `BottomNavItem.testTag` | `MainTab.testID` |
| 화면 기준점 | `screen.<탭>` | 각 Screen 루트 | `MainTab.screenTestID` |
| 환율 입력 카드 | `exchange.from`, `exchange.to` | `ExchangeScreen` | `ExchangeView` |
| 통화 선택 버튼 | `currency.selector` | `CurrencySelector` | `CurrencySelectorView` |
| 선택 창 검색·행·하트·닫기 | `currency.search`, `currency.row.<코드>`, `favorite.toggle.<코드>`, `currency.picker.close`(iOS만) | `CurrencyPickerSheet` | `CurrencyPickerSheet` |
| 즐겨찾기 카드 | `favorite.card.<코드>` | `FavoriteScreen` | `FavoriteView` |
| 친구에게 추천하기 | `setting.share` | `SettingScreen` | `SettingView`(App Store 출시 전에는 숨김) |

- Android는 `MainScreen`에서 `testTagsAsResourceId`를 켠다. 바텀시트·Dialog는 별도 창이라 따로 켠다(`CurrencyPickerSheet` 참고).
- 계산기 키 id는 양쪽 유닛 테스트(`CalculatorKeyTestTagTest.kt`, `CalculatorKeyTests.swift`)가 고정한다.
  바꾸면 Flow와 반대쪽 플랫폼도 함께 바꾼다.
- 앱은 영어·한국어를 지원한다. 화면 문구로 찾지 않는다. 통화 코드·숫자는 괜찮다
  (예: `"1 [A-Z]{3} = [0-9]+\\.[0-9]{4} [A-Z]{3}"`).

## 이 앱의 Flow 함정

- 계산 결과는 키패드 글자(한 자리 숫자)와 겹치지 않는 값으로 단언한다. `"7"`을 단언하면 7 키가 잡힌다.
- 환율 값은 매일 바뀐다. 값이 아니라 형식을 본다.
- 선택 창 닫기는 Android `back`, iOS `currency.picker.close`다. Android는 검색창에 입력해 키보드가 떠 있으면 첫 `back`이 키보드만 내리니 `hideKeyboard`를 먼저 한다.
- Android `inputText`는 adb 키 입력이라 기기가 터치 모드를 벗어난다. 그 뒤 시트·대화상자가 닫히면 일부 기기(Galaxy S25)는 뒤 화면의 첫 입력칸에 기본 포커스를 줘(`ViewRootImpl.leaveTouchMode` → `restoreDefaultFocus`) 키보드가 하단 탭을 가린다. 화면 키보드로 치는 사용자에게는 생기지 않는 Flow 전용 현상이라, 탭이 안 보일 때만 `hideKeyboard`를 한 번 더 한다(`release_favorite.yaml` 참고). 에뮬레이터에서는 재현되지 않으니 실기기로 확인한다.
- 통화 목록은 160개 안팎이다. `scrollUntilVisible` 기본 20초로는 뒤쪽 통화에 닿지 못하니, 스크롤하지 말고 `currency.search`에 통화 코드를 입력해 찾는다.
- 즐겨찾기 화면은 기준 통화와 같은 통화를 카드로 만들지 않는다. 즐겨찾기 Flow에서 그 통화를 기준 통화로 고르지 않는다.

## 제약

- debug 빌드는 App Check debug provider를 쓴다. Firestore가 App Check를 강제하면 기기의 디버그 토큰을
  Firebase 콘솔에 등록해야 `release` Flow가 통과한다(첫 실행 시 Logcat/Xcode 콘솔에 토큰이 찍힌다).
- CI는 `smoke`만 돌린다. Android는 `android-ci.yml`의 `e2e_smoke`(에뮬레이터 API 34, 영어), iOS는 `ios-ci.yml`의 `e2e_smoke`
  (iPhone 17 Pro 시뮬레이터, 최신 iOS)다. Flow를 고치면 두 job이 모두 돈다.
  `release`는 릴리스 전에 실기기에서 태그 커밋의 debug 빌드로 돌린다(`docs/continuous-deployment.md`의 "릴리스 전 실기기 확인").
- `e2e_smoke`는 태그 릴리스의 관문이다. `release_bundle`과 `firebase_app_distribution`이 이 job을 기다리므로
  smoke Flow가 불안정하면 릴리스가 막힌다. 새 `smoke` Flow는 CI 에뮬레이터에서 여러 번 통과하는지 확인하고 넣는다.
- CI 실행 여부는 `e2e_changes` job이 바뀐 파일로 정한다. 새 최상위 폴더를 만들었는데 앱과 무관하다면
  그 job의 제외 목록에 추가한다.
