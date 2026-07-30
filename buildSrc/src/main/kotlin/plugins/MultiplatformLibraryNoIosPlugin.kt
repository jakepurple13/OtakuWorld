package plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MultiplatformLibraryNoIosPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.pluginManager.apply("kotlin-multiplatform")
        target.pluginManager.apply(target.libs.plugins.android.kotlin.multiplatform.library.get().pluginId)

        val dependency = target.extensions.create(
            "otakuDependencies",
            DependencyHandling::class.java,
            target
        )

        target.setupKotlinCompileOptions()

        target.extensions
            .findByType(KotlinMultiplatformExtension::class.java)
            ?.apply { setup(dependency) }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun KotlinMultiplatformExtension.setup(
        dependencyHandling: DependencyHandling,
    ) {
        jvmToolchain(21)

        (this as org.gradle.api.plugins.ExtensionAware)
            .extensions
            .configure(com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension::class.java) {
                namespace = dependencyHandling.androidPackageName
                compileSdk = AppInfo.compileVersion
                minSdk = AppInfo.minimumSdk

                lint {
                    checkReleaseBuilds = false
                }
            }

        jvm()

        applyDefaultHierarchyTemplate()
    }
}