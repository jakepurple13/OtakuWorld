import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

// Native JVM splash shown before any app code runs. Nucleus adds the -splash arg for both the
// packaged app and the Gradle `run` task from nativeDistributions.splashImage.
const val DESKTOP_SPLASH_IMAGE = "splash.png"

/**
 * Copies `splash/` (splash.png + splash@2x.png) into `build/appResources/common`, which must be
 * the Nucleus `appResourcesRootDir`.
 */
fun Project.registerDesktopSplash() {
    val prepareSplash = tasks.register<Copy>("prepareSplash") {
        from(layout.projectDirectory.dir("splash"))
        into(layout.buildDirectory.dir("appResources/common"))
    }

    tasks.named { it == "prepareAppResources" || it == "run" }.configureEach {
        dependsOn(prepareSplash)
    }
}
