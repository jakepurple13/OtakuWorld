package com.programmersbox.otakuworld.repository

import android.content.Context
import android.graphics.drawable.Drawable
import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.OtakuProvider

class OtakuRepository(
    private val context: Context,
    private val appInfo: AppInfo,
) {
    fun hasAnimeWorld(): OtakuInfo? = hasApp(App.AnimeWorld)
    fun hasMangaWorld(): OtakuInfo? = hasApp(App.MangaWorld)
    fun hasNovelWorld(): OtakuInfo? = hasApp(App.NovelWorld)

    private fun hasApp(app: App) = runCatching {
        val packageName = OtakuProvider.OtakuBuilder()
            .setPackage(app)
            .setProvider(appInfo.provider)
            .build()
        context.packageManager.getApplicationIcon(packageName)
    }
        .onFailure { it.printStackTrace() }
        .mapCatching { OtakuInfo(it) }
        .getOrNull()
}

data class OtakuInfo(
    val drawable: Drawable,
)