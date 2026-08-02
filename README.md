# CalcMoney

한국수출입은행 환율을 바탕으로 금액을 빠르게 환산하는 Android 앱입니다. 계산기 수식 환산, 통화 변환, 즐겨찾기 통화 비교를 지원합니다.

## 주요 기능

- 계산기 수식 입력 및 환율 환산
- 기준 통화와 대상 통화 간 실시간 환율 변환
- 자주 쓰는 통화를 즐겨찾기에 등록해 한눈에 비교
- 환율 정보의 Room 로컬 캐시 및 만료 후 갱신
- 시스템 설정을 따르는 라이트·다크 테마
- AdMob 광고 동의 관리(UMP) 및 배너 광고

## 기술 구성

| 영역 | 사용 기술 |
| --- | --- |
| UI | Kotlin, Jetpack Compose, Material 3 |
| 상태 관리 | Orbit MVI |
| 의존성 주입 | Hilt, KSP |
| 로컬 저장소 | Room, DataStore |
| 원격 데이터 | Firebase Firestore, Cloud Functions |
| 분석·안정성 | Firebase Analytics, Crashlytics |
| 테스트 | Kotest, MockK, Orbit Test |
| 품질 관리 | Detekt, Android Lint, GitHub Actions |

## 아키텍처

Clean Architecture 기반의 멀티 모듈 프로젝트입니다.

```text
:app          → :data, :domain, :presentation  # 앱 조립 및 DI 진입점
:data         → :domain                         # Room, Firestore, DataStore, Repository 구현
:presentation → :domain                         # Compose UI 및 Orbit MVI
:domain                                        # 순수 Kotlin: 모델, 인터페이스, UseCase
```

환율은 앱이 외부 API를 직접 호출하지 않고 Firestore의 `exchangeRates/latest` 문서에서 읽습니다. Cloud Functions가 매일 한국수출입은행 API에서 환율을 받아 해당 문서를 갱신하며, 앱은 이를 Room에 12시간 TTL로 캐시합니다.

## 시작하기

### 요구 사항

- Android Studio 최신 안정 버전
- JDK 17
- Android SDK 36
- Android 9(API 28) 이상 기기 또는 에뮬레이터

### 로컬 설정

1. 저장소를 복제합니다.

   ```bash
   git clone https://github.com/DeokWooAhn/CalcMoney.git
   cd CalcMoney
   ```

2. Android SDK 경로를 `local.properties`에 설정합니다.

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

3. 디버그 앱을 빌드하거나 Android Studio에서 실행합니다.

   ```bash
   ./gradlew :app:assembleDebug
   ```

디버그 빌드는 Google 제공 AdMob 테스트 ID를 사용합니다. 릴리스용 키스토어와 광고 ID는 저장소에 넣지 않습니다.

## 검증

```bash
# 정적 분석
./gradlew detekt

# 전체 단위 테스트
./gradlew test

# 디버그 APK 빌드
./gradlew :app:assembleDebug
```

모듈 단위 검증도 가능합니다.

```bash
./gradlew :domain:test
./gradlew :presentation:detekt
./gradlew :data:connectedDebugAndroidTest  # 에뮬레이터 또는 기기 필요
```

`build`, `bundleRelease` 같은 릴리스 태스크는 서명·AdMob 환경 변수가 필요합니다. 일반적인 로컬 검증에는 위의 디버그 빌드와 테스트 명령을 사용하세요.

## CI/CD와 배포

GitHub Actions는 `master` 대상 PR과 `master` 푸시에서 정적 분석, 단위 테스트, Android Lint, 디버그 APK 빌드를 실행합니다.

Google Play 내부 테스트 배포는 `master`에 병합된 커밋에 `v*` 태그를 푸시하면 자동으로 진행됩니다.

```bash
git tag v1.0.1
git push origin v1.0.1
```

- 태그에서 `v`를 뺀 값이 앱의 `versionName`이 됩니다.
- `versionCode`는 GitHub Actions 실행 번호를 기준으로 자동 증가합니다.
- 배포 전 GitHub Actions Secrets에 키스토어, AdMob, Google Play 서비스 계정 값을 등록해야 합니다.

Firebase Functions와 Firestore 규칙은 `functions/**`, `firebase.json`, `firestore.rules`, `.firebaserc` 변경이 `master`에 반영되면 자동 배포됩니다.

자세한 시크릿 목록과 배포 절차는 [지속적 배포 문서](docs/continuous-deployment.md)를 참고하세요.

## 프로젝트 문서

- [에이전트 작업 지침](AGENTS.md)
- [프로젝트 개요와 개발 메모](CLAUDE.md)
- [지속적 배포 안내](docs/continuous-deployment.md)

## 라이선스

라이선스는 아직 정의되지 않았습니다.
