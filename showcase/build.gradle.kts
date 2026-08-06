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
    implementation(projects.favoritesdatabase)
    implementation(projects.showcase.annotations)
    ksp(projects.showcase.processor)

    implementation(commonLibs.compose.material3)
    implementation(commonLibs.material.icons.extended)
    implementation(commonLibs.runtime)
    implementation(commonLibs.ui)
    implementation(commonLibs.foundation)
    implementation(commonLibs.material.kolor)
    implementation(compose.desktop.currentOs)

    implementation(commonLibs.cmp.navigation3.ui)
    implementation(commonLibs.cmp.lifecycle.viewmodel.navigation3)
    implementation(commonLibs.cmp.navigationevent.compose)
    implementation(commonLibs.cmp.material3.adaptive.nav3)

    implementation(project.dependencies.platform(commonLibs.koin.bom))
    implementation(commonLibs.bundles.koinKmp)

    implementation(projects.kmpuiviews)
}

ksp {
    arg("showcaseModuleId", "showcase")
}

compose.desktop {
    application {
        mainClass = "com.programmersbox.showcase.MainKt"
    }
}
