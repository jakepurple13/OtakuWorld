import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN

plugins {
    `otaku-multiplatform`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildKonfig)
    //alias(libs.plugins.koin.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.kmpuiviews"
}

kotlin {
    android {
        namespace = "com.programmersbox.kmpuiviews"
        androidResources {
            enable = true
        }
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
                implementation(commonLibs.compose.material3)
                //implementation(compose.material3)
                implementation(commonLibs.material.icons.extended)
                implementation(commonLibs.runtime)
                implementation(commonLibs.ui)
                implementation(commonLibs.cmp.ui.util)
                implementation(commonLibs.foundation)
                implementation(commonLibs.material3.adaptive.navigation.suite)
                implementation(commonLibs.components.resources)
                api(commonLibs.ui.backhandler)
                implementation(commonLibs.material3.window.size)
                api(commonLibs.haze)
                api(commonLibs.haze.blur)
                api(commonLibs.haze.materials)
                api(commonLibs.backdrop)
                implementation(commonLibs.material.kolor)
                api(commonLibs.kamel.image)
                api(commonLibs.kamel.decoder.animated.image)
                api(commonLibs.kamel.decoder.image.bitmap)
                api(commonLibs.kamel.decoder.image.vector)
                api(commonLibs.kamel.decoder.svg.std)
                api(commonLibs.coilCompose)
                api(commonLibs.kotlinxSerialization)
                api(commonLibs.ktorCore)
                implementation(commonLibs.ktorAuth)
                implementation(commonLibs.ktorLogging)
                implementation(commonLibs.ktorSerialization)
                implementation(commonLibs.ktorJson)
                implementation(commonLibs.ktorContentNegotiation)
                implementation(commonLibs.coroutinesCore)

                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.bundles.koinKmp)

                implementation(commonLibs.kmpalette.core)

                implementation(projects.favoritesdatabase)
                api(projects.datastore)
                api(projects.kmpmodels)
                implementation(projects.sharedutils.kmpextensionloader)
                implementation(commonLibs.bundles.datastoreLibs)

                api(commonLibs.kotlinx.datetime)

                implementation(commonLibs.roomRuntime)

                api(commonLibs.compose.webview.multiplatform)

                implementation(commonLibs.connectivity.core)
                implementation(commonLibs.connectivity.compose)

                api(commonLibs.filekit.core)
                implementation(commonLibs.filekit.dialogs.compose)

                implementation(commonLibs.lifecycle.viewmodel.compose)

                implementation(commonLibs.aboutLibrariesCore)
                implementation(commonLibs.aboutLibrariesCompose)

                implementation(commonLibs.sonner)

                implementation(commonLibs.urlencoder.lib)
                //implementation(commonLibs.blurhash)

                implementation(commonLibs.dragselect)

                implementation(commonLibs.compottie)

                implementation(commonLibs.roomPaging)

                implementation(commonLibs.constraintlayout.compose.multiplatform)
                implementation(commonLibs.compose.constraintlayout.compose.multiplatform)

                implementation(commonLibs.qrose)
                //implementation(commonLibs.androidx.navigationevent)
                //implementation(commonLibs.androidx.navigationevent.compose)
                implementation(commonLibs.androidx.navigation3.runtime)

                implementation(commonLibs.scanner)
                implementation(commonLibs.multiplatform.lifecycle.runtime.compose)

                implementation(commonLibs.materialAdaptiveCmp)
                implementation(commonLibs.materialAdaptiveLayoutCmp)
                implementation(commonLibs.materialAdaptiveLayoutNavCmp)

                implementation(commonLibs.reorderable)

                implementation(commonLibs.paging.compose.common)

                /*implementation(commonLibs.androidx.navigation3.runtime)
                implementation(commonLibs.androidx.navigation3.ui)*/

                implementation(commonLibs.generativeai.google)
                implementation(commonLibs.generic.ai)
                implementation(commonLibs.anthropic.sdk.kotlin)
                implementation(commonLibs.xemantic.ai.tool.schema)

                //implementation(commonLibs.heatmap)

                implementation(commonLibs.cmp.navigation3.ui)
                implementation(commonLibs.cmp.lifecycle.viewmodel.navigation3)
                implementation(commonLibs.cmp.navigationevent.compose)
                implementation(commonLibs.cmp.material3.adaptive.nav3)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
            }
        }

        androidMain {
            dependencies {
                implementation(commonLibs.heatmap)
                implementation(commonLibs.kamel.decoder.image.bitmap.resizing)
                implementation(commonLibs.kamel.decoder.svg.batik)
                implementation(commonLibs.ktorAndroid)
                implementation(androidx.browser.browser)
                implementation(androidLibs.androidBrowserHelper)
                implementation(project.dependencies.platform(androidLibs.firebasePlatform))
                implementation(androidLibs.firebaseAuth)
                implementation(androidLibs.playServices)
                implementation(androidLibs.bundles.firebaseCrashLibs)
                implementation(androidLibs.drawablePainter)
                implementation(androidLibs.ackpine.core)
                implementation(androidLibs.ackpine.ktx)
                implementation(androidLibs.glideCompose)
                implementation(androidLibs.landscapist.bom)
                implementation(androidLibs.landscapistGlide)
                implementation(androidLibs.landscapistPalette)
                implementation(androidLibs.landscapistPlaceholder)
                implementation(androidLibs.zoomable.peek.overlay)
                implementation(androidLibs.barcode.scanning)
                implementation(androidLibs.biometric)
                implementation(androidx.activity.activityKtx)
                implementation(androidLibs.lazyColumnScrollbar)
                implementation(androidLibs.workRuntime)
                implementation(androidLibs.koin.workmanager)
                implementation(androidx.paging.pagingCompose)
            }
        }

        iosMain {
            dependencies {
                implementation(commonLibs.moko.biometry)
                implementation(commonLibs.moko.biometry.compose)
            }
        }

        jvmMain {
            dependencies {
                implementation(commonLibs.heatmap)
                implementation(desktopLibs.core)
                implementation(desktopLibs.javase)
                implementation(desktopLibs.knotify)
                implementation(desktopLibs.kotlinx.coroutines.swing)
                api(desktopLibs.kotlin.multiplatform.appdirs)
                api(desktopLibs.kfswatch)
                implementation(desktopLibs.nucleus.system.color)
                api(desktopLibs.github.nucleus.scheduler)
                api(desktopLibs.github.nucleus.scheduler.testing)
                api(desktopLibs.github.nucleus.taskbar.progress)
                api(desktopLibs.github.nucleus.notifications.common)
                api(desktopLibs.nucleus.system.info)
                //implementation("io.github.n7ghtm4r3:Biometrik:1.0.0beta-01")
            }
        }

        val deviceMain by creating {
            dependsOn(commonMain.get())
            androidMain.get().dependsOn(this)
            iosMain.get().dependsOn(this)
            dependencies {
                implementation(commonLibs.connectivity.device)
                implementation(commonLibs.connectivity.compose.device)
            }
        }

        val httpMain by creating {
            dependsOn(commonMain.get())
            jvmMain.get().dependsOn(this)
            dependencies {
                implementation(commonLibs.connectivity.http)
                implementation(commonLibs.connectivity.compose.http)
            }
        }

        val usesJvmMain by creating {
            dependsOn(commonMain.get())
            androidMain.get().dependsOn(this)
            jvmMain.get().dependsOn(this)
        }

        all {

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

        buildConfigField(
            type = FieldSpec.Type.STRING,
            const = true,
            name = "VERSION_NAME_KMP",
            value = AppInfo.otakuVersionName
        )

        buildConfigField(
            type = FieldSpec.Type.STRING,
            const = true,
            name = "VERSION_CODE_KMP",
            value = AppInfo.versionCode.toString()
        )
    }
}