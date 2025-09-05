package com.programmersbox.otakuworld.repository

import android.content.Context
import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.OtakuProvider

class OtakuRepository(
    private val context: Context,
    private val appInfo: AppInfo,
) {
    fun hasAnimeWorld(): Boolean = hasApp(App.AnimeWorld)
    fun hasMangaWorld(): Boolean = hasApp(App.MangaWorld)
    fun hasNovelWorld(): Boolean = hasApp(App.NovelWorld)

    private fun hasApp(app: App) = runCatching {
        context.packageManager.getPackageInfo(
            OtakuProvider.OtakuBuilder()
                .setPackage(app)
                .setProvider(appInfo.provider)
                .build(),
            0
        )
    }
        .onSuccess { println(it.toString()) }
        .onFailure { it.printStackTrace() }
        .getOrNull() != null

}