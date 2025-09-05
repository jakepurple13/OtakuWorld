package com.programmersbox.otakuworld.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.programmersbox.favoritesdatabase.CustomList
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.otakuworld.App
import com.programmersbox.otakuworld.AppInfo
import com.programmersbox.otakuworld.BuildConfig
import com.programmersbox.otakuworld.OtakuProvider
import com.programmersbox.otakuworld.Provider
import com.programmersbox.otakuworld.repository.OtakuRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class InfoViewModel(
    private val otakuProvider: OtakuProvider,
    private val otakuRepository: OtakuRepository,
    private val appInfo: AppInfo,
) : ViewModel() {
    var hasApps by mutableStateOf(
        AppCheck(
            hasAnimeWorld = false,
            hasMangaWorld = false,
            hasNovelWorld = false
        )
    )

    val animeWorld = OtakuItem(
        app = App.AnimeWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )
    val mangaWorld = OtakuItem(
        app = App.MangaWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )
    val novelWorld = OtakuItem(
        app = App.NovelWorld,
        appProvider = appInfo.provider,
        otakuProvider = otakuProvider
    )

    init {
        val appCheck = snapshotFlow { hasApps }

        appCheck
            .filter { it.hasAnimeWorld }
            .setupApp(
                app = App.AnimeWorld,
                otakuItem = animeWorld
            )

        appCheck
            .filter { it.hasMangaWorld }
            .setupApp(
                app = App.MangaWorld,
                otakuItem = mangaWorld
            )

        appCheck
            .filter { it.hasNovelWorld }
            .setupApp(
                app = App.NovelWorld,
                otakuItem = novelWorld
            )

    }

    private fun Flow<AppCheck>.setupApp(
        app: App,
        otakuItem: OtakuItem,
    ) = flatMapMerge {
        otakuProvider
            .favoritesBuilder {
                appType = app
                provider = appInfo.provider
            }
            .getAllFavoritesAsListFlow(appInfo.context)
    }
        .catch {
            it.printStackTrace()
            emit(emptyList())
        }
        .onEach {
            otakuItem.favorites.clear()
            otakuItem.favorites.addAll(it)
        }
        .launchIn(viewModelScope)

    fun checkForApps() {
        hasApps = AppCheck(
            hasAnimeWorld = otakuRepository.hasAnimeWorld(),
            hasMangaWorld = otakuRepository.hasMangaWorld(),
            hasNovelWorld = otakuRepository.hasNovelWorld()
        )
    }

}

data class AppCheck(
    val hasAnimeWorld: Boolean,
    val hasMangaWorld: Boolean,
    val hasNovelWorld: Boolean,
)

class OtakuItem(
    val app: App,
    val appProvider: Provider,
    otakuProvider: OtakuProvider,
) {
    val favorites = mutableStateListOf<DbModel>()
    val list = mutableStateListOf<CustomList>()

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
}