package plugins

import com.android.build.gradle.BaseExtension
import org.gradle.api.Project

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
            ApplicationBuildTypes.Release.setup(this) {
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            }
            ApplicationBuildTypes.Debug.setup(this)
            ApplicationBuildTypes.Beta.setup(this)
        }

        flavorDimensions(ProductFlavorTypes.dimension)
        productFlavors {
            ProductFlavorTypes.NoFirebase(this) {
                versionNameSuffix("-noFirebase")
                applicationIdSuffix(".noFirebase")
            }
            ProductFlavorTypes.Full(this)
        }
    }
}
