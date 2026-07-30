package plugins

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//internal val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Project.libs get() = the<LibrariesForLibs>()
internal val Project.androidLibs: VersionCatalog get() = extensions.getByType<VersionCatalogsExtension>().named("androidLibs")

internal fun DependencyHandlerScope.implementation(dependency: Any) = add("implementation", dependency)

internal fun Project.setupKotlinCompileOptions() {
    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xwhen-guards")
            freeCompilerArgs.add("-Xcontext-parameters")
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}