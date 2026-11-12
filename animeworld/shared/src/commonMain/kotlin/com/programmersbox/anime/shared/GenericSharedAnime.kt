package com.programmersbox.anime.shared

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.episodeload.EpisodeAction
import com.programmersbox.anime.shared.episodeload.EpisodeLoader
import com.programmersbox.anime.shared.episodeload.downloadRowStates
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.AppUpdate
import com.programmersbox.kmpuiviews.painterLogo
import com.programmersbox.kmpuiviews.presentation.components.M3CoverCard
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.sharedItemKey
import com.programmersbox.kmpuiviews.presentation.components.colorFilterBlind
import com.programmersbox.kmpuiviews.presentation.components.placeholder.M3PlaceHolderCoverCard
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.utils.AppConfig
import com.programmersbox.kmpuiviews.utils.ComponentState
import com.programmersbox.kmpuiviews.utils.adaptiveGridCell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

abstract class GenericSharedAnime(
    protected val appConfig: AppConfig,
) : KmpGenericInfo {

    override val sourceType: String get() = "anime"

    override val apkString: AppUpdate.AppUpdates.() -> String?
        get() = {
            when (appConfig.buildType) {
                BuildType.NoFirebase -> animeNoFirebaseFile
                BuildType.Full -> animeFile
            }
        }

    val episodeLoader = EpisodeLoader()

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
        seedColor: Int?,
    ) {
        episodeLoader.load(
            action = EpisodeAction.Play,
            model = model,
            infoModel = infoModel,
            // Callers run this on IO; navigating mutates the back stack, which belongs on Main.
            onResolved = { withContext(Dispatchers.Main) { playOrCast(navController, it, model, infoModel, seedColor) } },
        )
    }

    override fun observeChapterDownloadStates(
        chapters: List<KmpChapterModel>,
        mangaTitle: String,
    ): Flow<Map<String, ChapterDownloadUiState>> = snapshotFlow { downloadRowStates(episodeLoader.state) }

    /** Download counterpart of [chapterOnClick]; call from a coroutine (it suspends until the load finishes). */
    protected suspend fun loadForDownload(model: KmpChapterModel, infoModel: KmpInfoModel) {
        if (!canStartDownload()) return
        episodeLoader.load(
            action = EpisodeAction.Download,
            model = model,
            infoModel = infoModel,
            onResolved = { enqueueResolved(it, model, infoModel) },
            onDownloadFailed = { storage, error ->
                if (storage == null) {
                    onNoDownloadSources(model)
                } else {
                    error?.printStackTrace()
                    onResolveFailed(storage, error ?: IllegalStateException("No link"))
                }
            },
        )
    }

    /** Platform hand-off after a successful download resolve. */
    protected abstract suspend fun enqueueResolved(storage: KmpStorage, model: KmpChapterModel, infoModel: KmpInfoModel)

    /** Download with zero sources. */
    protected abstract fun onNoDownloadSources(model: KmpChapterModel)

    /** Checked before a download load starts; return false to abort (e.g. missing permission). */
    protected open fun canStartDownload(): Boolean = true

    /** Called when resolving a stream fails, so the platform can tell the user. */
    open fun onResolveFailed(storage: KmpStorage, error: Throwable) {}

    abstract fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        seedColor: Int?,
    )

    @Composable
    override fun ComposeShimmerItem() {
        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) { items(10) { M3PlaceHolderCoverCard(placeHolder = painterLogo()) } }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ItemListView(
        list: List<KmpItemModel>,
        favorites: List<DbModel>,
        listState: LazyGridState,
        onLongPress: (KmpItemModel, ComponentState) -> Unit,
        modifier: Modifier,
        paddingValues: PaddingValues,
        onClick: (KmpItemModel) -> Unit,
    ) {
        val colorBlindness: ColorBlindnessType by koinInject<NewSettingsHandling>().rememberColorBlindType()
        val colorFilter by remember { derivedStateOf { colorFilterBlind(colorBlindness) } }
        val keys = remember(list) { gridItemKeys(list.map { it.url }) }
        val favoriteUrls = remember(favorites) { favorites.mapTo(HashSet()) { it.url } }

        LazyVerticalGrid(
            columns = adaptiveGridCell(),
            state = listState,
            contentPadding = paddingValues,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                list,
                key = { i, _ -> keys[i] },
                contentType = { _, _ -> "item" }
            ) { _, it ->
                M3CoverCard(
                    onLongPress = { c -> onLongPress(it, c) },
                    imageUrl = it.imageUrl,
                    name = it.title,
                    sharedItemKey = it.sharedItemKey(),
                    headers = it.extras,
                    //placeHolder = { painterLogo() },
                    placeHolder = { rememberVectorPainter(Icons.Default.BrokenImage) },
                    favoriteIcon = {
                        if (it.url in favoriteUrls) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            Icon(
                                Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        }
                    },
                    colorFilter = colorFilter,
                    modifier = Modifier.animateItem()
                ) { onClick(it) }
            }
        }
    }
}

/** Grid keys for [urls]: the url itself, so keys don't shift on insert; only repeats get a count so they stay unique. */
internal fun gridItemKeys(urls: List<String>): List<String> {
    val seen = mutableMapOf<String, Int>()
    return urls.map { url ->
        val count = seen[url] ?: 0
        seen[url] = count + 1
        if (count == 0) url else "$url\u0000$count"
    }
}
