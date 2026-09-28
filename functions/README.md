# CalcMoney Firebase Functions

Firebase Functions가 [ExchangeRate-API](https://www.exchangerate-api.com/)를 호출하고, 앱은 Firestore에 저장된 최신 환율 캐시만 읽습니다.

## 최초 설정

```bash
firebase login
firebase use calculator-money-6ebb9
firebase functions:secrets:set EXCHANGE_RATE_API_KEY
firebase deploy --only firestore:rules,functions
```

`EXCHANGE_RATE_API_KEY`에는 ExchangeRate-API 대시보드에서 발급받은 API 키를 입력합니다. 무료 플랜(월 1,500회)으로 충분합니다. 함수는 하루 2번만 호출합니다.

API 키는 요청 URL 경로에 들어가므로 URL이나 원본 네트워크 오류를 로그·`lastError`에 남기지 않습니다. `lastError`는 공개 읽기 문서에 저장됩니다.

## 데이터 구조

앱은 아래 문서만 읽습니다.

```text
exchangeRates/latest
```

주요 필드:

- `rateDate`: 환율 기준일, `yyyyMMdd`
- `fetchedAt`: 서버가 환율을 마지막으로 성공 갱신한 시각, epoch millis
- `rateFetchedAt`: 서버가 환율을 마지막으로 성공 갱신한 시각, epoch millis
- `lastCheckedAt`: 서버가 환율 갱신을 마지막으로 시도한 시각, epoch millis
- `sourceName`: 데이터 출처
- `status`: `FRESH`, `STALE`, `ERROR`
- `message`: stale/error 상태 안내
- `rates`: 통화별 환율 목록. 통화 코드 알파벳순입니다. 새 앱은 화면에서 직접 정렬하지만, 구버전 앱은 이 순서를 그대로 보여주므로 순서를 바꾸지 않습니다.
  - `code`, `currencyUnit`: 통화 코드 (KRW는 앱이 직접 붙이므로 목록에서 제외)
  - `currencyName`: 한국어 통화 이름 (`Intl.DisplayNames`, ISO 4217에 없는 통화는 직접 매핑)
  - `baseRate`: 외화 1단위당 원. API는 1원당 외화로 주므로 역수로 저장합니다.

## 스케줄

`syncExchangeRates` 함수는 한국 시간 기준 매일 11:10에 실행되고, `retrySyncExchangeRates` 함수가 12:30에 한 번 더 실행됩니다.
ExchangeRate-API 무료 플랜은 하루 한 번(00:00 UTC 전후) 갱신되므로 그 뒤에 받습니다. `rateDate`는 API의 마지막 갱신 시각을 한국 날짜로 바꾼 값입니다.
갱신에 실패하면 이전 환율을 남기고 `status`를 `STALE`로 바꿉니다.

## 로컬 검증

변환 로직은 Firebase 의존성이 없는 `exchangeRates.js`에 있어 `node`로 바로 불러 확인할 수 있습니다.
