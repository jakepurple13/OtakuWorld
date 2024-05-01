package plugins

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class AndroidApplicationPlugin : AndroidPluginBase<BaseAppModuleExtension>(BaseAppModuleExtension::class) {

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.application")
        pluginManager.apply(EasyLauncherSetup::class)
        pluginManager.apply("com.google.gms.google-services")
        pluginManager.apply("com.google.firebase.crashlytics")
        pluginManager.apply(libs.plugins.compose.compiler.get().pluginId)
        afterEvaluate { useGoogleType() }
    }

    override fun BaseAppModuleExtension.androidConfig(project: Project) {
        buildFeatures.compose = true
        buildFeatures.buildConfig = true

        composeOptions {
            useLiveLiterals = true
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
                if(System.getenv("CI") == null) //TODO: Testing
                    applicationIdSuffix = ".noFirebase"
                isDefault = true
            }
            ProductFlavorTypes.Full(this)
        }
    }
}
