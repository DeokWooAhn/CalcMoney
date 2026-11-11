# AGP 9 업그레이드

2026-09-15에 조사해 "detekt 2.0.0 정식 전에는 착수하지 않는다"로 미뤘다. 2026-11-11에 alpha.6으로 진행했다. 이 문서는 그 결정과 남은 확인을 기록한다.

이 문서는 Android 쪽만 다룬다. iOS(`ios/`)는 SPM이라 영향이 없다.

## 결론

AGP 8.12.1 → **9.5.1**로 올렸고 built-in Kotlin으로 옮겼다. **release 빌드 확인(아래 체크리스트)만 남았다.** 로컬 release 빌드는 시크릿 강제 검증 때문에 막혀 있어(`app/build.gradle.kts`의 `isReleaseBuildRequested()`) 다음 `android-v*` 태그에서 CI가 만든 서명 빌드로 확인한다.

### 착수 조건을 바꾼 이유

처음 조건은 `dev.detekt` 2.0.0 정식 출시였다. 이유는 detekt 신 좌표가 AGP 9와 맞는지 **판정 자체가 없다**는 것이었다. 2026-11-11 기준 정식은 아직 없지만 판정은 생겼다.

- `2.0.0-alpha.3`부터 AGP 9 built-in Kotlin을 지원한다. `android.newDsl=false`·`android.builtInKotlin=false` 옵트아웃이 필요 없다.
- 이 저장소는 원래도 알파(`2.0.0-alpha.0`)를 썼다. 알파에서 알파로 올리는 것이라 위험이 새로 생기지 않는다.
- alpha.6으로 실제로 돌려 소스 변경 없이 통과했다(아래 "detekt 설정").

## 바뀐 버전

| 항목 | 전 | 후 | 이유 |
| --- | --- | --- | --- |
| AGP | 8.12.1 | 9.5.1 | 2026-11-11 최신 stable |
| Gradle | 8.13 | 9.9.0 | AGP 9.5 하한 9.6.0. wrapper jar는 9.9.0 공식 SHA-256과 같다 |
| Kotlin(KGP·Compose 컴파일러) | 2.2.0 | 2.2.21 | AGP 9가 KGP 2.2.10에 런타임 의존한다. 2.2 안의 마지막 패치 |
| KSP | 2.2.0-2.0.2 | 2.3.13 | built-in Kotlin 인식은 2.3.6부터. 2.3.0부터 Kotlin 버전과 따로 간다 |
| Hilt | 2.57.1 | 2.59.2 | AGP 9 지원은 2.59, 느린 증분 빌드 수정은 2.59.2. 2.60은 Kotlin 2.3.21로 올라가 피했다 |
| detekt | 2.0.0-alpha.0 | 2.0.0-alpha.6 | AGP 9 built-in Kotlin 지원은 alpha.3부터 |

그대로 둔 것: Kover 0.9.11(AGP 9 지원), google-services 4.4.4, Crashlytics 플러그인 3.0.7, dependency-guard 0.5.0, manifest-shield 0.1.11. 모두 그대로 동작했다.

**Android Studio는 Rabbit 2(2026.2.2) 이상이 필요하다.** [호환성 표](https://developer.android.com/studio/releases)에서 AGP 9.5를 여는 첫 버전이다. Panda 4(2025.3.4)는 AGP 9.2까지만 연다. Rabbit의 기본 Gradle JDK는 25인데, Gradle 9.9는 JDK 25에서 돌아 그대로 쓴다. 다른 프로젝트가 Gradle 8.x면 그 프로젝트만 Gradle JDK를 21로 고른다. "Migrate to Gradle Daemon toolchain" 알림은 무시한다. 받아들이면 `gradle/gradle-daemon-jvm.properties`가 생겨 CI의 JDK 17보다 우선하므로, 하려면 17로 고정하는 별도 PR로 한다.

## 바꾼 설정

- **built-in Kotlin.** `:app`·`:data`·`:presentation`에서 `org.jetbrains.kotlin.android` 플러그인과 `kotlin { compilerOptions { jvmTarget } }`를 지웠다. jvmTarget은 `compileOptions.targetCompatibility`(17)를 따른다. `:domain`·`:konsist`는 순수 JVM 모듈이라 해당 없다.
- **`resValues = true`.** AGP 9부터 기본값이 `false`다. `:app`(`admob_app_id`)과 `:presentation`(배너 3종·전면 광고 ID)이 `resValue`로 AdMob ID를 넣으므로 두 모듈에서 켰다. 안 켜면 빌드는 통과하고 광고만 안 나온다. 디버그 APK에 `admob_*` 문자열 5개가 있는 것을 `aapt2 dump resources`로 확인했다.
- **R8 플래그 제거.** `gradle.properties`의 `android.r8.optimizedResourceShrinking=true`는 AGP 9.0부터, `app/proguard-rules.pro`의 `-repackageclasses`는 AGP 9.1부터 기본값이다.
- **유닛 테스트는 debug만 돈다.** `android.onlyEnableUnitTestForTheTestedBuildType`가 기본으로 켜져 `testReleaseUnitTest`가 없어졌다. 같은 테스트를 두 번 돌리던 것이 빠진 것이고, Kover는 원래 debug만 본다.

### detekt 설정

alpha.0 → alpha.6에서 바뀐 것을 `build.gradle.kts`와 `config/detekt/detekt.yml`에서 맞췄다. **목표는 업그레이드만으로 검사 기준이 달라지지 않는 것**이다. 기준을 바꾸려면 별도 PR에서 코드와 함께 고친다.

- 리포트 이름: `xml` → `checkstyle`(파일 확장자는 그대로 `.xml`), `md` → `markdown`
- `code_style: android_studio` → `intellij_idea`. alpha.0은 이 값을 줄 길이·trailing comma 기본값에 반영하지 않아, 그동안 실제로는 120자·trailing comma 허용으로 검사됐다. alpha.6은 `android_studio`를 100자·trailing comma 금지로 적용해 1,673건이 실패했다. 실제로 쓰던 기준에 맞춘다.
- `MaxLineLength` → `MaximumLineLength`. `MaxLineLength`는 별칭이라 그 아래 `maxLineLength: 120`이 적용되지 않았다.
- 기본 활성 상태가 바뀐 ktlint 규칙 14개(alpha.2가 ktlint 1.8 표준 규칙에 맞춰 5개를 켜고 9개를 껐다)는 처음에 alpha.0 상태로 고정했다. 후속 PR에서 고정을 풀고 새 기본값으로 옮겼다. 새로 켜진 `BlankLineBetweenWhenConditions`가 낸 22곳(`when` 조건 사이 빈 줄)을 고쳤다.
- 같은 PR에서 모듈 3개의 `detekt-baseline.xml`을 비웠다. 약 600건 가운데 실제로 남아 있던 위반은 테스트 파일의 빈 줄 1건뿐이었다. alpha.0에서도 baseline을 비우고 돌리면 그 1건만 나왔으므로, 나머지는 이미 고쳐졌거나 꺼 둔 규칙의 죽은 항목이었다. 그 1건도 고쳐 baseline에 남은 항목이 없다.

## 로컬에서 확인한 것

| 검사 | 결과 |
| --- | --- |
| `detekt` | 통과. 소스 파일 변경 없음 |
| `test` | 디버그 유닛 189개, JVM 96개. 전과 같은 수 |
| `:koverVerifyUnit` | 통과. 78.02% → 77.89%. 클래스별로 비교하면 Kotlin 2.2.21이 줄 번호를 한두 줄씩 다르게 매긴 차이(놓친 줄 +2, 덮은 줄 −3)이고 빠진 테스트는 없다 |
| `lintDebug` 3모듈, `:app:assembleDebug` | 통과 |
| `:app:dependencyGuard` | Hilt 2.59.2와 kotlin-stdlib 2.2.21만 바뀌어 baseline을 다시 만들었다 |
| `:app:manifestShield` | 변화 없음. 권한·exported 컴포넌트가 그대로다 |
| Maestro (에뮬레이터 Pixel 9 Pro, Android 16) | smoke 3개 + release 2개 통과. 앱 프로세스에 크래시·`ClassNotFoundException` 없음 |

## 남은 확인: release 빌드

R8 동작이 AGP 9.0·9.1에서 바뀌었다. 이 저장소는 영향을 받지 않을 것으로 본다. `-keep class` 규칙이 없고 전역 옵션도 없기 때문이다. 다만 release에서만 드러나므로 다음 태그의 서명 빌드로 확인한다. 바뀐 동작은 다음과 같다.

- `android.r8.strictFullModeForKeepRules`가 켜져, `-keep class A`가 기본 생성자를 더 이상 암묵적으로 남기지 않는다.
- R8이 클래스를 기본 패키지로 리패키징한다.
- R8이 코드와 리소스를 함께 보고 축소한다(optimized resource shrinking).

내부 테스트판은 실제 광고가 뜨므로 Maestro를 돌리지 않고 손으로 확인한다.

- AdMob 배너 3종 노출: Exchange / Favorite / Settings, 계산기 전면 광고
- UMP 동의 흐름(최초 실행, 동의·거부, 설정에서 다시 열기)
- 환율 조회와 Room 캐시(12h TTL 만료 뒤 갱신 포함)
- 계산기 수식 환산, 즐겨찾기 등록·해제
- 라이트·다크 테마
- Logcat에 `ClassNotFoundException`, `NoSuchMethodException`, `Resources$NotFoundException`이 없음
- AAB 크기를 직전 릴리스와 비교. 8.12.1에서 두 R8 플래그를 직접 켜 둔 상태와 같아야 한다

## 롤백

이 PR 하나를 되돌린다. AGP만 8.x로 내리는 것은 Gradle 9.9와 조합이 검증되지 않았으므로 Gradle·Kotlin·KSP·Hilt·detekt를 함께 되돌린다.

## 참고

- [AGP 9.0.0 릴리스 노트](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — Gradle 프로퍼티 기본값 변경 전체 목록
- [AGP 9.1.0 릴리스 노트](https://developer.android.com/build/releases/agp-9-1-0-release-notes) — R8 기본 리패키징
- [built-in Kotlin 마이그레이션 가이드](https://developer.android.com/build/migrate-to-built-in-kotlin)
- Google [`agp-9-upgrade` 스킬](https://github.com/android/skills/tree/main/build-system/agp/agp-9-upgrade) (Apache-2.0) — KSP 2.3.6+, Hilt 2.59.2+ 요구치
- detekt [2.0.0-alpha.3 릴리스 노트](https://github.com/detekt/detekt/releases/tag/v2.0.0-alpha.3) — AGP 9 built-in Kotlin 지원
