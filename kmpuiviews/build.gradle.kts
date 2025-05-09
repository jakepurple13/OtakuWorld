import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildKonfig)
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
                implementation("org.jetbrains.compose.ui:ui-backhandler:1.8.0")
                implementation(libs.material3.window.size)
                implementation(libs.haze)
                implementation(libs.hazeMaterials)
                implementation(libs.material.kolor)
                implementation(libs.kamel.image)
                implementation(libs.kamel.decoder.animated.image)
                implementation(libs.kamel.decoder.image.bitmap)
                implementation(libs.kamel.decoder.image.vector)
                implementation(libs.kamel.decoder.svg.std)
                implementation(libs.coilCompose)
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
                implementation(projects.sharedutils.kmpextensionloader)
                implementation(libs.bundles.datastoreLibs)

                implementation(libs.kotlinx.datetime)

                implementation(libs.roomRuntime)

                implementation(libs.compose.webview.multiplatform)

                implementation(libs.connectivity.core)
                implementation(libs.connectivity.compose)

                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.navigation.compose)

                implementation(libs.aboutLibrariesCore)
                implementation(libs.aboutLibrariesCompose)

                implementation(libs.sonner)

                implementation("net.thauvin.erik.urlencoder:urlencoder-lib:1.6.0")
                implementation(libs.blurhash)

                implementation(libs.dragselect)

                implementation(libs.compottie)

                /// Compose 1.7.3 with different tech.annexflow.constraintlayout.core package
                implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.5.1-shaded-core")
                /// Compose 1.7.3 with different tech.annexflow.constraintlayout package
                implementation("tech.annexflow.compose:constraintlayout-compose-multiplatform:0.5.1-shaded")
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
                implementation(project.dependencies.platform(libs.firebasePlatform))
                implementation(libs.firebaseAuth)
                implementation(libs.playServices)
                implementation(libs.bundles.firebaseCrashLibs)
                implementation(libs.drawablePainter)
                implementation(libs.ackpine.core)
                implementation(libs.ackpine.ktx)
                implementation(libs.glideCompose)
                implementation(libs.landscapist.bom)
                implementation(libs.landscapistGlide)
                implementation(libs.landscapistPalette)
                implementation(libs.landscapistPlaceholder)
                implementation(libs.zoomable.peek.overlay)
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

        val deviceMain by creating {
            dependsOn(commonMain.get())
            androidMain.get().dependsOn(this)
            iosMain.get().dependsOn(this)
            dependencies {
                implementation(libs.connectivity.device)
                implementation(libs.connectivity.compose.device)
            }
        }

        val httpMain by creating {
            dependsOn(commonMain.get())
            jvmMain.get().dependsOn(this)
            dependencies {
                implementation(libs.connectivity.http)
                implementation(libs.connectivity.compose.http)
            }
        }
    }
}

buildkonfig {
    packageName = "com.programmersbox.kmpuiviews"

    defaultConfigs {
        buildConfigField(
            type = BOOLEAN,
            const = true,
            name = "IS_PRERELEASE",
            value = runCatching { System.getenv("IS_PRERELEASE") }
                .onFailure { it.printStackTrace() }
                .mapCatching { it.toBoolean() }
                .getOrDefault(false)
                .toString()
                .also { println("IS_PRERELEASE: $it") }
        )
    }
}