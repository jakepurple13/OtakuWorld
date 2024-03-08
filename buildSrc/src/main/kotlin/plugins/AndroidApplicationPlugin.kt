package plugins

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project

class AndroidApplicationPlugin : AndroidPluginBase<BaseAppModuleExtension>(BaseAppModuleExtension::class) {

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.application")
    }

    override fun BaseAppModuleExtension.androidConfig(project: Project) {
        buildFeatures.compose = true
        buildFeatures.buildConfig = true

        composeOptions {
            useLiveLiterals = true
            kotlinCompilerExtensionVersion = project.libs.versions.jetpackCompiler.get()
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

        flavorDimensions.add(ProductFlavorTypes.dimension)
        productFlavors {
            ProductFlavorTypes.NoFirebase(this) {
                versionNameSuffix = "-noFirebase"
                applicationIdSuffix = ".noFirebase"
                isDefault = true
            }
            ProductFlavorTypes.Full(this)
        }
    }
}
