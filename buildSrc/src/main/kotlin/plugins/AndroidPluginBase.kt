package plugins

import AppInfo
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import java.util.Locale
import kotlin.reflect.KClass

abstract class AndroidPluginBase<T : BaseExtension>(
    private val clazz: KClass<T>,
) : Plugin<Project> {

    abstract fun Project.projectSetup()
    abstract fun T.androidConfig(project: Project)

    override fun apply(target: Project) {
        target.projectSetup()
        target.pluginManager.apply("kotlin-android")
        target.setupKotlinCompileOptions()
        target.configureAndroidBase()
    }

    protected fun Project.useGoogleType() {
        /*extensions.findByType<BaseAppModuleExtension>()?.apply {
            applicationVariants.forEach { variant ->
                println(variant.name)
                val variantName = variant.name
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val googleTask = tasks.findByName("process${variantName}GoogleServices")
                // Need to get the noFirebase packages in firebase first
                // googleTask?.enabled = System.getenv("CI") != null
                //TODO: Testing
                googleTask?.enabled = ProductFlavorTypes.NoFirebase.nameType != variant.flavorName
            }
        }*/
        extensions.findByType<BaseAppModuleExtension>()?.apply {
            applicationVariants.configureEach { // configureEach is safer than forEach in Gradle
                val variant = this
                val variantName = variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }

                val isNoFirebase = ProductFlavorTypes.NoFirebase.nameType == variant.flavorName

                // Lazily find and disable the Google Services task
                project.tasks.matching { it.name == "process${variantName}GoogleServices" }
                    .configureEach {
                        enabled = !isNoFirebase
                    }

                // CRITICAL: Because you applied "com.google.firebase.crashlytics",
                // you must also disable Crashlytics tasks for the NoFirebase flavor,
                // otherwise they will crash looking for Google Services outputs.
                if (isNoFirebase) {
                    project.tasks.matching {
                        it.name.contains("Crashlytics", ignoreCase = true) &&
                                it.name.contains(variantName, ignoreCase = true)
                    }.configureEach {
                        enabled = false
                    }
                }
            }
        }
    }

    private fun Project.configureAndroidBase() {
        extensions.findByType(clazz)?.apply {
            androidConfig(this@configureAndroidBase)
            compileSdkVersion(AppInfo.compileVersion)

            defaultConfig {
                minSdk = AppInfo.minimumSdk
                targetSdk = AppInfo.targetSdk
                versionCode = AppInfo.versionCode
                versionName = AppInfo.otakuVersionName

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            packagingOptions {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}:"
                    excludes += "license/README.dom.txt"
                    excludes += "license/LICENSE.dom-documentation.txt"
                    excludes += "license/NOTICE"
                    excludes += "license/LICENSE.dom-software.txt"
                    excludes += "license/LICENSE*"
                    excludes += "license/LICENSE"
                }
            }

            dependencies {
                implementation(libs.kotlinStLib.get())
                implementation(libs.androidCore.get())
                implementation(libs.appCompat.get())
            }
        }
    }
}