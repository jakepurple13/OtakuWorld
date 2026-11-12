import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider

/** Runs the `:navgraph` scanner and writes `navgraph.json` for the debug Navigation map screen. */
abstract class GenerateNavGraphTask : JavaExec() {
    @get:Input
    abstract val appName: Property<String>

    @get:Internal
    abstract val repoRoot: DirectoryProperty

    /** Every Kotlin source and override file in the repo, so edits regenerate the graph. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
}

/**
 * Generates this app's navigation graph and packages it as an asset for `debug` builds only.
 * [appName] must be a key of `AppSources.apps` in `:navgraph`.
 */
fun Project.navGraph(appName: String) {
    val task = registerNavGraphTask(appName)
    extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
        // Debug builds only: published noFirebase release APKs must not carry repo paths or the debug screen.
        if (variant.buildType == "debug") {
            variant.sources.assets?.addGeneratedSourceDirectory(task, GenerateNavGraphTask::outputDir)
        }
    }
}

/** Local desktop run tasks only; packaged distributions (runDistributable, packageDmg, …) never see the graph. */
private val localDesktopRunTasks = setOf("run", "hotRunJvm", "jvmRun")

/**
 * Generates this desktop app's navigation graph and hands its path to local Gradle runs through the
 * `otaku.navgraph` system property (read by kmpuiviews' FileNavGraphSource). Nothing is packaged.
 */
fun Project.desktopNavGraph(appName: String) {
    val task = registerNavGraphTask(appName)
    val graphFile = task.flatMap { it.outputDir.file("navgraph.json") }
    tasks.matching { it.name in localDesktopRunTasks }.configureEach {
        val run = this as? JavaExec ?: return@configureEach
        run.dependsOn(task)
        run.jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf("-Dotaku.navgraph=${graphFile.get().asFile.absolutePath}")
        })
    }
}

private fun Project.registerNavGraphTask(appName: String): TaskProvider<GenerateNavGraphTask> {
    val tool = configurations.create("navGraphTool") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
    dependencies.add(tool.name, dependencies.project(mapOf("path" to ":navgraph")))

    return tasks.register<GenerateNavGraphTask>("generateNavGraph") {
        group = "otaku"
        description = "Scans sources and writes navgraph.json for the debug Navigation map"
        classpath = tool
        mainClass.set("com.programmersbox.navgraph.MainKt")
        this.appName.set(appName)
        repoRoot.set(rootProject.layout.projectDirectory)
        sourceFiles.from(
            rootProject.fileTree(rootDir) {
                include("**/*.kt", "navgraph/overrides/*.navgraph")
                exclude("**/build/**", "**/.gradle/**", ".git/**", ".superpowers/**", ".claude/**")
            }
        )
        outputDir.set(layout.buildDirectory.dir("generated/navgraph"))
        val out = outputDir
        val root = repoRoot
        val app = this.appName
        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "--app", app.get(),
                "--root", root.get().asFile.absolutePath,
                "--out", out.get().file("navgraph.json").asFile.absolutePath,
            )
        })
    }
}
