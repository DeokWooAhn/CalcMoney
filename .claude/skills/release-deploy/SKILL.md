---
name: release-deploy
description: 릴리스, 배포, 버전 관리, CI/CD 워크플로, Firebase Functions 배포 작업 시 사용.
---

# 릴리스 / 배포 규칙

상세 문서: `docs/continuous-deployment.md` — 여기 요약과 다르면 그쪽이 정답.

## 앱 릴리스 흐름 (자동)

1. `master`에 머지
2. `v*` 태그 push (예: `v1.2.0`)
3. `.github/workflows/android-ci.yml`이 서명된 AAB 빌드 → Play **internal** 트랙 자동 업로드

- `versionCode = 10000 + GITHUB_RUN_NUMBER`, `versionName = 태그에서 v 제거` — **절대 손으로 versionCode를 올리지 말 것**
- 패키지명은 `com.ahn.calcmoney` 고정 (Play Console 등록명)
- Firebase App Distribution 배포는 `workflow_dispatch` 수동 트리거 (`distribute_debug` 입력)

## CI 파이프라인 (PR → master)

`detekt` → `test` → `lintDebug` → `assembleDebug` 순서. PR 올리기 전 로컬에서 동일하게 검증:

```bash
./gradlew detekt test :app:assembleDebug
```

## 릴리스 빌드에 필요한 시크릿 (GitHub Secrets)

- 키스토어: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
- AdMob: `ADMOB_APP_ID`, `ADMOB_EXCHANGE_BANNER_ID`, `ADMOB_FAVORITE_BANNER_ID`, `ADMOB_SETTINGS_BANNER_ID`
- Play 업로드: `PLAY_SERVICE_ACCOUNT_JSON` / Firebase: `FIREBASE_SERVICE_ACCOUNT`

이 시크릿 검증 로직(`adMobValueStrict` 등)이 `app/build.gradle.kts`·`presentation/build.gradle.kts`에 있어서, 로컬에서 Release/build/bundle 계열 태스크를 돌리면 즉시 `error()`로 실패한다.

## Firebase Functions / Firestore 배포

- `master`에 push 시 `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경이 있으면 `.github/workflows/firebase-deploy.yml`이 자동 배포 (프로젝트 `calculator-money-6ebb9`)
- Functions는 plain JS(Node 22, CommonJS), lint는 `node --check`뿐 — 문법 외 검증 없음
- 스케줄 함수 2개(`syncExchangeRates` 11:10 KST, `retrySyncExchangeRates` 12:30 KST)가 수출입은행 API를 호출해 `exchangeRates/latest` 한 문서에 씀. `status` 필드: `FRESH`/`STALE`/`ERROR`
- API 키는 Firebase secret: `firebase functions:secrets:set KOREA_EXIM_API_KEY`
- `firestore.rules`: `exchangeRates/latest`만 읽기 공개, 쓰기는 전부 거부 — 앱에서 Firestore 쓰기 코드를 추가하면 동작하지 않음
