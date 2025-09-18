package com.programmersbox.otakuworld.repository

import android.content.Context
import android.graphics.drawable.Drawable
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.BuildConfig
import com.programmersbox.otakuworld.providers.App

class OtakuRepository(
    private val context: Context,
    private val appInfo: AppInfo,
) {
    fun hasAnimeWorld(): OtakuInfo? = hasApp(App.AnimeWorld)
    fun hasMangaWorld(): OtakuInfo? = hasApp(App.MangaWorld)
    fun hasNovelWorld(): OtakuInfo? = hasApp(App.NovelWorld)

    private fun hasApp(app: App) = runCatching {
        val packageName = when (app) {
            App.AnimeWorld -> BuildConfig.ANIMEWORLD_PACKAGE
            App.MangaWorld -> BuildConfig.MANGAWORLD_PACKAGE
            App.NovelWorld -> BuildConfig.NOVELWORLD_PACKAGE
        }
        OtakuInfo(
            context.packageManager.getApplicationIcon(packageName),
            context.packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        )
    }
        .onFailure { it.printStackTrace() }
        .getOrNull()
}

data class OtakuInfo(
    val drawable: Drawable,
    val version: String,
)