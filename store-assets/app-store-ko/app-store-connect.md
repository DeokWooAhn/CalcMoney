# App Store Connect 입력값

앱을 처음 만들 때와 첫 버전을 심사에 낼 때 App Store Connect가 묻는 항목의 답이다.
이름·설명·키워드는 `listing-ko-KR.txt`·`../app-store-en/listing-en-US.txt`, 앱 개인정보 보호 답은 `app-privacy.md`에 있다.

## 1. 새로운 앱 만들기

| 항목 | 값 |
|---|---|
| 플랫폼 | iOS |
| 이름 | 심플 환율 계산기 - 여행 환전 |
| 기본 언어 | 한국어로 만든 뒤 영어(미국)로 바꾼다(아래 2절) |
| 번들 ID | `com.deokwooahn.calcmoney` (`GoogleService-Info.plist`와 같다) |
| SKU | `calcmoney-ios` (사용자에게 보이지 않는 내부 식별자. 나중에 바꿀 수 없다) |
| 사용자 액세스 | 전체 액세스 |

번들 ID는 Apple Developer의 Identifiers에 먼저 등록돼 있어야 목록에 나온다. 2026-10-30에 App Attest 기능과 함께 등록했다. 처음 쓰려던 `com.ahn.CalcMoney`는 다른 개발자가 이미 등록해 쓸 수 없었다(번들 ID는 App Store 전체에서 하나뿐이다).

앱 이름도 App Store 전체에서 하나뿐이다. "심플 환율 계산기"는 이미 쓰여 있어 2026-10-30에 "심플 환율 계산기 - 여행 환전"으로 만들었다. 홈 화면 이름(`InfoPlist.xcstrings`의 `CFBundleDisplayName`)은 그대로 "심플 환율 계산기"다. 이름에 든 단어는 키워드와 겹치면 낭비라 키워드에서 "환전"을 뺐다.

## 2. 앱 정보

| 항목 | 값 |
|---|---|
| 부제 | `listing-ko-KR.txt` 참고 |
| 카테고리 | 기본 유틸리티, 보조 여행 |
| 콘텐츠 권한 | 제3자 콘텐츠를 표시함: **예**. 환율 데이터(ExchangeRate-API)를 보여 준다. 쓰는 요금제의 이용 조건(출처 표시 등)을 지키는지 확인하고 답한다. 앱은 설정의 "환율 기준 정보"에 출처를 표시한다 |
| 연령 등급 | 아래 3절 |

개인정보처리방침 URL(https://deokwooahn.github.io/CalcMoney/privacy-policy.html)은 앱 정보가 아니라 "앱 개인정보 보호" 화면에서 언어별로 넣는다.

영어(미국) 현지화를 추가하고 `listing-en-US.txt`의 이름·부제를 넣는다. 영어 이름도 "Simple Currency Calculator"가 이미 쓰여 2026-10-30에 "Simple Currency Calc - Travel"로 정했다. 이름에서 "Calculator"가 빠져 영어 키워드에 `calculator`를 넣고, 이름과 겹치는 `travel`과 100자를 넘기는 `eur`를 뺐다.

**기본 언어는 영어(미국)로 둔다.** 스토어 언어 중 맞는 현지화가 없으면 기본 언어의 이름·설명·스크린샷이 보이는데, 영어(미국)를 지원하는 스토어는 미국·일본뿐이고 EU 나라와 영국·태국·베트남·대만 등은 영어(영국)만 지원한다([App Store localizations](https://developer.apple.com/help/app-store-connect/reference/app-store-localizations/)). 기본 언어가 한국어면 그 나라들에 한국어 페이지가 나간다. 한국 스토어는 한국어 현지화가 있어 한국어로 보인다. 기본 언어는 내용을 채운 현지화로만 바꿀 수 있어 영어 이름·부제를 먼저 저장한다.

## 3. 연령 등급

예상 등급은 **4+**다.

- 폭력, 성적 내용, 욕설, 공포, 약물, 도박, 의료 정보 같은 콘텐츠 항목: 모두 **없음**
- 사용자 생성 콘텐츠, 사용자 간 메시지·채팅: **아니요**
- 무제한 웹 접근: **아니요**. 개인정보처리방침 링크는 앱 안이 아니라 Safari로 연다
- 광고가 있는지 묻는 항목이 나오면 **예**

광고는 앱의 연령 등급에 맞아야 한다(심사 지침 2.5.18). AdMob iOS 앱을 만들 때 **최대 광고 콘텐츠 등급**을 `G`(모든 연령)로 정하는 것을 권한다. 지금 코드에는 이 설정이 없어 콘솔 설정을 따른다.

## 4. 가격 및 사용 가능 여부

| 항목 | 값 |
|---|---|
| 가격 | 무료 |
| 국가 | 전체(EU 포함). EU에 내므로 아래 DSA 신고가 필요하다 |

**EU 디지털 서비스법(DSA) 거래자 신고**: EU 스토어에 내려면 거래자인지 신고해야 한다. 광고로 수익을 내면 거래자로 보는 것이 일반적이다. 거래자로 신고하면 주소·전화번호·이메일이 EU 앱 페이지에 공개된다. **EU에도 출시하기로 했다(2026-10-29).** 거래자로 신고하고 연락처를 입력한다. Apple이 연락처를 확인하는 데 시간이 걸릴 수 있어 앱을 만든 직후 신고한다.

## 5. 버전 정보 (1.0.0)

App Store Connect가 버전 번호를 `1.0`으로 만들어 두는데, 빌드의 `MARKETING_VERSION`(1.0.0)과 같아야 빌드를 붙일 수 있어 `1.0.0`으로 고친다.

| 항목 | 값 |
|---|---|
| 스크린샷 | iPhone 6.3형(필수, 1206×2622): `iphone63-0{1,2,3,4}.png`, iPhone 6.9형(선택, 1320×2868): `iphone-0{1,2,3,4}.png`, iPad 13형(필수): `ipad-0{1,2,3,4}.png` (영어는 `../app-store-en/`). 2026-10-30 기준 App Store Connect의 iPhone 필수 크기는 "Dynamic Island 지원 iPhone(중형 디스플레이)"라 6.9형 이미지를 `sips -z 2622 1206`으로 줄여 만들었다 |
| 프로모션 텍스트·설명·키워드 | 언어별 listing 파일. 설명에 `★`를 넣으면 "유효하지 않은 문자"로 저장되지 않는다 |
| 지원 URL | https://deokwooahn.github.io/CalcMoney/support.html |
| 마케팅 URL | 비워 둔다 |
| 저작권 | `2026 DeokWoo Ahn`. 판매자 이름(비즈니스 화면의 법적 이름)과 맞춘다 |
| 빌드 | Xcode Archive로 올린 빌드. 수출 규정은 `Info.plist`의 `ITSAppUsesNonExemptEncryption = NO`로 미리 답해 두어 빌드마다 묻지 않는다 |
| 버전 출시 | **이 버전을 수동으로 출시**. 첫 출시는 승인 뒤 TestFlight·실기기 확인을 한 번 더 하고 내보낸다 |

## 6. 앱 심사 정보

| 항목 | 값 |
|---|---|
| 로그인 필요 | 아니요 (계정 기능이 없다) |
| 연락처 | 이름·전화번호는 직접 입력. 이메일은 지원 페이지와 같은 `dwahndev@gmail.com` |
| 첨부 파일 | 없음 |

심사자는 영어로 읽으므로 메모는 영어로 쓴다.

```
No account or login is required.

The app is a calculator with currency conversion. Exchange rates are downloaded
from our server (data source: ExchangeRate-API) and cached on the device, so the
app keeps working offline after the first launch.

On first launch, Google's consent form (UMP) appears where required by the user's
region, followed once by the App Tracking Transparency prompt. Ads are served by
Google AdMob: banner ads on the Exchange, Favorites and Settings tabs, and an
occasional interstitial ad when leaving the Calculator tab. Every feature works
whether or not tracking is allowed.
```

(요약: 로그인 없음. 환율은 서버에서 받아 기기에 저장해 오프라인에서도 쓴다. 첫 실행 때 지역에 따라 UMP 동의 창, 이어서 ATT 창이 한 번 뜬다. 배너 3곳과 계산기를 떠날 때 가끔 전면 광고가 나오고, 추적 허용과 무관하게 모든 기능을 쓸 수 있다.)
