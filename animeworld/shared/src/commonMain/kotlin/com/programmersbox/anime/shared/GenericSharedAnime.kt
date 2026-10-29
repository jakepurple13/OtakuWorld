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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.programmersbox.anime.shared.videochoice.VideoSourceModel
import com.programmersbox.datastore.ColorBlindnessType
import com.programmersbox.datastore.NewSettingsHandling
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.BuildType
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.firstOrNull
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

    var isLoadingChapter by mutableStateOf(false)
        private set

    override suspend fun chapterOnClick(
        model: KmpChapterModel,
        allChapters: List<KmpChapterModel>,
        infoModel: KmpInfoModel,
        navController: NavigationActions,
    ) {
        if (isLoadingChapter) return
        isLoadingChapter = true
        try {
            val storages = model.getChapterInfo().firstOrNull().orEmpty()

            when {
                storages.size == 1 -> playResolved(navController, storages.first(), model, infoModel)
                storages.isNotEmpty() -> VideoSourceModel.showVideoSources = VideoSourceModel(
                    c = storages,
                    infoModel = infoModel,
                    isStreaming = true,
                    model = model,
                )
                else -> Unit
            }
        } finally {
            isLoadingChapter = false
        }
    }

    /**
     * Download counterpart of [chapterOnClick]: same loading guard, one storage downloads directly,
     * several go to the picker, none or an error calls [onNone].
     */
    suspend fun downloadOnClick(
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
        onNone: suspend () -> Unit,
        download: suspend (KmpStorage) -> Unit,
    ) {
        if (isLoadingChapter) return
        isLoadingChapter = true
        try {
            val storages = try {
                model.getChapterInfo().firstOrNull().orEmpty()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }

            when {
                storages.size == 1 -> download(storages.first())
                storages.isNotEmpty() -> VideoSourceModel.showVideoSources = VideoSourceModel(
                    c = storages,
                    infoModel = infoModel,
                    isStreaming = false,
                    model = model,
                )
                else -> onNone()
            }
        } finally {
            isLoadingChapter = false
        }
    }

    /** Lets the source resolve a lazily-described stream first; on failure nothing plays. */
    suspend fun playResolved(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
    ) {
        val resolved = try {
            model.source.resolve(storage)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            onResolveFailed(storage, e)
            return
        }
        playOrCast(navController, resolved, model, infoModel)
    }

    /** Called when resolving a stream fails, so the platform can tell the user. */
    open fun onResolveFailed(storage: KmpStorage, error: Throwable) {}

    abstract fun playOrCast(
        navController: NavigationActions,
        storage: KmpStorage,
        model: KmpChapterModel,
        infoModel: KmpInfoModel,
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
                key = { i, it -> "${it.url}$i" },
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
                        if (favorites.any { f -> f.url == it.url }) {
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
