package plugins

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType

class AndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.library")
        target.pluginManager.apply("kotlin-android")
        target.configureAndroidLibraryPlugin()
        target.afterEvaluate { useGoogleType() }
    }

    fun Project.useGoogleType() {
        extensions.findByType<BaseAppModuleExtension>()?.apply {
            applicationVariants.forEach { variant ->
                println(variant.name)
                val googleTask = tasks.findByName("process${variant.name.capitalize()}GoogleServices")
                googleTask?.enabled = "noFirebase" != variant.flavorName
            }
        }
    }

    fun Project.configureAndroidLibraryPlugin() {
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            compileSdkVersion(AppInfo.compileVersion)
            buildToolsVersion(AppInfo.buildVersion)

            defaultConfig {
                minSdk = AppInfo.minimumSdk
                targetSdk = AppInfo.targetSdk
                versionCode = 1
                versionName = AppInfo.otakuVersionName

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_1_8
            }

            packagingOptions {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}:"
                }
            }

            dependencies {

            }
        }
    }

}
