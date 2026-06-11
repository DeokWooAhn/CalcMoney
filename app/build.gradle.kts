import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

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
}

android {
    namespace = "com.ahn.calcmoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ahn.calcmoney"
        minSdk = 28
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            // Current generated kotlinx.serialization DTOs do not need extra keep rules.
            // See app/proguard-rules.pro before adding polymorphic serialization.
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

    implementation(libs.androidx.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
