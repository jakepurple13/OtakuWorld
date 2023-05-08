package plugins

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.provideDelegate

class AndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.android.application")
        target.pluginManager.apply("kotlin-android")
        target.configureAndroidBasePlugin()
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

    fun Project.configureAndroidBasePlugin() {
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

            buildFeatures.compose = true

            composeOptions {
                useLiveLiterals = true
            }

            packagingOptions {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}:"
                }
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
                create("noFirebase") {
                    dimension("version")
                    versionNameSuffix("-noFirebase")
                    applicationIdSuffix(".noFirebase")
                }
                create("full") {
                    dimension("version")
                }
            }

            dependencies {

            }
        }
    }
}
