package plugins

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project

class AndroidLibraryPlugin : AndroidPluginBase<LibraryExtension>(LibraryExtension::class) {

    override fun Project.projectSetup() {
        pluginManager.apply("com.android.library")
    }

    override fun LibraryExtension.androidConfig(project: Project) {
        lint {
            checkReleaseBuilds = false
        }
    }
}
