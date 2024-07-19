package plugins

import AppInfo
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale
import kotlin.reflect.KClass

abstract class AndroidPluginBase<T: BaseExtension>(
    private val clazz: KClass<T>
) : Plugin<Project> {

    abstract fun Project.projectSetup()
    abstract fun T.androidConfig(project: Project)

    override fun apply(target: Project) {
        target.projectSetup()
        target.pluginManager.apply("kotlin-android")
        target.tasks.withType<KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.add("-Xcontext-receivers")
                jvmTarget.set(JvmTarget.JVM_1_8)
            }
        }
        target.configureAndroidBase()
    }

    protected fun Project.useGoogleType() {
        extensions.findByType<BaseAppModuleExtension>()?.apply {
            applicationVariants.forEach { variant ->
                println(variant.name)
                val variantName = variant.name
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val googleTask = tasks.findByName("process${variantName}GoogleServices")
                // Need to get the noFirebase packages in firebase first
                // googleTask?.enabled = System.getenv("CI") != null
                //TODO: Testing
                googleTask?.enabled = ProductFlavorTypes.NoFirebase.nameType != variant.flavorName || System.getenv("CI") != null
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