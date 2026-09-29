---
name: maestro
description: Maestro E2E Flow(.maestro/)를 작성·수정·실행하거나, 화면에 테스트 id를 붙일 때 사용. Maestro MCP로 기기를 조작해 Flow를 만들고 CLI로 검증하는 절차와 이 저장소의 id·태그 규칙.
---

# Maestro E2E 테스트

Flow 하나로 Android와 iOS를 함께 돌린다. 두 플랫폼이 **같은 id**를 쓰는 게 전제다.

## 구조

```
.maestro/
  config.yaml              # flows/* 만 테스트로 잡는다
  subflows/launch_clean.yaml   # 데이터 초기화 + UI_TESTING 실행 + 계산기 화면 대기
  flows/smoke_*.yaml       # [smoke] 네트워크 없이 통과해야 한다
  flows/release_*.yaml     # [release] Firestore에서 환율을 받아야 통과한다
```

## 실행

`APP_ID`는 플랫폼마다 다르다(대소문자까지). 저장소 루트에서 실행한다.

```bash
# Android (debug 빌드 설치 후)
maestro --device <serial> test .maestro -e APP_ID=com.ahn.calcmoney
# iOS 시뮬레이터 (debug 빌드 설치 후)
maestro --device <udid> test .maestro -e APP_ID=com.ahn.CalcMoney
# 태그로 거르기
maestro test .maestro -e APP_ID=... --include-tags=smoke
```

- Android는 실기기가 연결돼 있으면 실기기로 돌린다. 실기기에 Play 스토어판이 깔려 있으면 서명이 달라
  debug 앱을 덮어쓸 수 없고, 지우면 사용자 데이터가 사라지므로 **지우기 전에 반드시 사용자에게 묻는다.**
- 실패하면 `--test-output-dir`에 스크린샷이 남는다. 화면 계층은 `maestro --device <id> hierarchy --compact`.
- iOS 빌드: `ios/`에서 `xcodebuild build -workspace CalcMoney.xcworkspace -scheme CalcMoney -destination 'platform=iOS Simulator,id=<udid>' -derivedDataPath <dir>` 후 `xcrun simctl install`.

## MCP로 Flow 만들기

`.mcp.json`에 `maestro mcp`가 등록돼 있다. 기기를 켠 상태에서:

1. `inspect_view_hierarchy`로 현재 화면의 id를 확인한다.
2. `tap_on`·`run_flow`로 한 단계씩 조작해 본다.
3. 성공한 단계를 `.maestro/flows/`에 YAML로 옮기고 `check_flow_syntax`로 검사한다.
4. `run_flow_files`로 처음부터 다시 돌린 뒤, **CLI로 Android와 iOS 양쪽에서** 통과하는지 확인한다.

MCP로 한 번 성공한 시나리오와 매번 통과하는 시나리오는 다르다. 좌표(`point:`)로 누르는 단계는 옮기지 않는다.

## id 규칙

| 대상 | id | Android | iOS |
|---|---|---|---|
| 계산기 키 | `keypad.7`, `keypad.plus`, `keypad.equals` … | `CalculatorKey.testTag` | `CalculatorKey.testID` |
| 하단 탭 | `tab.calculator` `tab.exchange` `tab.favorite` `tab.setting` | `BottomNavItem.testTag` | `MainTab.testID` |
| 화면 기준점 | `screen.<탭>` | 각 Screen 루트 | `MainTab.screenTestID` |
| 환율 입력 카드 | `exchange.from`, `exchange.to` | `ExchangeScreen` | `ExchangeView` |
| 통화 선택 버튼 | `currency.selector` | `CurrencySelector` | `CurrencySelectorView` |
| 선택 창 행·하트·닫기 | `currency.row.<코드>`, `favorite.toggle.<코드>`, `currency.picker.close`(iOS만) | `CurrencyPickerDialog` | `CurrencyPickerSheet` |
| 즐겨찾기 카드 | `favorite.card.<코드>` | `FavoriteScreen` | `FavoriteView` |

- **Maestro는 `id:`와 글자 단언을 정규식으로 해석한다.** `keypad.+`는 "keypad 뒤에 아무 글자나"가 되어
  다른 키를 누른다. id에 기호를 쓰지 말고 영어 이름으로 짓는다. 글자 단언의 `.`은 `\\.`로 이스케이프한다.
- 한 id가 다른 id의 앞부분이 되지 않게 짓는다(`currency.row.AUD`와 `favorite.toggle.AUD`처럼 종류를 앞에 둔다).
- 화면 문구로 찾지 않는다. 앱은 영어·한국어를 지원해서 기기 언어가 바뀌면 깨진다. 예외는 통화 코드·숫자처럼
  언어와 무관한 글자다(`"1 [A-Z]{3} = [0-9]+\\.[0-9]{4} [A-Z]{3}"`).
- Android는 `MainScreen`에서 `testTagsAsResourceId`를 켜서 testTag를 노출한다. **Dialog는 별도 창이라
  적용되지 않으므로** 새 Dialog에는 따로 켠다(`CurrencyPickerDialog` 참고).
- iOS 컨테이너에 id를 달 때는 `.accessibilityElement(children: .contain)`를 함께 쓴다. 안 그러면 id가 없는
  자식 요소가 모두 같은 id를 물려받는다.
- 계산기 키 id는 양쪽 유닛 테스트(`CalculatorKeyTestTagTest.kt`, `CalculatorKeyTests.swift`)가 고정한다.
  바꾸면 Flow와 반대쪽 플랫폼도 함께 바꾼다.

## Flow 작성 규칙

- 모든 Flow는 `runFlow: ../subflows/launch_clean.yaml`로 시작한다. `clearState`로 매번 빈 상태에서 시작하고,
  `UI_TESTING` 인자가 광고 동의 창과 광고 로드를 끈다. **실제 광고를 노출하거나 누르는 Flow를 만들지 않는다.**
- 태그는 `smoke`(네트워크 불필요, PR마다) 또는 `release`(Firestore 필요, 릴리스 전)만 쓴다.
- 환율 값처럼 매일 바뀌는 값은 단언하지 않는다. 형식만 본다.
- 계산 결과는 키패드 글자(한 자리 숫자)와 겹치지 않는 값으로 단언한다. `"7"`을 단언하면 7 키가 잡힌다.
- 플랫폼마다 조작이 다르면 `runFlow: { when: { platform: Android|iOS }, commands: [...] }`로 나눈다
  (예: 선택 창 닫기는 Android `back`, iOS `currency.picker.close`).
- `scrollUntilVisible` 기본 제한 시간은 20초다. 목록 뒤쪽 항목은 닿지 못하니 앞쪽 항목을 쓰거나 `timeout`을 늘린다.
- 즐겨찾기 화면은 기준 통화와 같은 통화를 카드로 만들지 않는다. 즐겨찾기 Flow에서 그 통화를 기준 통화로 고르지 않는다.

## 알려진 제약

- debug 빌드는 App Check debug provider를 쓴다. Firestore가 App Check를 강제하면 기기의 디버그 토큰을
  Firebase 콘솔에 등록해야 `release` Flow가 통과한다(첫 실행 시 Logcat/Xcode 콘솔에 토큰이 찍힌다).
- CI 연동은 아직 없다. 계획은 PR에 `smoke`, `android-v*` 태그에 `release`를 붙이는 것이다.
