object AppInfo {
    const val otakuVersionName = "32.1.0"
    val versionCode by lazy {
        if (System.getenv("CI") != null) {
            runCatching { System.getenv("GITHUB_RUN_NUMBER").toInt() }
                .getOrNull()
        } else {
            null
        } ?: 2
    }
    const val compileVersion = 36
    const val minimumSdk = 28
    const val targetSdk = 36
}