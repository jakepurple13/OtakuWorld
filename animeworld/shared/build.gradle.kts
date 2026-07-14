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

            implementation(commonLibs.androidx.navigation3.runtime)
        }

        androidMain.dependencies {
            implementation(androidLibs.bundles.media3)
            implementation(projects.uiViews)
            implementation(Deps.helpfulutils)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(desktopLibs.kotlinx.coroutines.swing)
        }
    }
}
