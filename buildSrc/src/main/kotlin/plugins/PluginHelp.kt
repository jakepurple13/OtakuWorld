package plugins

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.the

//internal val Project.libs get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal val Project.libs get() = the<LibrariesForLibs>()

internal fun DependencyHandlerScope.implementation(dependency: Any) = add("implementation", dependency)