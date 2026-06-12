plugins {
    `otaku-multiplatform-no-ios`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
}

otakuDependencies {
    androidPackageName = "com.programmersbox.koogintegration"
}

kotlin {

    // Target declarations - add or remove as needed below. These define
    // which platforms this KMP module supports.
    // See: https://kotlinlang.org/docs/multiplatform-discover-project.html#targets
    android {
        namespace = "com.programmersbox.koogintegration"
        compileSdk {
            version = release(36) {
                minorApiLevel = 1
            }
        }
        minSdk = 24
    }

    // Source set declarations.
    // Declaring a target automatically creates a source set with the same name. By default, the
    // Kotlin Gradle Plugin creates additional source sets that depend on each other, since it is
    // common to share sources between related targets.
    // See: https://kotlinlang.org/docs/multiplatform-hierarchy.html
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinStLib)
                implementation(libs.koog.agents)
                implementation(libs.koog.agents.additions)
                implementation(libs.koog.memory)
                implementation(libs.compose.material3)
                //implementation(compose.material3)
                implementation(libs.material.icons.extended)
                implementation(libs.runtime)
                implementation(libs.ui)
                implementation(libs.cmp.ui.util)
                implementation(libs.foundation)
                implementation(libs.markdown.renderer)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.bundles.koinKmp)
                implementation(projects.favoritesdatabase)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinxSerialization)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutinesTest)
            }
        }

        jvmMain {

        }

        androidMain {
            dependencies {
                implementation(libs.workRuntime)
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.workmanager)
            }
        }
        iosMain {
            dependencies {
                // Add iOS-specific dependencies here. This a source set created by Kotlin Gradle
                // Plugin (KGP) that each specific iOS target (e.g., iosX64) depends on as
                // part of KMP’s default source set hierarchy. Note that this source set depends
                // on common by default and will correctly pull the iOS artifacts of any
                // KMP dependencies declared in commonMain.
            }
        }
    }
}