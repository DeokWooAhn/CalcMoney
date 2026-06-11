# CalcMoney Firebase Functions

Firebase Functions가 한국수출입은행 환율 정보 Open API를 호출하고, 앱은 Firestore에 저장된 최신 환율 캐시만 읽습니다.

## 최초 설정

```bash
firebase login
firebase use calculator-money-6ebb9
firebase functions:secrets:set KOREA_EXIM_API_KEY
firebase deploy --only firestore:rules,functions
```

`KOREA_EXIM_API_KEY`에는 한국수출입은행에서 발급받은 API 인증키를 입력합니다.

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
- `rates`: 통화별 환율 목록

## 스케줄

`syncExchangeRates` 함수는 한국 시간 기준 매일 11:10에 실행되고, `retrySyncExchangeRates` 함수가 12:30에 한 번 더 실행됩니다.
오늘 환율이 비어 있으면 직전 영업일 기준으로 최대 7개 영업일을 조회합니다.
