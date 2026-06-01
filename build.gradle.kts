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
    alias(libs.plugins.detekt) apply false
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
