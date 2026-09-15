# AGP 9 업그레이드 계획

조사 기준일: 2026-09-15. AGP 9.4.0 stable, Gradle 9.7.1, 프로젝트 AGP 8.12.1 / Gradle 8.13 시점.

이 문서는 Android 쪽만 다룬다. iOS(`ios/`)는 SPM이라 영향이 없다.

## 결론

**지금 착수하지 않는다.** 착수 조건은 하나다 — **`dev.detekt`가 2.0.0 정식으로 올라오는 것**. 2026-09-15 기준 `2.0.0-alpha.6`까지만 나와 있고, 이 프로젝트는 `2.0.0-alpha.0`이다.

AGP 9를 미루는 것이 손해가 아닌 이유는, **AGP 9가 주는 최적화를 이미 받고 있기 때문**이다. `android.r8.optimizedResourceShrinking`과 `-repackageclasses` 두 플래그를 8.12.1에서 직접 켜서 release APK를 11.5MB에서 10.2MB로(-11.35%) 줄였다. AGP 9.0/9.1은 이 두 줄을 기본값으로 만들어 줄 뿐 결과물을 더 줄여주지는 않는다.

즉 AGP 9는 **이 두 줄을 지우기 위한 작업**이지, 최적화를 얻기 위한 작업이 아니다.

## 현재 상태 대조

최소 버전은 [AGP 9.0.0 릴리즈 노트](https://developer.android.com/build/releases/agp-9-0-0-release-notes)의 Compatibility 표와 Google `agp-9-upgrade` 스킬을 따랐다.

| 항목 | AGP 9.0 요구 | CalcMoney 현재 | 판정 |
| --- | --- | --- | --- |
| AGP | 9.0.0 (권장 9.0.1+) | 8.12.1 | 상향 대상. 8.x 최신은 8.13.2 |
| **Gradle** | **9.1.0** | **8.13** | 메이저 업그레이드 필요 |
| **KGP** | **2.2.10** | **2.2.0** | 미달. AGP 9가 강제로 끌어올린다 |
| **KSP** | **2.3.6+** | 2.2.0-2.0.2 | 상향 필요. 버전 표기 체계 변경 |
| **Hilt** | **2.59.2+** | 2.57.1 | 상향 필요 |
| JDK | 17 | 17 | 충족 |
| SDK Build Tools | 36.0.0 | compileSdk 36 | 충족 |
| Room | 2.8.0+ | 2.8.4 | 충족 |
| 최대 API 레벨 | 36.1 | compileSdk 36 | 충족 |
| kapt 미사용 | 필수 | KSP만 사용 | 충족 |
| 레거시 variant API 미사용 | 필수 | 사용 없음 | 충족 |
| **detekt** | — | `dev.detekt` 2.0.0-alpha.0 | **호환 여부 미확인** |

KGP가 요구치보다 낮다는 점이 중요하다. AGP 9.0은 built-in Kotlin을 위해 KGP 2.2.10에 런타임 의존하므로, **Kotlin 버전이 의도와 무관하게 올라간다.** 2.2.0에 묶어 두려면 built-in Kotlin을 옵트아웃해야 하는데, 그러면 AGP 9로 올릴 이유가 없어진다.

KSP는 2.3.0부터 Kotlin 컴파일러 버전과 분리됐다. `2.2.0-2.0.2` 형태에서 `2.3.12`처럼 접두사 없는 단일 버전으로 바뀐다.

## 이미 준비된 부분

업그레이드 난이도를 낮추는 요소들이다. 착수할 때 다시 조사하지 않아도 된다.

- **kapt이 없다.** 전 모듈 KSP다. AGP 9는 `org.jetbrains.kotlin.kapt`과 호환되지 않으므로 이게 없는 것이 가장 크다.
- **레거시 variant API를 쓰지 않는다.** `applicationVariants`, `libraryVariants`, `variantFilter`, `dexOptions` 사용처가 없다.
- **keep 규칙이 거의 없다.** `app/proguard-rules.pro`에 `-keepattributes` 한 건뿐이고 `consumer-rules.pro` 둘은 비어 있다. 패키지 와일드카드 0개, 멤버 미지정 `-keep class` 0개. AGP 9의 `strictFullModeForKeepRules` 기본값 변경(`-keep class A`가 기본 생성자를 암묵적으로 남기지 않음) 영향을 받지 않는다.
- **`getDefaultProguardFile("proguard-android-optimize.txt")`** 를 쓴다. AGP 9가 막는 `proguard-android.txt`가 아니다.
- **전역 옵션이 없다.** `-dontoptimize`, `-dontobfuscate` 등이 없어 `globalOptionsInConsumerRules.disallowed` 변경과 무관하다.
- **namespace가 모듈별로 유일**하다(`com.ahn.calcmoney`, `com.ahn.data`, `com.ahn.presentation`).
- **`targetSdk = 36`이 명시**돼 있어 기본값이 `compileSdk`로 바뀌는 변경 영향이 없다.
- **Java source/target이 17로 명시**돼 있어 기본값 8→11 변경 영향이 없다.
- **Java 소스가 없다.** R class가 compile-time non-final로 바뀌어도 Java `switch` 상수 문제가 없다.
- **shader, NDK, density split, ABI filter 사용처가 없다.**
- **`android.enableR8.fullMode=false`가 없다.** full mode가 이미 켜져 있다.
- **CI가 JDK 17**(`temurin`)을 쓴다. AGP 9 요구치를 이미 만족한다.

## 블로커

### 1. detekt — 판정 자체가 없다

JetBrains 호환성 표는 구 좌표 `io.gitlab.arturbosch.detekt` (< 2.0.0)에 대해 `android.newDsl=false` + `android.builtInKotlin=false`를 요구한다. 이 프로젝트는 신 좌표 **`dev.detekt` 2.0.0-alpha.0**이라 표의 어느 행에도 해당하지 않는다.

두 플래그는 AGP 9의 핵심 기능 둘을 끄는 플래그다. detekt 때문에 둘 다 끄면 AGP 9로 올려도 얻는 게 없고, `android.newDsl=false` 옵트아웃은 **AGP 10.0에서 제거**된다.

영향 범위가 lint 태스크에 그치지 않는다. 루트 `build.gradle.kts`가 `dev.detekt.gradle.Detekt`, `DetektCreateBaselineTask`, `extensions.DetektExtension` 타입을 **직접 import해 `subprojects {}`로 전 모듈에 적용**한다. detekt의 API가 바뀌면 **루트 빌드 스크립트 자체가 컴파일되지 않아** 어떤 Gradle 태스크도 실행되지 않는다. `android-ci.yml`은 `detekt`가 첫 스텝이라 CI 전체가 거기서 멈춘다.

**정식 릴리스를 기다리는 것이 1순위 선행 조건이다.** 여기서 막히면 나머지 단계는 의미가 없다.

### 2. Gradle 8.13 → 9.1+ 는 별도의 메이저 업그레이드

AGP 9.0은 Gradle 9.1.0 미만에서 동작하지 않는다. Gradle 9는 그 자체로 별도의 breaking change 묶음을 갖는다. AGP 업그레이드와 같은 커밋에 섞지 않고 먼저 단독으로 올려 통과시킨다.

### 3. built-in Kotlin 마이그레이션

AGP 9.0은 built-in Kotlin을 기본으로 켠다. `org.jetbrains.kotlin.android`는 신 DSL과 호환되지 않으므로 Android 모듈 3개에서 `alias(libs.plugins.jetbrains.kotlin.android)`을 걷어내야 한다.

| 모듈 | 플러그인 | 조치 |
| --- | --- | --- |
| `:app` | `android.application` + `kotlin.android` | kotlin.android 제거 |
| `:data` | `android.library` + `kotlin.android` | kotlin.android 제거 |
| `:presentation` | `android.library` + `kotlin.android` | kotlin.android 제거 |
| `:domain` | `java-library` + `kotlin.jvm` | 해당 없음 |

`:domain`은 순수 JVM 모듈이라 built-in Kotlin 대상이 아니다.

### 4. `resValue` — 이 프로젝트 고유 항목

AGP 9는 `android.defaults.buildfeatures.resvalues`를 `true → false`로 바꾼다. 이 프로젝트는 **AdMob 단위 ID를 전부 `resValue`로 주입**한다.

| 모듈 | `resValue` 개수 | 내용 |
| --- | --- | --- |
| `:app` | 2 | `admob_app_id` (debug/release) |
| `:presentation` | 7 | `admob_app_id`, 배너 3종 ID (debug/release) |

두 모듈에 아래를 명시하지 않으면 **광고 ID 리소스가 생성되지 않는다.** 현재 두 모듈의 `buildFeatures` 블록에는 `compose = true`만 있다.

```kotlin
buildFeatures {
    compose = true
    resValues = true
}
```

빌드는 통과하고 **런타임에 광고만 안 나오는** 형태로 실패하므로 놓치기 쉽다.

### 5. 호환성 미확인 플러그인

호환성 표에 행이 없는 플러그인들이다. 착수 시 각각 확인한다.

- `com.google.gms.google-services` 4.4.4
- `com.google.firebase.crashlytics` 3.0.7
- `org.jetbrains.kotlin.plugin.compose` (KGP에 종속되므로 KGP가 올라가면 따라간다)

## R8 플래그 제거

AGP 9로 올리면 아래 두 줄을 지운다. 지우는 시점이 다르다.

| 위치 | 내용 | 제거 시점 |
| --- | --- | --- |
| `gradle.properties` | `android.r8.optimizedResourceShrinking=true` | **AGP 9.0** |
| `app/proguard-rules.pro` | `-repackageclasses` | **AGP 9.1** |

AGP 9.0에서 둘 다 지우면 리패키징이 꺼져 DEX가 다시 커진다. 각 파일의 주석에 제거 시점을 적어 두었다.

## 실행 순서

각 단계를 별도 커밋으로 나눈다. 한 단계라도 실패하면 거기서 멈추고 원인을 정리한 뒤 다음으로 넘어간다. 각 단계의 검증은 CI와 동일하게 `detekt` → `test` → `lintDebug` → `assembleDebug` 순으로 돌린다.

| # | 작업 | 비고 |
| --- | --- | --- |
| 1 | `dev.detekt` 2.0.0 정식 릴리스 확인 및 상향. **여기서 막히면 중단** | 루트 빌드 스크립트의 import 3개가 살아 있는지 함께 확인 |
| 2 | AGP 8.12.1 → 8.13.2 | 8.x 안에서 먼저 최신화 |
| 3 | Gradle 8.13 → 9.1+ 단독 업그레이드 | |
| 4 | KSP 2.2.0-2.0.2 → 2.3.6+ | 버전 표기 체계가 바뀐다 |
| 5 | Hilt 2.57.1 → 2.59.2+ | |
| 6 | `:app`·`:presentation`에 `resValues = true` 명시 | AGP 9 이전에 넣어도 무해하다 |
| 7 | AGP 8.13.2 → 9.0.1. Android Studio **AGP Upgrade Assistant** 사용 | |
| 8 | built-in Kotlin 마이그레이션 (Android 모듈 3개에서 `kotlin.android` 제거) | |
| 9 | `gradle.properties`에서 `optimizedResourceShrinking` 제거 | AGP 9.0부터 기본값 |
| 10 | 서명 release 빌드로 크기·광고 검증 | 아래 체크리스트 |
| 11 | (AGP 9.1 이후) `-repackageclasses` 제거 | |

착수 시 Google `agp-9-upgrade` 스킬을 설치해 쓴다.

```bash
android skills add agp-9-upgrade --project .
```

### 옵트아웃 플래그

서드파티 플러그인이 막으면 아래를 `gradle.properties`에 **임시로** 넣는다. 임시라는 주석을 반드시 남기고, 원인이 풀리면 제거한다. `android.newDsl=false`는 AGP 10.0에서 제거되므로 영구 해결책이 아니다.

```properties
# 임시: <플러그인명>이 AGP 9 신 DSL 미지원. <조건> 충족 시 제거
android.newDsl=false
android.builtInKotlin=false
```

`android.disallowKotlinSourceSets=false`는 넣지 않는다. Google 스킬이 명시적으로 금지한다.

### 검증 체크리스트 (단계 10)

로컬 release 빌드는 `isReleaseBuildRequested()`의 시크릿 강제 검증 때문에 막힌다(`app/build.gradle.kts`). 실기기 검증은 **CI가 만든 서명 빌드**로 한다.

- AdMob 배너 3종 노출: Exchange / Favorite / Settings
- UMP 동의 플로우 (최초 실행, 동의·거부·설정에서 재호출)
- 환율 조회와 Room 캐시 (12h TTL 만료 후 갱신 포함)
- 계산기 수식 환산, 즐겨찾기 등록·해제
- 라이트·다크 테마
- Logcat에 `ClassNotFoundException`, `NoSuchMethodException`, `Resources$NotFoundException` 없음
- AAB 크기를 직전 릴리스와 비교

`resValues`와 리소스 축소 최적화가 얽히는 지점이라 **광고 ID 리소스가 실제로 살아 있는지**가 핵심이다.

## 롤백

단계별 커밋이므로 문제 단계만 되돌린다. 단계 7 이후 문제가 발견되면, AGP만 8.13.2로 내리는 것은 Gradle 9와 조합이 검증되지 않았으므로 단계 3까지 함께 되돌린다.

## 참고

- [AGP 9.0.0 릴리즈 노트](https://developer.android.com/build/releases/agp-9-0-0-release-notes) — breaking change와 Gradle 프로퍼티 기본값 변경 전체 목록
- [built-in Kotlin 마이그레이션 가이드](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [AGP Upgrade Assistant](https://developer.android.com/build/agp-upgrade-assistant)
- Android Studio **Otter 3 (2025.2.3) 이상** 필요. IntelliJ IDEA는 2026.1 기준 AGP 9.0 미지원이다.
