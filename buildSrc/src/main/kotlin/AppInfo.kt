object AppInfo {
    const val otakuVersionName = "32.1.2"
    val versionCode by lazy {
        val code = if (System.getenv("CI") != null) {
            runCatching { System.getenv("GITHUB_RUN_NUMBER").toInt() }
                .getOrNull()
        } else {
            null
        } ?: 2

        println("Version code: $code")

        code
    }
    const val compileVersion = 36
    const val minimumSdk = 28
    const val targetSdk = 36
}