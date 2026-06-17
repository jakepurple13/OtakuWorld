import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.io.FileInputStream
import java.util.Properties

plugins {
    `otaku-multiplatform-no-ios`
    alias(libs.plugins.ksp)
    id("kotlinx-serialization")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.buildKonfig)
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
                implementation(commonLibs.koog.agents)
                implementation(commonLibs.koog.agents.additions)
                implementation(commonLibs.koog.memory)
                implementation(commonLibs.compose.material3)
                //implementation(compose.material3)
                implementation(commonLibs.material.icons.extended)
                implementation(commonLibs.runtime)
                implementation(commonLibs.ui)
                implementation(commonLibs.cmp.ui.util)
                implementation(commonLibs.foundation)
                implementation(commonLibs.markdown.renderer)
                implementation(project.dependencies.platform(commonLibs.koin.bom))
                implementation(commonLibs.bundles.koinKmp)
                implementation(projects.favoritesdatabase)
                implementation(commonLibs.kotlinx.datetime)
            }
        }

        commonTest {
            dependencies {
                implementation(commonLibs.kotlin.test)
            }
        }

        jvmMain {

        }

        androidMain {
            dependencies {
                // Add Android-specific dependencies here. Note that this source set depends on
                // commonMain by default and will correctly pull the Android artifacts of any KMP
                // dependencies declared in commonMain.
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

buildkonfig {
    packageName = "com.programmersbox.kmpuiviews.koogintegration"

    // Initialize properties object
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }

    defaultConfigs {
        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "LANG_FUSE_SECRET_KEY",
            value = localProperties.getProperty("langfuseSecretKey"),
            nullable = true
        )

        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "LANG_FUSE_PUBLIC_KEY",
            value = localProperties.getProperty("langfusePublicKey"),
            nullable = true
        )

        buildConfigField(
            type = FieldSpec.Type.STRING,
            name = "LANG_FUSE_URL",
            value = localProperties.getProperty("langfuseUrl"),
            nullable = true
        )
    }
}