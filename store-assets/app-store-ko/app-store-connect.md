# App Store Connect 입력값

앱을 처음 만들 때와 버전을 심사에 낼 때 App Store Connect가 묻는 항목의 답이다. 다음 버전을 내는 순서는 7절에 있다.
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
- 위치 공유, 데이팅(2026-11 추가된 질문): 둘 다 **아니요**. 위치를 쓰지 않고 사용자끼리 만나는 기능이 없다.
  출시된 버전만 있으면 설문이 읽기 전용이라(2026-11-23 1.0.0 "배포 준비됨" 상태에서 확인), 2026-11-24 1.0.1을 "제출 준비 중"으로 만든 뒤 답했다. 등급은 4+ 그대로다

광고는 앱의 연령 등급에 맞아야 한다(심사 지침 2.5.18). AdMob iOS 앱을 만들 때 **최대 광고 콘텐츠 등급**을 `G`(모든 연령)로 정하는 것을 권한다. 지금 코드에는 이 설정이 없어 콘솔 설정을 따른다.

## 4. 가격 및 사용 가능 여부

| 항목 | 값 |
|---|---|
| 가격 | 무료 |
| 국가 | 중국 본토를 뺀 전체(174개, EU 포함). EU에 내므로 아래 DSA 신고가 필요하다. 중국 본토를 뺀 이유는 `docs/decisions/china-mainland-not-distributed.md` |

**EU 디지털 서비스법(DSA) 거래자 신고**: EU 스토어에 내려면 거래자인지 신고해야 한다. 광고로 수익을 내면 거래자로 보는 것이 일반적이다. 거래자로 신고하면 주소·전화번호·이메일이 EU 앱 페이지에 공개된다. **EU에도 출시하기로 했다(2026-10-29).** 거래자로 신고하고 연락처를 입력한다. Apple이 연락처를 확인하는 데 시간이 걸릴 수 있어 앱을 만든 직후 신고한다.

## 5. 버전 정보 (1.0.0)

App Store Connect가 버전 번호를 `1.0`으로 만들어 두는데, 빌드의 `MARKETING_VERSION`(1.0.0)과 같아야 빌드를 붙일 수 있어 `1.0.0`으로 고친다.

| 항목 | 값 |
|---|---|
| 스크린샷 | iPhone 6.3형(필수, 1206×2622): `iphone63-0{1,2,3,4}.png`, iPhone 6.9형(선택, 1320×2868): `iphone-0{1,2,3,4}.png`, iPad 13형(필수): `ipad-0{1,2,3,4}.png` (영어는 `../app-store-en/`). 2026-10-30 기준 App Store Connect의 iPhone 필수 크기는 "Dynamic Island 지원 iPhone(중형 디스플레이)"라 6.9형 이미지를 `sips -z 2622 1206`으로 줄여 만들었다 |
| 프로모션 텍스트·설명·키워드 | 언어별 listing 파일. 설명에 `★`를 넣으면 "유효하지 않은 문자"로 저장되지 않는다 |
| 지원 URL | https://deokwooahn.github.io/CalcMoney/support.html |
| 마케팅 URL | https://deokwooahn.github.io/CalcMoney/support.html (언어마다). **비워 두면 AdMob 앱 인증이 실패한다.** AdMob은 App Store 앱의 개발자 웹사이트를 마케팅 URL에서 찾고, 그 호스트 루트의 `app-ads.txt`(https://deokwooahn.github.io/app-ads.txt)를 확인한다. 2026-11-23 1.0.0을 비워 둔 채 출시해 AdMob iOS 앱이 "광고 게재가 제한됨"으로 남았다. 출시된 버전에서는 고칠 수 없어 1.0.1(2026-11-26 출시)에 넣었다. 마케팅 URL이 스토어에 반영된 뒤 AdMob 앱 설정에서 "업데이트 확인"을 누른다(최대 24시간) |
| 저작권 | `2026 DeokWoo Ahn`. 판매자 이름(비즈니스 화면의 법적 이름)과 맞춘다 |
| 빌드 | Xcode Archive로 올린 빌드. 수출 규정은 앱 타깃 빌드 설정의 `INFOPLIST_KEY_ITSAppUsesNonExemptEncryption = NO`로 미리 답해 두어 빌드마다 묻지 않는다. 빌드 번호(앱 타깃의 `CURRENT_PROJECT_VERSION`)는 iOS 배포 자동화가 없어 올릴 때마다 손으로 1씩 올린다. 이미 올린 번호는 다시 쓸 수 없다 |
| 버전 출시 | **이 버전을 수동으로 출시**. 첫 출시는 승인 뒤 TestFlight·실기기 확인을 한 번 더 하고 내보낸다 |

## 6. 앱 심사 정보

| 항목 | 값 |
|---|---|
| 로그인 필요 | 아니요 (계정 기능이 없다) |
| 연락처 | 이름·전화번호는 직접 입력. 이메일은 지원 페이지와 같은 `dwahndev@gmail.com` |
| 첨부 파일 | 1.0.0은 실기기 화면 녹화 영상을 올렸다. 새 버전은 메모만 복사되고 첨부 파일은 비어 있어, 1.0.1부터는 비워 두고 메모에서 녹화 문단을 뺐다. Apple이 다시 요구하면 아래 방법으로 찍는다. 확장자가 대문자 `.MP4`면 "지원되지 않는 확장자"로 거부되니 소문자 `.mp4`로 바꾼 사본을 올린다 |

심사자는 영어로 읽으므로 메모는 영어로 쓴다. 첫 제출(2026-10-31) 때 Apple이 "심사 이력이 적은 개발자 계정"이라며 Guideline 2.1 Information Needed로 화면 녹화와 설명 다섯 가지를 요구했다. 답한 설명을 다음 제출에도 쓰도록 메모 칸에 그대로 둔다(4000자 제한 안). 아래는 녹화 문단을 뺀 1.0.1의 메모다.

```
No account or login is required.

1. Purpose and target audience
Simple Currency Calculator is a calculator with built-in currency conversion. It is for travelers and online shoppers, mainly Korean users, who add up prices in a foreign currency and want to see the total in their own currency. Instead of switching between a calculator and a converter, the user types an expression such as 1,980 + 2,480 and sees the result and its converted value together while typing. Rates are cached on the device, so the app keeps working offline (for example on a plane) after the first launch.

2. How to use the main features
No login, setup or sample files are needed. An internet connection is needed only the first time, to download the rates.
- Calculator tab: the two currencies are at the top. Tap one to search for and pick another currency, or tap the arrows between them to swap. Type an expression; the converted amount appears below the result. The clock key shows the calculation history.
- Exchange tab: enter an amount to convert it between two currencies. The rate used and its date are shown.
- Favorites tab: in the currency picker, tap the heart next to a currency to add it. The Favorites tab converts one base amount into all favorite currencies.
- Settings tab: Theme (system, light or dark), Exchange rate details (source and date), Privacy policy, Recommend to a friend, and Privacy and ad settings (shown only where Google's consent form is required).

3. External services
- ExchangeRate-API (exchangerate-api.com): source of the exchange rates. Our server fetches them once a day; the app never calls this API directly.
- Google Firebase: Cloud Functions (daily rate update on our server), Cloud Firestore (delivers the rate document to the app), App Check with App Attest (protects the database), Crashlytics (crash reports) and Google Analytics for Firebase (usage analytics).
- Google AdMob (Google Mobile Ads SDK): banner ads on the Exchange, Favorites and Settings tabs, and an occasional interstitial ad when leaving the Calculator tab.
- Google User Messaging Platform: consent form where required by law.
- Apple App Tracking Transparency. Every feature works whether or not tracking is allowed.
The app uses no authentication, payment or AI services.

4. Regional differences
All features and content are the same in every region. Rates are quoted against the Korean won for all users, and the interface follows the device language (English or Korean). The only regional differences come from privacy law: Google's consent form and the Privacy and ad settings item appear only in the EEA, the UK and Switzerland, and the ads themselves vary by region.

5. Regulated industry and third-party material
The app is not a financial service. It does not trade, transfer or hold money and gives no financial advice; rates are labeled as reference only. The exchange rate data is provided by ExchangeRate-API under its terms of use and is credited in the app (Settings > Exchange rate details > Data source) and in the App Store description. The app contains no other third-party content.
```

**화면 녹화는 실기기, 최신 iOS, 앱 실행부터 시작**이 조건이다(1.0.0 때 요구).

- iPhone을 최신 iOS로 올리고, TestFlight에서 앱을 지웠다가 다시 설치해 첫 실행의 ATT 창부터 찍는다.
- 심사자가 읽기 쉽게 iOS 설정 > 앱 > 이 앱 > 언어를 English로 바꿔 찍는다(기기 언어는 그대로 둬도 된다).
- 제어 센터의 화면 기록으로 홈 화면에서 시작해 1~2분 안에 끝낸다. 광고는 누르지 않는다.
- 영상은 App Store Connect의 심사 메시지 답장에 첨부하고, 같은 영상을 버전 페이지 "앱 심사 정보 > 첨부 파일"에도 올린다. 같은 글은 "앱 심사 정보 > 메모"에 넣는다.

정보 요청에는 답장만 하고 다시 제출하지 않는 것이 Apple 안내다. 다시 제출하면 새 심사가 시작되고 답장하던 대화가 닫힌다. 다만 답장해도 응답이 없을 수 있다. 1.0.0 (1)은 2026-11-02에 답장한 뒤 8일 동안 응답이 없어, Firebase 보안 업데이트를 넣은 1.0.0 (2)로 빌드를 바꿔 다시 제출했다(2026-11-10). 이때 답변은 메모 칸에, 영상은 첨부 파일 칸에 있어야 새 심사자도 본다.

심사를 통과하지 못한 제출을 고치는 순서는 다음과 같다.

1. 앱 심사 화면의 제출 항목에서 "편집"을 누른다.
2. 빌드·메모·첨부 파일을 한 번에 고친다. 이 상태에서는 "저장" 대신 "심사 업데이트"가 저장 버튼이다.
3. 앱 심사 화면으로 돌아가 "앱 심사에 다시 제출"을 누른다. "심사 업데이트"만으로는 심사에 들어가지 않는다.

## 7. 다음 버전 내기

1.0.1(2026-11-24 제출, 2026-11-26 출시)에서 밟은 순서다.

1. 앱 타깃의 `MARKETING_VERSION`과 `CURRENT_PROJECT_VERSION`(+1)을 PR로 올린다. 테스트 타깃은 그대로 둔다.
2. Xcode Archive로 올린다. 로컬 `ios/CalcMoney/Config/AdMob.xcconfig`가 있어야 Release 빌드가 된다.
3. App Store Connect의 앱 페이지에서 새 iOS 버전을 만든다. 설명·키워드·스크린샷·심사 메모는 이전 버전에서 복사되고, 심사 첨부 파일은 비어 있다.
4. 언어마다 "이 버전의 새로운 기능"을 쓰고, 빌드를 연결한다. 연령 등급에 새 질문이 생겼으면 이때 답한다.
5. "심사에 추가"로 제출하고, 승인 메일이 오면 버전 페이지에서 "이 버전 출시"를 누른다(수동 출시).

1.0.1은 정보 요청 없이 승인됐다.
