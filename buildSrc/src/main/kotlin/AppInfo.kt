import com.android.build.api.dsl.CompileSdkSpec

object AppInfo {
    const val otakuVersionName = "33.0.0"
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
}