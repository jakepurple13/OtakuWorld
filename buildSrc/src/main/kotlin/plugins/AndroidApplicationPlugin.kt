package plugins

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.provideDelegate

class AndroidApplicationPlugin : AndroidPluginBase() {

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.application")
    }

    override fun BaseExtension.androidConfig(project: Project) {
        buildFeatures.compose = true

        composeOptions {
            useLiveLiterals = true
            kotlinCompilerExtensionVersion = project.libs.findVersion("jetpackCompiler").get().requiredVersion
        }

        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
            getByName("debug") {
                extra["enableCrashlytics"] = false
            }
            create("beta") {
                initWith(getByName("debug"))
                matchingFallbacks.addAll(listOf("debug", "release"))
                isDebuggable = false
            }
        }

        flavorDimensions("version")
        productFlavors {
            ProductFlavorTypes.NoFirebase(this) {
                versionNameSuffix("-noFirebase")
                applicationIdSuffix(".noFirebase")
            }
            ProductFlavorTypes.Full(this)
        }
    }
}
