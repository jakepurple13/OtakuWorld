package com.programmersbox.kmpuiviews.presentation.settings.prerelease

enum class AssetPlatform { Android, MacOS, Windows, Linux, Other }

expect val currentAssetPlatform: AssetPlatform

fun isPrereleaseAssetForPlatform(name: String, appName: String, platform: AssetPlatform): Boolean {
    val extensions = when (platform) {
        AssetPlatform.Other -> return true
        AssetPlatform.Android -> return name.endsWith(".apk", ignoreCase = true)
        AssetPlatform.MacOS -> listOf(".dmg", ".zip")
        AssetPlatform.Windows -> listOf(".exe")
        AssetPlatform.Linux -> listOf(".deb", ".appimage")
    }
    return name.startsWith(appName, ignoreCase = true) &&
            extensions.any { name.endsWith(it, ignoreCase = true) }
}
