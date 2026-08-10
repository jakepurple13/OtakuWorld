// bare id(...) — buildSrc convention plugins already apply this plugin unversioned; alias(libs.plugins.*) conflicts with that.
plugins {
    id("kotlin")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.showcase.annotations)
    implementation(libs.ksp.symbol.processing.api)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(commonLibs.runtime)
    testImplementation(libs.kotlin.compile.testing.core)
    testImplementation(libs.kotlin.compile.testing.ksp)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// :showcase:processor is a plain JVM module, so it has no KMP `allTests` task.
// CI runs `./gradlew allTests` — register an alias so these tests are not skipped.
tasks.register("allTests") { dependsOn(tasks.named("test")) }
