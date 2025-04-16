package plugins

import AppInfo
import com.android.build.api.dsl.androidLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import javax.inject.Inject

class MultiplatformLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("kotlin-multiplatform")
        target.pluginManager.apply(target.libs.plugins.android.kotlin.multiplatform.library.get().pluginId)

        val dependency = target.extensions.create(
            "otakuDependencies",
            DependencyHandling::class.java,
            target
        )

        /*target.extensions.findByType(LibraryExtension::class.java)?.apply {
            compileSdk = AppInfo.compileVersion
            defaultConfig {
                minSdk = AppInfo.minimumSdk
            }
        }*/

        //target.afterEvaluate {
        target.extensions
                .findByType(KotlinMultiplatformExtension::class.java)
                ?.apply { setup(dependency) }
        //}
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun KotlinMultiplatformExtension.setup(
        dependencyHandling: DependencyHandling,
    ) {
        /*androidTarget {
            compilations.all {
                this@androidTarget.compilerOptions {
                    freeCompilerArgs.add("-Xcontext-receivers")
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }*/

        androidLibrary {
            namespace = dependencyHandling.androidPackageName
            compileSdk = AppInfo.compileVersion
            minSdk = AppInfo.minimumSdk
        }

        val xcfName = "sharedKit"

        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = xcfName
                isStatic = true
            }
        }

        jvm()

        /*sourceSets.getByName("commonMain") {
            dependencies {
                dependencyHandling.commonDependencyBlock(this)
            }
        }

        sourceSets.getByName("androidMain") {
            dependencies {
                dependencyHandling.androidDependencyBlock(this)
            }
        }*/
    }
}

abstract class DependencyHandling @Inject constructor(project: Project) {

    var androidPackageName: String = ""

    internal var commonDependencyBlock: KotlinDependencyHandler.() -> Unit = {}

    fun commonDependencies(block: KotlinDependencyHandler.() -> Unit) {
        commonDependencyBlock = block
    }

    internal var androidDependencyBlock: KotlinDependencyHandler.() -> Unit = {}

    fun androidDependencies(block: KotlinDependencyHandler.() -> Unit) {
        androidDependencyBlock = block
    }

}