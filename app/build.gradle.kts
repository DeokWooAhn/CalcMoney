import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
private val DEFAULT_VERSION_CODE = 2
private val DEFAULT_VERSION_NAME = "1.0"

fun releaseVersionCode(): Int {
    val versionCode = providers.gradleProperty("versionCode").orNull
        ?: return DEFAULT_VERSION_CODE

    return versionCode.toIntOrNull()
        ?: error("versionCode must be an integer.")
}

fun releaseVersionName(): String {
    return providers.gradleProperty("versionName").orNull ?: DEFAULT_VERSION_NAME
}

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

fun releaseSigningValueStrict(name: String): String {
    val provider = providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))

    if (provider.isPresent) return provider.get()

    if (isReleaseBuildRequested()) {
        error("Missing $name. Set $name as a Gradle property or environment variable for release signing.")
    }

    return ""
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("com.google.devtools.ksp")
    alias(libs.plugins.dependency.guard)
    alias(libs.plugins.manifest.shield)
}

android {
    namespace = "com.ahn.calcmoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahn.calcmoney"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = releaseVersionCode()
        versionName = releaseVersionName()

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = releaseSigningValueStrict("RELEASE_KEYSTORE_FILE")
            if (keystoreFile.isNotBlank()) {
                storeFile = file(keystoreFile)
            }
            storePassword = releaseSigningValueStrict("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = releaseSigningValueStrict("RELEASE_KEY_ALIAS")
            keyPassword = releaseSigningValueStrict("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", ADMOB_TEST_APP_ID)
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            resValue("string", "admob_app_id", adMobValueStrict("ADMOB_APP_ID"))
            signingConfig = signingConfigs.getByName("release")
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
}

// 라이브러리 버전 한 줄만 올려도 함께 바뀌는 것을 저장소의 baseline 파일과 비교한다.
// 달라지면 검사가 실패하고, 의도한 변경이면 baseline을 다시 만들어 PR에 함께 커밋한다.
// 태스크 이름에 Release가 없어 릴리스 시크릿 검증에 걸리지 않는다.
dependencyGuard {
    // 출시 앱에 실제로 들어가는 의존성 목록. dependencies/releaseRuntimeClasspath.txt
    configuration("releaseRuntimeClasspath")
}

manifestShield {
    // Merged Manifest의 권한·exported 컴포넌트. manifestShield/releaseAndroidManifest.txt
    configuration("release") {
        // 어느 라이브러리가 넣었는지도 함께 남긴다.
        sources = true
        // minSdk가 23 이상이라 <uses-permission-sdk-23>으로 들어온 권한도 실제로 적용된다.
        usesPermissionSdk23 = true
        // 권한으로 보호된 exported 컴포넌트도 남긴다. 아무 앱이나 받을 수 있는 권한으로 보호하면 막히지 않는다.
        unprotectedOnly = false
    }
}

dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":presentation"))

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // App Check — provider 구현은 src/debug, src/release에 각각 둔다.
    // 디버그 provider가 릴리스 APK에 들어가지 않도록 빌드 타입별로 분리한다.
    debugImplementation(libs.firebase.appcheck.debug)
    releaseImplementation(libs.firebase.appcheck.playintegrity)

    implementation(libs.androidx.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
}
