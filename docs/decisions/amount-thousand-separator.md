# 금액에는 계산기와 같은 천 단위 쉼표를 표시에만 넣는다

- 정한 날: 2026-11-27
- 근거: 2026-11-25 iOS 시뮬레이터 확인에서 환율 화면 받을 금액이 "1343.87", 계산기가 "1,000"으로 달라 보인 것. 2026-11-27 대화에서 두 플랫폼을 함께 고치기로 정했다
- 적용: Android·iOS 환율 화면의 받을 금액, 즐겨찾기 카드 금액

## 규칙

- 환율 화면의 받을 금액과 즐겨찾기 카드 금액은 계산기처럼 정수부에 세 자리마다 쉼표를 넣는다(`1,343.87`)
- **쉼표는 표시에만 넣는다.** 상태의 금액(`toAmount`)과 도메인 `CalculateExchangeAmountUseCase`의 결과는 쉼표 없는 숫자 문자열로 둔다.
  통화를 바꾸면 `toAmount`가 그대로 기준 금액(`fromAmount`)으로 옮겨져 숫자로 읽히기 때문이다
- 구분 기호는 기기 로케일을 따르지 않고 계산기와 같은 고정 형식(쉼표·점)을 쓴다. 한 화면 안에서 계산기와 환율 금액의 모양이 같아야 한다
- 형식 함수는 계산기 것을 쓴다: Android `formatNumberWithCommas`, iOS `CalculatorFormatting.formatWithCommas`

## 하지 않기로 한 것

| 안 | 이유 |
| --- | --- |
| 기준 금액 입력 칸에도 쉼표 | 입력 중에 글자를 바꿔야 해 커서 위치·붙여넣기를 따로 다뤄야 한다(iOS `TextField`는 Compose `VisualTransformation` 같은 표시 전용 변환이 없다). 필요해지면 따로 한다 |
| 환율 줄(`1 USD = 1342.9849 KRW`)에도 쉼표 | 소수 넷째 자리까지 보이는 참고 값이고 이번 문제(계산기와 다른 모양) 범위가 아니다 |
| 기기 로케일 형식(`NumberFormatter`·`DecimalFormat` 로케일) | 계산기가 고정 형식이라 화면끼리 모양이 달라진다. 독일어처럼 쉼표·점이 뒤바뀌는 로케일에서 계산기 입력과 헷갈린다 |

## 코드

- Android: `presentation/.../ui/component/ExchangeInputContainer.kt`(읽기 전용 금액), `ui/screen/exchange/FavoriteRatesLoader.kt`(`convertedAmount`)
- iOS: `Presentation/Component/ExchangeInputContainer.swift`, `Presentation/Screen/Exchange/FavoriteRatesLoader.swift`

## 확인 방법

- Android `FavoriteRatesLoaderTest`·`ThousandSeparatorTransformationTest`, iOS `FavoriteRatesLoaderTests`·`CalculatorFormattingTests`
- Maestro `smoke_exchange_favorite`가 받을 금액 `12,000.00`을 확인한다(두 플랫폼 같은 Flow)
