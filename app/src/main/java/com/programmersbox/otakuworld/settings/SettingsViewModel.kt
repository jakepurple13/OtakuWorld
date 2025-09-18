package com.programmersbox.otakuworld.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.BuildConfig
import com.programmersbox.otakuworld.info.AppCheck
import com.programmersbox.otakuworld.providers.App
import com.programmersbox.otakuworld.providers.OtakuProvider
import com.programmersbox.otakuworld.providers.Provider
import com.programmersbox.otakuworld.repository.OtakuRepository

class SettingsViewModel(
    private val otakuProvider: OtakuProvider,
    private val otakuRepository: OtakuRepository,
    private val appInfo: AppInfo,
) : ViewModel() {
    var hasApps by mutableStateOf(
        AppCheck(
            hasAnimeWorld = null,
            hasMangaWorld = null,
            hasNovelWorld = null
        )
    )

    val animeWorld = OtakuSettingsItem(
        app = App.AnimeWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )
    val mangaWorld = OtakuSettingsItem(
        app = App.MangaWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )
    val novelWorld = OtakuSettingsItem(
        app = App.NovelWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )

    fun checkForApps() {
        hasApps = AppCheck(
            hasAnimeWorld = otakuRepository.hasAnimeWorld(),
            hasMangaWorld = otakuRepository.hasMangaWorld(),
            hasNovelWorld = otakuRepository.hasNovelWorld()
        )
    }
}

class OtakuSettingsItem(
    val app: App,
    val appProvider: Provider,
    otakuProvider: OtakuProvider,
) {
    val favoritePermission: String = otakuProvider.favoritesPermissions {
        appType = app
        provider = appProvider
    }

    val listsPermission: String = otakuProvider.listPermissions {
        appType = app
        provider = appProvider
    }

    val favoritesUri = when (app) {
        App.AnimeWorld -> BuildConfig.AnimeWorld_FAVORITES_URI
        App.MangaWorld -> BuildConfig.MangaWorld_FAVORITES_URI
        App.NovelWorld -> BuildConfig.NovelWorld_FAVORITES_URI
    }

    val listsUri = when (app) {
        App.AnimeWorld -> BuildConfig.AnimeWorld_LISTS_URI
        App.MangaWorld -> BuildConfig.MangaWorld_LISTS_URI
        App.NovelWorld -> BuildConfig.NovelWorld_LISTS_URI
    }
}