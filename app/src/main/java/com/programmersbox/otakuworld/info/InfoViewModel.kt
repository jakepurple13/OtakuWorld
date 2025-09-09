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

        setupApp(
            flow = appCheck.filter { it.hasAnimeWorld },
            app = App.AnimeWorld,
            otakuItem = animeWorld
        )


        setupApp(
            flow = appCheck.filter { it.hasMangaWorld },
            app = App.MangaWorld,
            otakuItem = mangaWorld
        )


        setupApp(
            flow = appCheck.filter { it.hasNovelWorld },
            app = App.NovelWorld,
            otakuItem = novelWorld
        )

    }

    private fun setupApp(
        flow: Flow<AppCheck>,
        app: App,
        otakuItem: OtakuItem,
    ) {
        flow.flatMapMerge {
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

        flow.flatMapMerge {
            otakuProvider
                .listsBuilder {
                    appType = app
                    provider = appInfo.provider
                }
                .getAllCustomListsFlow(appInfo.context)
        }
            .catch {
                it.printStackTrace()
                emit(emptyList())
            }
            .onEach {
                otakuItem.list.clear()
                otakuItem.list.addAll(it)
            }
            .launchIn(viewModelScope)
    }

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

    val listsUri = when (app) {
        App.AnimeWorld -> BuildConfig.AnimeWorld_LISTS_URI
        App.MangaWorld -> BuildConfig.MangaWorld_LISTS_URI
        App.NovelWorld -> BuildConfig.NovelWorld_LISTS_URI
    }
}