# CalcMoney (심플 환율 계산기)

한국수출입은행 환율을 보여주는 안드로이드 앱. Cloud Functions가 매일 환율을 받아 Firestore `exchangeRates/latest` 문서 하나에 쓰고, 앱은 그 문서를 읽어 Room에 캐시(12h TTL)한다. Retrofit 없음 — Firestore가 곧 네트워크 레이어.

## 모듈 구조 (Clean Architecture)

```
:app  ──→ :data, :domain, :presentation   Hilt 조립 전용 셸 (Kotlin 파일 1개)
:data ──→ :domain                          Repository 구현, Room, Firestore, DataStore
:presentation ──→ :domain                  Compose UI + Orbit MVI (:data를 모름)
:domain                                    순수 Kotlin JVM. model / repository 인터페이스 / usecase
```

- 화면 4개: Calculator / Exchange / Favorite / Settings — `presentation/src/main/java/com/ahn/presentation/main/Route.kt`
- MainActivity는 `:app`이 아니라 `:presentation`에 있음
- Settings·Favorite 화면은 activity-scoped `ExchangeViewModel`을 공유함 (`MainNavGraph.kt`의 `hiltViewModel(sharedOwner)`) — 이 owner를 바꾸면 화면 간 상태 공유가 조용히 깨진다

## 기술 스택

Kotlin 2.2 / JVM 17 / compileSdk 36 · Compose(Material3) · Hilt+KSP(kapt 없음) · Orbit MVI · Room(스키마 `data/schemas/`) · DataStore(Qualifier로 분리된 4개 스토어) · Firebase(Analytics·Crashlytics·Firestore) · AdMob+UMP · Kotest+MockK

## 자주 쓰는 명령

```bash
./gradlew detekt            # 린트 (ktlint 포함, autoCorrect 켜짐)
./gradlew test              # 전체 유닛 테스트
./gradlew :app:assembleDebug
```

## 주의사항 (Gotchas)

- **`./gradlew build` 는 로컬에서 실패한다.** 태스크 이름에 Release/assemble/bundle/build가 들어가면 AdMob·키스토어 시크릿 8개를 강제 검증(`error()`)하기 때문. 항상 `assembleDebug` / `test` / `detekt`를 쓸 것.
- **versionCode를 손으로 고치지 말 것.** 릴리스는 `v*` 태그 push → CI가 `10000 + GITHUB_RUN_NUMBER`로 계산. 로컬 기본값은 `app/build.gradle.kts`의 `DEFAULT_VERSION_CODE`.
- 기본 브랜치는 `main`이 아니라 **`master`**.
- detekt는 `dev.detekt` 2.0 알파 (구 `io.gitlab.arturbosch.detekt` 아님). 모듈별 `detekt-baseline.xml` 존재.
- 테스트는 JUnit5 플랫폼(`useJUnitPlatform()`) 위의 Kotest — JUnit4 러너로 돌리면 안 됨.
- 앱은 환율 API를 직접 호출하지 않는다. 수출입은행 키는 Cloud Functions의 Firebase secret `KOREA_EXIM_API_KEY` 하나뿐 (`local.properties`에는 `sdk.dir`만 있으면 됨).
- 주석·KDoc·테스트 설명·커밋 메시지는 **한국어**, 코드 식별자·로그는 영어.

## 세부 규칙 (필요할 때 해당 스킬 참조)

| 작업 | 스킬 |
|---|---|
| 새 화면/ViewModel 추가, MVI 패턴 | `.claude/skills/mvi-screen/SKILL.md` |
| 테스트 작성 | `.claude/skills/testing/SKILL.md` |
| 릴리스·배포·CI | `.claude/skills/release-deploy/SKILL.md` |
