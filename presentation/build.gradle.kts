import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
private val ADMOB_TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"

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
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ahn.presentation"
    compileSdk = 35

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
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
    implementation(libs.play.services.ads)

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
    debugImplementation(libs.androidx.ui.test.manifest)

    // 단위 테스트
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlin.reflect)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    // Android 계측 테스트
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

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

}
