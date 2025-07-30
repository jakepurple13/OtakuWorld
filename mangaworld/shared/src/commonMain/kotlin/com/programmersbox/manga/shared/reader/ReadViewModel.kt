package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.fireListener
import com.programmersbox.manga.shared.ChapterHolder
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private const val FAVORITE_CHECK = 2

class ReadViewModel(
    mangaReader: MangaReader,
    private val chapterHolder: ChapterHolder,
    private val favoritesRepository: FavoritesRepository,
    itemListenerFirebase: KmpFirebaseConnection.KmpFirebaseListener,
    private val heatMapDao: HeatMapDao,
    private val exceptionDao: ExceptionDao,
) : ViewModel() {

    val isDownloaded: Boolean = mangaReader.downloaded
    val headers = mutableStateMapOf<String, String>()

    val model: Flow<List<String>>? = chapterHolder.chapterModel
        ?.getChapterInfo()
        ?.map {
            headers.putAll(it.flatMap { h -> h.headers.toList() })
            it.mapNotNull(KmpStorage::link)
        }

    val filePath: PlatformFile? = runCatching { mangaReader.filePath?.let { PlatformFile(it) } }.getOrNull()
    val modelPath: Flow<List<String>>? = if (isDownloaded && filePath != null) {
        flow {
            filePath
                .list()
                .sortedBy { f -> f.name.split(".").first().toInt() }
                .fastMap { it.toKotlinxIoPath() }
                .fastMap { it.toString() }
                .let { emit(it) }
        }
            .catch { emit(emptyList()) }
            .flowOn(Dispatchers.IO)
    } else {
        model
    }

    companion object {
        fun navigateToMangaReader(
            navController: NavigationActions,
            mangaTitle: String? = null,
            mangaUrl: String? = null,
            mangaInfoUrl: String? = null,
            downloaded: Boolean = false,
            filePath: String? = null,
        ) {
            navController.navigate(
                MangaReader(
                    mangaTitle = mangaTitle,
                    mangaUrl = mangaUrl,
                    mangaInfoUrl = mangaInfoUrl,
                    downloaded = downloaded,
                    filePath = filePath
                )
            )// { launchSingleTop = true }
        }
    }

    @Serializable
    data class MangaReader(
        val mangaTitle: String? = null,
        val mangaUrl: String? = null,
        val mangaInfoUrl: String? = null,
        val downloaded: Boolean,
        val filePath: String? = null,
    ) : NavKey

    val title by lazy { mangaReader.mangaTitle ?: "" }

    var list by mutableStateOf<List<KmpChapterModel>>(emptyList())

    private val mangaUrl by lazy { mangaReader.mangaInfoUrl ?: "" }

    var currentChapter: Int by mutableIntStateOf(0)

    val pageList = mutableStateListOf<String>()
    var isLoadingPages by mutableStateOf(false)

    val currentChapterModel by derivedStateOf { list.getOrNull(currentChapter) }

    private val itemListener = fireListener(itemListener = itemListenerFirebase)
    var addToFavorites by mutableStateOf(FavoriteChecker(false, 0))

    data class FavoriteChecker(val hasShown: Boolean, val count: Int, val isFavorite: Boolean = false) {
        val shouldShow: Boolean = !hasShown && count > FAVORITE_CHECK && !isFavorite
    }

    init {
        val url = chapterHolder.chapterModel?.url ?: mangaReader.mangaUrl
        list = chapterHolder.chapters.orEmpty()
        currentChapter = list.indexOfFirst { l -> l.url == url }.coerceIn(0, list.lastIndex)

        loadPages(modelPath)

        favoritesRepository
            .isFavorite(
                url = mangaUrl,
                fireListenerClosable = itemListener
            )
            .dispatchIo()
            .onEach { addToFavorites = addToFavorites.copy(isFavorite = it) }
            .launchIn(viewModelScope)
    }

    var showInfo by mutableStateOf(true)

    var firstScroll by mutableStateOf(true)

    fun addChapterToWatched(newChapter: Int, chapter: () -> Unit) {
        currentChapter = newChapter
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        list.getOrNull(newChapter)?.let { item ->
            ChapterWatched(item.url, item.name, mangaUrl)
                .let {
                    viewModelScope.launch {
                        if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                            favoritesRepository.addWatched(it)
                        }
                        withContext(Dispatchers.Main) { chapter() }
                    }
                }

            item
                .getChapterInfo()
                .map { it.mapNotNull(KmpStorage::link) }
                .let { loadPages(it) }
        }
    }

    fun addToFavorites() {
        addToFavorites = addToFavorites.copy(hasShown = true)
        viewModelScope.launch {
            currentChapterModel
                ?.source
                ?.getSourceByUrlFlow(mangaUrl)
                ?.firstOrNull()
                ?.toDbModel()
                ?.let { favoritesRepository.addFavorite(it) }
        }
    }

    private fun loadPages(modelPath: Flow<List<String>>?) {
        modelPath
            ?.onStart {
                isLoadingPages = true
                pageList.clear()
            }
            ?.catch { exceptionDao.insertException(it) }
            ?.onEach { pageList.addAll(it) }
            ?.onEach { heatMapDao.upsertHeatMap() }
            ?.onCompletion { isLoadingPages = false }
            ?.launchIn(viewModelScope)
    }

    fun refresh() {
        headers.clear()
        loadPages(
            list.getOrNull(currentChapter)
                ?.getChapterInfo()
                ?.map {
                    headers.putAll(it.flatMap { h -> h.headers.toList() })
                    it.mapNotNull(KmpStorage::link)
                }
        )
    }

    override fun onCleared() {
        super.onCleared()
        chapterHolder.chapterModel = null
        chapterHolder.chapters = null
    }
}