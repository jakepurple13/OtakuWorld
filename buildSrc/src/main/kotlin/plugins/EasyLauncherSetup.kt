package plugins

import com.project.starter.easylauncher.filter.ChromeLikeFilter
import com.project.starter.easylauncher.plugin.EasyLauncherExtension
import com.project.starter.easylauncher.plugin.EasyLauncherTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

class EasyLauncherSetup : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("com.starter.easylauncher")
        target.tasks.withType<com.android.build.gradle.tasks.MapSourceSetPathsTask>().configureEach {
            target.tasks.withType<EasyLauncherTask>().forEach {
                this@configureEach.mustRunAfter(it)
            }
        }

        /*target.afterEvaluate {
            extensions.findByType<EasyLauncherExtension>()?.apply {
                defaultFlavorNaming(true)
                productFlavors.register(ProductFlavorTypes.NoFirebase.nameType) {
                    filters(chromeLike())
                }

                buildTypes.apply {
                    register(ApplicationBuildTypes.Debug.buildTypeName) {
                        filters(chromeLike(gravity = ChromeLikeFilter.Gravity.TOP))
                    }
                    register(ApplicationBuildTypes.Beta.buildTypeName) {
                        filters(chromeLike(gravity = ChromeLikeFilter.Gravity.TOP))
                    }
                }
            }
        }*/
    }
}