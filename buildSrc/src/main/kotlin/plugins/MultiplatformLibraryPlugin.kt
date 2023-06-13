package plugins

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getting
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfo
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.inject.Inject
import kotlin.reflect.KClass

class MultiplatformLibraryPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("kotlin-multiplatform")
        target.pluginManager.apply("com.android.library")
        target.pluginManager.apply("org.jetbrains.compose")

        val dependency = target.extensions.create(
            "otakuDependencies",
            DependencyHandling::class.java,
            target
        )

        target.extensions.findByType(LibraryExtension::class.java)?.apply {
            compileSdk = 33
            defaultConfig {
                minSdk = 23
            }
        }

        target.afterEvaluate {
            extensions
                .findByType(KotlinMultiplatformExtension::class.java)
                ?.apply { setup(dependency) }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun KotlinMultiplatformExtension.setup(
        dependencyHandling: DependencyHandling,
    ) {
        targetHierarchy.default()
        android {
            compilations.all {
                kotlinOptions { jvmTarget = "1.8" }
            }
        }

        sourceSets.getByName("commonMain") {
            dependencies {
                dependencyHandling.commonDependencyBlock(this)
            }
        }

        sourceSets.getByName("androidMain") {
            dependencies {
                dependencyHandling.androidDependencyBlock(this)
            }
        }
    }
}

abstract class DependencyHandling @Inject constructor(project: Project) {

    internal var commonDependencyBlock: KotlinDependencyHandler.() -> Unit = {}

    fun commonDependencies(block: KotlinDependencyHandler.() -> Unit) {
        commonDependencyBlock = block
    }

    internal var androidDependencyBlock: KotlinDependencyHandler.() -> Unit = {}

    fun androidDependencies(block: KotlinDependencyHandler.() -> Unit) {
        androidDependencyBlock = block
    }

}