plugins {
    id("org.jetbrains.kotlin.jvm")
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
