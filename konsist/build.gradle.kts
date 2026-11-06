// 구조 규칙 검사 전용 모듈. 앱에 들어가지 않고 테스트만 있다.
// Konsist가 다른 모듈의 소스 파일을 직접 읽어 ViewModel·Contract·UseCase·Repository의 모양을 확인한다.
plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

// 이 모듈은 다른 모듈에 의존하지 않는다. 읽는 소스를 입력으로 잡아야 소스만 바뀌었을 때도 Gradle이 테스트를 다시 돌린다.
val checkedSources = listOf("app", "data", "domain", "presentation").map { module ->
    fileTree(rootDir.resolve("$module/src")) {
        include("**/*.kt")
        exclude("test/**", "androidTest/**")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    inputs.files(checkedSources)
        .withPropertyName("checkedSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
