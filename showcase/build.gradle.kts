plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(projects.showcase.annotations)
    ksp(projects.showcase.processor)

    implementation(commonLibs.compose.material3)
    implementation(commonLibs.material.icons.extended)
    implementation(commonLibs.runtime)
    implementation(commonLibs.ui)
    implementation(commonLibs.foundation)
    implementation(commonLibs.cmp.ui.util)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.showcase.MainKt"
    }
}
