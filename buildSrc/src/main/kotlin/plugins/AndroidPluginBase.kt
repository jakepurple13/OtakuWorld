package plugins

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import kotlin.reflect.KClass

abstract class AndroidPluginBase<T: BaseExtension>(
    private val clazz: KClass<T>
) : Plugin<Project> {

    abstract fun Project.projectSetup()
    abstract fun T.androidConfig(project: Project)

    override fun apply(target: Project) {
        target.projectSetup()
        target.pluginManager.apply("kotlin-android")
        target.configureAndroidBase()
        target.afterEvaluate { useGoogleType() }
    }

    private fun Project.useGoogleType() {
        extensions.findByType<BaseAppModuleExtension>()?.apply {
            applicationVariants.forEach { variant ->
                println(variant.name)
                val googleTask = tasks.findByName("process${variant.name.capitalize()}GoogleServices")
                googleTask?.enabled = "noFirebase" != variant.flavorName
            }
        }
    }

    fun Project.configureAndroidBase() {
        extensions.findByType(clazz)?.apply {
            androidConfig(this@configureAndroidBase)
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
                implementation(libs.findLibrary("kotlinStLib").get())
                implementation(libs.findLibrary("androidCore").get())
                implementation(libs.findLibrary("appCompat").get())
            }
        }
    }
}