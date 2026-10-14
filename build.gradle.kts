import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.detekt) apply false
    // 루트가 병합 모듈이라 apply false가 아니다.
    alias(libs.plugins.kover)
}

// :app은 Hilt 조립 셸이고 테스트가 없어 병합 대상에서 뺀다.
dependencies {
    kover(project(":domain"))
    kover(project(":data"))
    kover(project(":presentation"))
}

kover {
    currentProject {
        // 하위 모듈의 같은 이름 variant(domain=jvm, data·presentation=debug)를 합친다.
        createVariant("unit") {}
    }

    // 병합 리포트에는 루트 필터만 적용된다. 하위 모듈에 필터를 두지 않는다.
    reports {
        filters {
            excludes {
                classes(
                    // Hilt·Dagger 생성 코드
                    "*_Factory",
                    "*_Factory\$*",
                    "*_HiltModules*",
                    // 패턴은 패키지까지 포함한 전체 이름과 비교된다.
                    "*.Hilt_*",
                    "hilt_aggregated_deps.*",
                    // Room 생성 코드
                    "*_Impl",
                    "*_Impl\$*",
                    // Compose 컴파일러 생성 코드
                    "*ComposableSingletons*",
                    // DI 모듈과 Room entity
                    "com.ahn.data.di.*",
                    "*.local.entity.*",
                    // 유닛 테스트 대상이 아닌 프레임워크 진입점과 테마
                    "com.ahn.presentation.main.MainActivity*",
                    "com.ahn.presentation.ads.*",
                    "com.ahn.presentation.ui.theme.*",
                )
                // 화면은 Maestro E2E가 맡는다. 유닛 커버리지는 ViewModel·reducer·유틸만 본다.
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }

        variant("unit") {
            verify {
                // 도입 시점 기준선 77.08%에서 2%p 남짓 낮춘 하한. 커버리지가 오르면 함께 올린다.
                rule {
                    minBound(75)
                }
            }
        }
    }
}

val detektToolVersion = libs.versions.detekt.get()
val detektKtlintWrapper = libs.detekt.rules.ktlint.wrapper

subprojects {
    apply(plugin = "dev.detekt")

    extensions.configure<DetektExtension>("detekt") {
        toolVersion = detektToolVersion
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        parallel = true
    }

    dependencies {
        add("detektPlugins", detektKtlintWrapper)
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget.set("17")
        reports {
            html.required.set(true)
            xml.required.set(true)
            sarif.required.set(true)
            md.required.set(false)
        }
    }

    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget.set("17")
    }
}
