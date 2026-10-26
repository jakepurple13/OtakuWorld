import plugins.ProductFlavorTypes

plugins {
    `otaku-multiplatform`
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlinx-serialization")
}

kotlin {
    android {
        namespace = "com.programmersbox.anime.shared"
        withHostTestBuilder {}.configure {
            isReturnDefaultValues = true
        }
        androidResources {
            enable = true
        }
        // :animeworld:shared's android target has no product flavors of its own, but its
        // androidMain dependency on `projects.uiViews` (for GenericInfo/BatteryInformation2) does
        // -- UIViews declares the noFirebase/full "version" flavor dimension. Without this, Gradle
        // can't pick a variant of UIViews to compile against. Match the app's default flavor.
        localDependencySelection {
            productFlavorDimension(ProductFlavorTypes.dimension) {
                selectFrom.set(listOf(ProductFlavorTypes.NoFirebase.nameType))
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinStLib)
            implementation(projects.kmpuiviews)
            implementation(commonLibs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3AdaptiveNavigationSuite)
            implementation(compose.components.resources)
            implementation(commonLibs.material.kolor)

            implementation(project.dependencies.platform(commonLibs.koin.bom))
            implementation(commonLibs.bundles.koinKmp)

            implementation(projects.favoritesdatabase)
            implementation(projects.datastore)
            implementation(projects.kmpmodels)
            implementation(commonLibs.bundles.datastoreLibs)
            implementation(commonLibs.kotlinxSerialization)

            implementation(commonLibs.androidx.navigation3.runtime)
        }

        // Plain-JVM code (OkHttp, java.io) used by both the Android app and the desktop app.
        androidMain {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
        }
        androidMain.dependencies {
            implementation(androidLibs.bundles.media3)
            implementation(projects.uiViews)
            implementation(Deps.helpfulutils)
            implementation(androidLibs.glide)
            implementation(androidLibs.glideCompose)
            implementation(androidLibs.workRuntimeKtx)
            implementation(androidLibs.okhttpLib)
            implementation(androidLibs.nanohttpd)
            implementation(androidLibs.castFramework)
            implementation(androidLibs.mediaRouter)
        }

        commonTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(commonLibs.coroutinesTest)
        }

        getByName("androidHostTest") {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
            dependencies {
                implementation(commonLibs.kotlin.test)
                implementation(commonLibs.coroutinesTest)
                implementation(androidLibs.okhttpMockWebServer)
            }
        }

        jvmMain {
            kotlin.srcDir("src/jvmAndroidShared/kotlin")
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
            // api: VlcRuntime.factory() exposes vlcj's MediaPlayerFactory to the desktop app.
            api(desktopLibs.vlcj)
            implementation(androidLibs.nanohttpd)
            implementation(androidLibs.okhttpLib)
        }

        jvmTest {
            kotlin.srcDir("src/jvmAndroidSharedTest/kotlin")
        }
        jvmTest.dependencies {
            implementation(commonLibs.kotlin.test)
            implementation(androidLibs.okhttpMockWebServer)
        }
    }
}
