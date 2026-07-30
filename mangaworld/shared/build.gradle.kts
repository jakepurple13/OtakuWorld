plugins {
    `otaku-multiplatform`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
}

kotlin {
    jvmToolchain(21)

    android {
        namespace = "com.programmersbox.manga.shared"
        androidResources {
            enable = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            //implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.uiUtil)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.datastore.mangasettings)
            implementation(projects.kmpmodels)
            implementation(commonLibs.bundles.datastoreLibs)

            implementation(commonLibs.androidx.navigation3.runtime)
            implementation(commonLibs.multiplatform.lifecycle.runtime.compose)

            implementation(commonLibs.zoomableModifier)
            implementation(commonLibs.coilCompose)
        }

        androidMain.dependencies {
            implementation(androidLibs.panpf.zoomimage.compose.glide)
            implementation(androidLibs.telephoto.zoomable.image.glide)
            implementation(androidLibs.workRuntime)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation(commonLibs.coroutinesTest)
            implementation(commonLibs.ktorMock)
        }
    }
}