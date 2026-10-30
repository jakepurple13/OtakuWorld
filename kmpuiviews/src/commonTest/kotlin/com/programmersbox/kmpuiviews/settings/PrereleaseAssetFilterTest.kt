package com.programmersbox.kmpuiviews.settings

import com.programmersbox.kmpuiviews.presentation.settings.prerelease.AssetPlatform
import com.programmersbox.kmpuiviews.presentation.settings.prerelease.isPrereleaseAssetForPlatform
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrereleaseAssetFilterTest {
    @Test
    fun androidShowsAllApks() {
        assertTrue(isPrereleaseAssetForPlatform("animeworld-noFirebase-release.apk", "MangaWorld", AssetPlatform.Android))
        assertFalse(isPrereleaseAssetForPlatform("MangaWorld-33.0.0.dmg", "MangaWorld", AssetPlatform.Android))
        assertFalse(isPrereleaseAssetForPlatform("mangaworld-mac.yml", "MangaWorld", AssetPlatform.Android))
    }

    @Test
    fun macShowsOnlyThisAppsDmgAndZip() {
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.dmg", "MangaWorld", AssetPlatform.MacOS))
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.zip", "MangaWorld", AssetPlatform.MacOS))
        assertFalse(isPrereleaseAssetForPlatform("AnimeWorld-33.0.0-arm64.dmg", "MangaWorld", AssetPlatform.MacOS))
        assertFalse(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-arm64.zip.blockmap", "MangaWorld", AssetPlatform.MacOS))
    }

    @Test
    fun windowsShowsExe() {
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-x64.exe", "MangaWorld", AssetPlatform.Windows))
    }

    @Test
    fun linuxShowsDebAndAppImage() {
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-amd64.deb", "MangaWorld", AssetPlatform.Linux))
        assertTrue(isPrereleaseAssetForPlatform("MangaWorld-33.0.0-x86_64.AppImage", "MangaWorld", AssetPlatform.Linux))
        assertFalse(isPrereleaseAssetForPlatform("mangaworld-linux.yml", "MangaWorld", AssetPlatform.Linux))
    }

    @Test
    fun otherShowsEverything() {
        assertTrue(isPrereleaseAssetForPlatform("anything.bin", "MangaWorld", AssetPlatform.Other))
    }
}
