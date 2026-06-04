import org.jetbrains.kotlin.gradle.dsl.JvmTarget

private val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"

fun adMobValue(name: String, fallback: String): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(fallback)
        .get()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ahn.calcmoney"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ahn.calcmoney"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
            resValue("string", "admob_app_id", adMobValue("ADMOB_APP_ID", ADMOB_TEST_APP_ID))
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
