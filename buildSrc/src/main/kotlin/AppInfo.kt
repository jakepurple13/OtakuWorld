import com.android.build.api.dsl.CompileSdkSpec
import org.gradle.api.Project

object AppInfo {
    const val otakuVersionName = "33.1.0"
    val versionBump = 5000
    val versionCode by lazy {
        val code = if (System.getenv("CI") != null) {
            runCatching { System.getenv("GITHUB_RUN_NUMBER").toInt() + versionBump }
                .getOrNull()
        } else {
            null
        } ?: 2

        println("Version code: $code")

        code
    }

    const val compileVersion = "37.2"
    const val minimumSdk = 28
    const val targetSdk = 37

    fun setCustomCompileSdkVersion(spec: CompileSdkSpec) {
        with(spec) {
            version = release(37) {
                minorApiLevel = 2
            }
        }
    }

    /**
     * iOS targets are opt-in to keep configuration and IDE sync fast.
     * Enable with `-Potaku.includeIos=true` or `otaku.includeIos=true` in ~/.gradle/gradle.properties.
     */
    fun includeIos(project: Project): Boolean =
        project.providers.gradleProperty("otaku.includeIos").orNull?.toBoolean() == true
}