plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.kmpuiviews"
}

kotlin {
    androidLibrary {
        namespace = "com.programmersbox.kmpuiviews"
        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3AdaptiveNavigationSuite)
                implementation(compose.components.resources)
                implementation(libs.material3.window.size)
                implementation(libs.haze)
                implementation(libs.hazeMaterials)
                implementation(libs.material.kolor)
                implementation(libs.kamel.image)
                implementation(libs.kamel.decoder.animated.image)
                implementation(libs.kamel.decoder.image.bitmap)
                implementation(libs.kamel.decoder.image.vector)
                implementation(libs.kamel.decoder.svg.std)
                implementation(libs.kotlinxSerialization)
                implementation(libs.ktorCore)
                implementation(libs.ktorAuth)
                implementation(libs.ktorLogging)
                implementation(libs.ktorSerialization)
                implementation(libs.ktorJson)
                implementation(libs.ktorContentNegotiation)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.bundles.koinKmp)

                implementation(projects.favoritesdatabase)
                api(projects.datastore)
                api(projects.kmpmodels)
                implementation(libs.bundles.datastoreLibs)

                implementation(libs.roomRuntime)

                implementation(libs.constraintlayout.compose.multiplatform)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.kamel.decoder.image.bitmap.resizing)
                implementation(libs.kamel.decoder.svg.batik)
                implementation(libs.ktorAndroid)
                implementation(androidx.browser.browser)
                implementation(libs.androidBrowserHelper)
            }
        }

        iosMain {
            dependencies {

            }
        }

        jvmMain {
            dependencies {

            }
        }
    }
}