import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.exclude
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Main class shared by every desktop app (`com.programmersbox.desktop.MainKt` in each `*world/desktop`). */
const val DESKTOP_MAIN_CLASS = "com.programmersbox.desktop.MainKt"

/**
 * @property name full version, e.g. `1.2.3-nightly`; from `DESKTOP_VERSION` or [AppInfo.otakuVersionName].
 * @property numeric [name] without its suffix, for the macOS and Windows installers that only accept numbers.
 */
data class DesktopVersion(val name: String, val numeric: String)

/**
 * Shared setup for the `*world/desktop` apps that does not need the Compose, hot-reload or
 * Nucleus plugin types (those plugins are not on the buildSrc classpath). Call it after the
 * `plugins {}` block. Dependencies and the `nucleus.application {}` block stay in each app.
 *
 * Sets up the JVM target and compiler flags, drops `kotlinx-coroutines-android`, registers the
 * splash card with [title], and returns the version to package.
 */
fun Project.otakuDesktopApp(title: String): DesktopVersion {
    configurations.all {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
    }

    extensions.configure<KotlinMultiplatformExtension> {
        jvm()

        compilerOptions {
            freeCompilerArgs.add("-Xwhen-guards")
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    registerDesktopSplash(title = title)

    val desktopVersion = System.getenv("DESKTOP_VERSION") ?: AppInfo.otakuVersionName

    // DESKTOP_VERSION is read from the environment, so Gradle must see it as an input or it reuses stale outputs.
    tasks.configureEach {
        if (name == "generateAppProperties" || name.startsWith("package") || (name.startsWith("create") && name.endsWith("Distributable"))) {
            inputs.property("desktopVersion", desktopVersion)
        }
    }

    return DesktopVersion(name = desktopVersion, numeric = desktopVersion.substringBefore('-'))
}
