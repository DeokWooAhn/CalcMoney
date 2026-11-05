# 통화 목록은 서버가 정하고, 순서·국기·이름은 앱이 만든다

- 정한 날: 2026-09-28(#57), 이름 순서는 2026-10-28(#92)
- 근거: #57(환율 출처를 ExchangeRate-API로 교체, 빠른 이동 인덱스), #92(영어 화면의 한국어 통화 이름)
- 적용: Android·iOS의 통화 선택·표시, Cloud Functions가 저장하는 순서

## 규칙

- **통화 목록을 앱에 하드코딩하지 않는다.** 목록은 Firestore `exchangeRates/latest`가 정한다.
- **순서:** 통화 선택 화면은 즐겨찾기를 맨 위에 두고, 나머지를 통화 코드 알파벳순으로 앱에서 직접 정렬한다.
  서버도 알파벳순으로 저장한다.
- **국기:** 통화 코드 앞 두 글자로 만든다. 예외는 `EUR`→🇪🇺, `ANG`·`XCG`→🇨🇼(퀴라소).
  `XAF`·`XOF`·`XCD`·`XPF`·`XDR`처럼 여러 나라가 함께 쓰는 `X`로 시작하는 통화는 🌐.
- **이름:** 아래 순서로 처음 나오는 것을 쓴다.
  1. 직접 다듬은 번역 22개
  2. 플랫폼(OS) 현지화 이름
  3. 예비 이름 12개(한국어·영어)
  4. 서버 이름. **한국어 화면에서만** 쓰고, 다른 언어 화면에서는 대신 통화 코드를 보여 준다.
- **예비 이름**은 플랫폼이 모를 수 있는 통화의 이름이다. ISO 4217에 없는 `FOK`·`GGP`·`IMP`·`JEP`·`KID`·`TVD`와
  오래된 OS가 모르는 `MRU`·`SLE`·`STN`·`VES`·`XCG`·`ZWG`. 서버가 새 통화를 주면 여기에 더한다.

## 이유

- 통화를 22개에서 165개로 늘리며 출처를 바꿨다(#57). 목록이 서버에서 바뀌어도 앱 업데이트 없이 따라가게 한다.
- 오른쪽 ★·A~Z 빠른 이동 인덱스가 이 정렬 순서에 기대고 있다.
- 서버가 알파벳순으로 저장하는 것은 정렬 코드가 없는 구버전 앱도 같은 순서로 보이게 하려는 것이다.
- 서버 이름은 늘 한국어다. Cloud Functions가 `Intl.DisplayNames(["ko"])`로 만든다. 그대로 쓰면 다른 언어 화면에
  한국어가 섞인다. #92 전에는 영어로 "J"를 검색하면 맨 위의 `JEP`가 "저지 파운드"로 나왔다.
- 플랫폼이 이름을 모르는 통화는 두 종류다(2026-10-28 확인).
  - ISO 4217에 없는 지역 통화 6개는 어떤 OS도 모른다.
  - iOS 18.3 시뮬레이터는 `XCG`·`ZWG`를 몰랐다. iOS 26부터는 안다.
  - Android 최소 버전(Android 9)의 ICU는 더 오래돼 `MRU`·`SLE`·`STN`·`VES`도 모를 수 있다.

## 코드

- 정렬: Android `presentation/.../ui/component/CurrencyPickerList.kt`,
  iOS `ios/Packages/Presentation/Sources/Presentation/Component/CurrencyPickerList.swift`
- 국기·이름: Android `data/.../exchange/mapper/ExchangeRateMapper.kt`, `presentation/.../util/CurrencyNameFormatter.kt`,
  iOS `ios/Packages/Data/Sources/Data/Exchange/Mapper/CurrencyInfoMapper.swift`,
  `ios/Packages/Presentation/Sources/Presentation/Util/CurrencyName.swift`
