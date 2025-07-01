import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import implementation

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

    applyDefaultHierarchyTemplate {
        common {
            group("macos") {
                withJvm()
                withMacos()
            }

            group("windows") {
                withJvm()
                withMingw()
            }

            group("linux") {
                withJvm()
                withLinux()
            }
        }
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
                api(libs.ui.backhandler)
                implementation(libs.material3.window.size)
                api(libs.haze)
                api(libs.hazeMaterials)
                implementation(libs.material.kolor)
                api(libs.kamel.image)
                api(libs.kamel.decoder.animated.image)
                api(libs.kamel.decoder.image.bitmap)
                api(libs.kamel.decoder.image.vector)
                api(libs.kamel.decoder.svg.std)
                api(libs.coilCompose)
                api(libs.kotlinxSerialization)
                api(libs.ktorCore)
                implementation(libs.ktorAuth)
                implementation(libs.ktorLogging)
                implementation(libs.ktorSerialization)
                implementation(libs.ktorJson)
                implementation(libs.ktorContentNegotiation)

                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.bundles.koinKmp)

                implementation(libs.kmpalette.core)

                implementation(projects.favoritesdatabase)
                api(projects.datastore)
                api(projects.kmpmodels)
                implementation(projects.sharedutils.kmpextensionloader)
                implementation(libs.bundles.datastoreLibs)

                api(libs.kotlinx.datetime)

                implementation(libs.roomRuntime)

                api(libs.compose.webview.multiplatform)

                implementation(libs.connectivity.core)
                implementation(libs.connectivity.compose)

                api(libs.filekit.core)
                implementation(libs.filekit.dialogs.compose)

                implementation(libs.lifecycle.viewmodel.compose)
                implementation(libs.navigation.compose)

                implementation(libs.aboutLibrariesCore)
                implementation(libs.aboutLibrariesCompose)

                implementation(libs.sonner)

                implementation(libs.urlencoder.lib)
                implementation(libs.blurhash)

                implementation(libs.dragselect)

                implementation(libs.compottie)

                implementation(libs.roomPaging)

                implementation(libs.constraintlayout.compose.multiplatform)
                implementation(libs.compose.constraintlayout.compose.multiplatform)

                implementation(libs.qrose)
                implementation(libs.androidx.navigationevent)
                implementation(libs.androidx.navigationevent.compose)
                implementation(libs.scanner)

                implementation(libs.multiplatform.lifecycle.runtime.compose)
                implementation(libs.androidx.navigation3.runtime)

                implementation(libs.materialAdaptiveCmp)
                implementation(libs.materialAdaptiveLayoutCmp)
                implementation(libs.materialAdaptiveLayoutNavCmp)

                implementation(libs.reorderable)

                implementation(libs.paging.compose.common)

                implementation(libs.human.readable)

                /*implementation(libs.androidx.navigation3.runtime)
                implementation(libs.androidx.navigation3.ui)*/

                implementation(libs.generativeai.google)
                implementation(libs.generic.ai)
                implementation(libs.anthropic.sdk.kotlin)
                implementation(libs.xemantic.ai.tool.schema)
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
                implementation(libs.barcode.scanning)
                implementation(libs.biometric)
                implementation(androidx.activity.activityKtx)
                implementation(libs.lazyColumnScrollbar)
                implementation(libs.workRuntime)
                implementation(libs.koin.workmanager)
            }
        }

        iosMain {
            dependencies {

            }
        }

        jvmMain {
            dependencies {
                implementation(libs.core)
                implementation(libs.javase)
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

        all {
            languageSettings.enableLanguageFeature("WhenGuards")
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