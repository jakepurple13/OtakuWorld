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
    val tool = configurations.create("navGraphTool") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
    dependencies.add(tool.name, dependencies.project(mapOf("path" to ":navgraph")))

    val task = tasks.register<GenerateNavGraphTask>("generateNavGraph") {
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

    extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
        // Debug builds only: published noFirebase release APKs must not carry repo paths or the debug screen.
        if (variant.buildType == "debug") {
            variant.sources.assets?.addGeneratedSourceDirectory(task, GenerateNavGraphTask::outputDir)
        }
    }
}
