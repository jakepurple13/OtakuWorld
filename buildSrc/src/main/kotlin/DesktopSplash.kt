import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

// Native JVM splash shown before any app code runs. The packaged app expands $APPDIR itself;
// the Gradle `run` task does not, so registerDesktopSplash() rewrites it for that task.
const val DESKTOP_SPLASH_JVM_ARG = "-splash:\$APPDIR/resources/splash.png"

private const val RESOURCES_DIR_PROPERTY = "-Dcompose.application.resources.dir="

/**
 * Copies `splash/` (splash.png + splash@2x.png) into `build/appResources/common`, which must be
 * the Compose `appResourcesRootDir`, and points the `run` task's splash at the prepared resources.
 */
fun Project.registerDesktopSplash() {
    val prepareSplash = tasks.register<Copy>("prepareSplash") {
        from(layout.projectDirectory.dir("splash"))
        into(layout.buildDirectory.dir("appResources/common"))
    }

    tasks.named { it == "prepareAppResources" || it == "run" }.configureEach {
        dependsOn(prepareSplash)
    }

    tasks.withType<JavaExec>().named { it == "run" }.configureEach {
        doFirst {
            val args = jvmArgs.orEmpty()
            val resourcesDir = args
                .firstOrNull { it.startsWith(RESOURCES_DIR_PROPERTY) }
                ?.removePrefix(RESOURCES_DIR_PROPERTY)
                ?: return@doFirst
            jvmArgs = args.map { it.replace("\$APPDIR/resources", resourcesDir) }
        }
    }
}
