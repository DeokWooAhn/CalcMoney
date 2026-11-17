private val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
private val ADMOB_TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
private val ADMOB_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

private fun isReleaseBuildRequested(): Boolean =
    gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Release", ignoreCase = true) ||
            taskName == "assemble" ||
            taskName.endsWith(":assemble") ||
            taskName == "bundle" ||
            taskName.endsWith(":bundle") ||
            taskName == "build" ||
            taskName.endsWith(":build")
    }

fun adMobValueStrict(name: String): String {
    val provider = providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))

    if (provider.isPresent) return provider.get()

    if (isReleaseBuildRequested()) {
        error("Missing $name. Set $name as a Gradle property or environment variable for release builds.")
    }

    return ""
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kover)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ahn.presentation"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", ADMOB_TEST_APP_ID)
            resValue("string", "admob_exchange_banner_id", ADMOB_TEST_BANNER_ID)
            resValue("string", "admob_favorite_banner_id", ADMOB_TEST_BANNER_ID)
            resValue("string", "admob_settings_banner_id", ADMOB_TEST_BANNER_ID)
            resValue("string", "admob_calculator_interstitial_id", ADMOB_TEST_INTERSTITIAL_ID)
        }
        release {
            isMinifyEnabled = false
            resValue(
                "string",
                "admob_exchange_banner_id",
                adMobValueStrict("ADMOB_EXCHANGE_BANNER_ID"),
            )
            resValue(
                "string",
                "admob_favorite_banner_id",
                adMobValueStrict("ADMOB_FAVORITE_BANNER_ID"),
            )
            resValue(
                "string",
                "admob_settings_banner_id",
                adMobValueStrict("ADMOB_SETTINGS_BANNER_ID"),
            )
            resValue(
                "string",
                "admob_calculator_interstitial_id",
                adMobValueStrict("ADMOB_CALCULATOR_INTERSTITIAL_ID"),
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // AGP 9부터 기본값이 false다. AdMob 광고 단위 ID를 resValue로 넣는다.
        resValues = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

// debug만 묶는다. release variant 태스크는 이름에 Release가 들어가 AdMob 시크릿 검증에 걸린다.
kover {
    currentProject {
        // Robolectric이 화면 파일을 로드하면 루트 annotatedBy(Composable)이 람다 본문을 빼지 못해 수치가 떨어진다.
        // 루트 리포트 필터도 로드된 클래스에는 듣지 않았다(2026-11-17 측정, docs/decisions/compose-ui-tests-robolectric.md).
        // UI 전용 파일은 계측에서 뺀다. Compose UI 테스트로 새 UI 파일을 다루면 여기에 더한다.
        instrumentation {
            excludedClasses.addAll(
                "com.ahn.presentation.*ScreenKt*",
                "com.ahn.presentation.ui.component.LoadFailureMessageKt*",
                "com.ahn.presentation.ui.component.AdMobBannerKt*",
                "com.ahn.presentation.ui.component.CustomSnackbarKt*",
            )
        }
        createVariant("unit") {
            add("debug")
        }
    }
}

dependencies {
    implementation(project(":domain"))

    // AndroidX 코어 및 UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.material)

    // Compose BOM 및 UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // 디버그
    debugImplementation(libs.androidx.ui.tooling)

    // 단위 테스트
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Compose UI 테스트: Robolectric으로 JVM에서 실행한다.
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    // 테스트용 ComponentActivity를 manifest에 넣는다. testImplementation이라 앱 디버그 APK에는 섞이지 않는다.
    testImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.robolectric)
    // JUnit4 테스트를 기존 JUnit5 플랫폼에서 실행한다.
    testRuntimeOnly(libs.junit.vintage.engine)

    // Orbit MVI
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)  // Compose 연동
    testImplementation(libs.orbit.test)  // 테스트

    // 내비게이션
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // 의존성 주입
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // 광고
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // 인앱 리뷰 (Activity가 필요해 광고·동의처럼 presentation에 둔다)
    implementation(libs.play.review.ktx)
}
