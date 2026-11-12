// Plain JVM tool: scans app sources and writes navgraph.json for the debug Navigation map screen.
// bare id(...) — buildSrc convention plugins already apply these plugins unversioned.
plugins {
    id("kotlin")
    id("kotlinx-serialization")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(commonLibs.kotlinxSerialization)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.withType<Test> {
    useJUnitPlatform()
    // RepoGraphTest scans the real repo relative to this module.
    workingDir = projectDir
}

// CI runs `./gradlew jvmTest testAndroidHostTest` — alias so these tests are not skipped.
tasks.register("jvmTest") { dependsOn(tasks.named("test")) }
