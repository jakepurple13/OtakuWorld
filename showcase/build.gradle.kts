// bare id(...) — buildSrc convention plugins already apply this plugin unversioned; alias(libs.plugins.*) conflicts with that.
plugins {
    id("kotlin")
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
    implementation(compose.desktop.currentOs)

    implementation(projects.kmpuiviews)
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.showcase.MainKt"
    }
}
