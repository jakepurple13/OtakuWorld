package com.programmersbox.kmpuiviews.presentation.settings.prerelease

actual val currentAssetPlatform: AssetPlatform = System.getProperty("os.name").orEmpty().lowercase().let {
    when {
        "mac" in it || "darwin" in it -> AssetPlatform.MacOS
        "win" in it -> AssetPlatform.Windows
        "nux" in it || "nix" in it -> AssetPlatform.Linux
        else -> AssetPlatform.Other
    }
}
