object AppInfo {
    const val otakuVersionName = "32.1.2"
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
    const val compileVersion = 37
    const val minimumSdk = 28
    const val targetSdk = 37
}