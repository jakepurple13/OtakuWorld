plugins {
    `otaku-multiplatform`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
}

kotlin {
    androidLibrary {
        namespace = "com.programmersbox.novel.shared"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation("org.jetbrains.compose.material3:material3:1.9.0-SNAPSHOT+release-1-8-data-source-prototype")
            //implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(libs.material.kolor)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(libs.bundles.datastoreLibs)

            implementation(libs.androidx.navigation3.runtime)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}