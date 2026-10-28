# App Store 개인정보 라벨 초안

App Store Connect > 앱 > 앱 개인정보 보호(App Privacy)에 입력할 답이다.

## 근거

앱 코드는 아무것도 기기 밖으로 보내지 않는다. 계산 기록·즐겨찾기·설정은 기기 안에만 있고, Firestore는 환율 문서를 읽기만 한다.
그래서 라벨은 SDK가 수집하는 것만 모은 결과다.

| SDK | 버전 | 근거 |
|---|---|---|
| Google Mobile Ads | 12.14.0 | SDK 개인정보 매니페스트 |
| User Messaging Platform | 3.1.0 | SDK 개인정보 매니페스트 |
| Firebase Crashlytics·Firestore·Installations, GoogleDataTransport | 12.17.0 | SDK 개인정보 매니페스트 |
| Google Analytics for Firebase | 12.17.0 | 매니페스트가 없어 Google 지원 문서 기준. `GoogleAppMeasurementIdentitySupport`가 링크돼 있어 ATT를 허용하면 IDFA도 수집 |

여러 SDK가 같은 유형을 수집하면 목적은 합치고, 하나라도 "연결됨"·"추적"이면 그쪽으로 답했다.

## 1. 데이터 수집 여부

**예, 이 앱에서 데이터를 수집합니다.**

## 2. 데이터 유형 7개

| 분류 | 데이터 유형 | 수집하는 SDK |
|---|---|---|
| 위치 | 대략적인 위치 (Coarse Location) | AdMob, UMP, Analytics |
| 식별자 | 기기 ID (Device ID) | AdMob, Analytics |
| 사용 데이터 | 제품 상호 작용 (Product Interaction) | AdMob, UMP, Analytics |
| 사용 데이터 | 광고 데이터 (Advertising Data) | AdMob |
| 진단 | 충돌 데이터 (Crash Data) | Crashlytics, AdMob |
| 진단 | 성능 데이터 (Performance Data) | AdMob, UMP |
| 진단 | 기타 진단 데이터 (Other Diagnostic Data) | Crashlytics, Firestore, Installations, DataTransport, AdMob |

## 3. 유형별 답

| 데이터 유형 | 사용 목적 | 사용자에게 연결 | 추적에 사용 |
|---|---|---|---|
| 대략적인 위치 | 타사 광고, 개발자의 광고 또는 마케팅, 분석, 앱 기능 | 예 | 아니요 |
| 기기 ID | 타사 광고, 개발자의 광고 또는 마케팅, 분석 | 예 | **예** |
| 제품 상호 작용 | 타사 광고, 개발자의 광고 또는 마케팅, 분석, 앱 기능 | 예 | 아니요 |
| 광고 데이터 | 타사 광고, 개발자의 광고 또는 마케팅, 분석 | 예 | 아니요 |
| 충돌 데이터 | 분석, 앱 기능 | 아니요 | 아니요 |
| 성능 데이터 | 타사 광고, 개발자의 광고 또는 마케팅, 분석, 앱 기능 | 아니요 | 아니요 |
| 기타 진단 데이터 | 타사 광고, 개발자의 광고 또는 마케팅, 분석, 앱 기능 | 아니요 | 아니요 |

목적의 영어 이름: 타사 광고 = Third-Party Advertising, 개발자의 광고 또는 마케팅 = Developer's Advertising or Marketing, 분석 = Analytics, 앱 기능 = App Functionality.

## 4. 앱 페이지에 보이는 모습

- **사용자를 추적하는 데 사용되는 데이터:** 식별자
- **사용자에게 연결된 데이터:** 위치, 식별자, 사용 데이터
- **사용자에게 연결되지 않은 데이터:** 진단

## 고르지 않는 항목

| 항목 | 이유 |
|---|---|
| 사용자 콘텐츠, 기타 데이터 | 계산 기록·즐겨찾기·테마는 기기 밖으로 나가지 않는다. Apple 기준으로 기기 안에만 있는 데이터는 "수집"이 아니다. iCloud 백업도 해당하지 않는다 |
| 연락처 정보 | 회원가입·로그인이 없다. 메일 문의는 앱 밖의 메일 앱에서 이뤄진다 |
| 정확한 위치 | 위치 권한을 쓰지 않는다. 대략적인 위치는 SDK가 IP로 추정한다 |
| 구입 항목 | 앱 내 구입이 없다 |
| 사용자 ID | 앱 고유 계정이 없고 Analytics·Crashlytics에 사용자 ID를 넣지 않는다 |

## 주의

- **기기 ID의 "추적: 예"는 빼면 안 된다.** 앱이 ATT 동의 창을 띄우므로 라벨에 추적이 없으면 심사 지침 5.1.2로 거절될 수 있다.
- "개발자의 광고 또는 마케팅"은 앱이 자체 광고를 하지 않지만 AdMob 매니페스트가 선언한 목적이라 그대로 넣었다. SDK 선언과 라벨이 어긋나지 않게 하는 쪽이 안전하다.
- SDK를 올리거나 바꾸면 다시 확인한다. Xcode에서 Archive를 만든 뒤 Organizer > 해당 아카이브 우클릭 > Generate Privacy Report로 SDK 선언을 모아 볼 수 있다(Analytics는 매니페스트가 없어 이 보고서에 안 나온다).
- 사용자 ID를 Analytics·Crashlytics에 넣거나 Firebase Auth 등을 추가하면 "사용자 ID"와 연결 여부가 바뀐다.
