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
        private const val WINDOW_SIZE = 3

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

    val pageItems = mutableStateListOf<PageItem>()
    var loadingChapters by mutableStateOf(emptySet<Int>())
    val isLoadingPages: Boolean get() = loadingChapters.isNotEmpty()

    private val loadedChapterWindow = ArrayDeque<Int>()

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

        loadInitialChapter()

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
            viewModelScope.launch {
                if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                    favoritesRepository.addWatched(ChapterWatched(item.url, item.name, mangaUrl))
                }
                withContext(Dispatchers.Main) { chapter() }
            }

            loadedChapterWindow.clear()
            loadedChapterWindow.addLast(newChapter)
            item.getChapterInfo()
                .map { storages ->
                    headers.putAll(storages.flatMap { h -> h.headers.toList() })
                    storages.mapNotNull(KmpStorage::link)
                }
                .onStart {
                    loadingChapters = loadingChapters + newChapter
                    pageItems.clear()
                }
                .catch { exceptionDao.insertException(it) }
                .onEach { urls ->
                    pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, newChapter, i) })
                    heatMapDao.upsertHeatMap()
                }
                .onCompletion { loadingChapters = loadingChapters - newChapter }
                .launchIn(viewModelScope)
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

    private fun loadInitialChapter() {
        val flow = modelPath ?: return
        val chapterIndex = currentChapter
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(chapterIndex)
        flow
            .onStart {
                loadingChapters = loadingChapters + chapterIndex
                pageItems.clear()
            }
            .catch { exceptionDao.insertException(it) }
            .onEach { urls ->
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterIndex, i) })
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - chapterIndex }
            .launchIn(viewModelScope)
    }

    fun appendChapter(chapterListIndex: Int) {
        if (chapterListIndex < 0 || chapterListIndex > list.lastIndex) return
        if (chapterListIndex in loadedChapterWindow) return
        loadedChapterWindow.addLast(chapterListIndex)
        val fromChapterListIndex = loadedChapterWindow[loadedChapterWindow.size - 2]

        viewModelScope.launch {
            // Evict oldest loaded chapter if window is exceeded
            while (loadedChapterWindow.size > WINDOW_SIZE) {
                val dropped = loadedChapterWindow.removeFirst()
                val firstKeptIdx = pageItems.indexOfFirst { item ->
                    when (item) {
                        is PageItem.Page -> item.chapterListIndex != dropped
                        is PageItem.ChapterTransition -> item.fromChapterListIndex != dropped
                    }
                }
                if (firstKeptIdx > 0) pageItems.subList(0, firstKeptIdx).clear()
            }

            loadingChapters = loadingChapters + chapterListIndex

            pageItems.add(PageItem.ChapterTransition(fromChapterListIndex, chapterListIndex))

            list.getOrNull(chapterListIndex)
                ?.getChapterInfo()
                ?.map { storages ->
                    headers.putAll(storages.flatMap { h -> h.headers.toList() })
                    storages.mapNotNull(KmpStorage::link)
                }
                ?.catch { exceptionDao.insertException(it) }
                ?.onEach { urls ->
                    pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i) })
                    heatMapDao.upsertHeatMap()
                }
                ?.onCompletion {
                    loadingChapters = loadingChapters - chapterListIndex
                    list.getOrNull(chapterListIndex)?.let { item ->
                        if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                            favoritesRepository.addWatched(
                                ChapterWatched(item.url, item.name, mangaUrl)
                            )
                        }
                    }
                    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
                }
                ?.launchIn(viewModelScope)
        }
    }

    suspend fun prependChapter(chapterListIndex: Int): Int {
        if (chapterListIndex < 0 || chapterListIndex > list.lastIndex) return 0
        if (chapterListIndex in loadedChapterWindow) return 0

        // Evict newest loaded chapter if window is full
        if (loadedChapterWindow.size >= WINDOW_SIZE) {
            val dropped = loadedChapterWindow.removeLast()
            val removeFrom = pageItems.indexOfFirst {
                it is PageItem.ChapterTransition && it.toChapterListIndex == dropped
            }.takeIf { it >= 0 } ?: pageItems.indexOfFirst {
                it is PageItem.Page && it.chapterListIndex == dropped
            }
            if (removeFrom >= 0) {
                while (pageItems.size > removeFrom) pageItems.removeAt(removeFrom)
            }
        }

        val toChapterListIndex = loadedChapterWindow.first()
        loadedChapterWindow.addFirst(chapterListIndex)
        loadingChapters = loadingChapters + chapterListIndex

        val newPages = mutableListOf<PageItem>()
        list.getOrNull(chapterListIndex)
            ?.getChapterInfo()
            ?.map { storages ->
                headers.putAll(storages.flatMap { h -> h.headers.toList() })
                storages.mapNotNull(KmpStorage::link)
            }
            ?.catch { exceptionDao.insertException(it) }
            ?.collect { urls ->
                newPages.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i) })
                heatMapDao.upsertHeatMap()
            }

        loadingChapters = loadingChapters - chapterListIndex
        list.getOrNull(chapterListIndex)?.let { item ->
            if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                favoritesRepository.addWatched(
                    ChapterWatched(item.url, item.name, mangaUrl)
                )
            }
        }

        val insertedItems: List<PageItem> = newPages + PageItem.ChapterTransition(chapterListIndex, toChapterListIndex)
        pageItems.addAll(0, insertedItems)
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        return insertedItems.size
    }

    fun updateCurrentChapter(chapterListIndex: Int) {
        if (chapterListIndex == currentChapter) return
        currentChapter = chapterListIndex
    }

    fun refresh() {
        headers.clear()
        val chapterIndex = currentChapter
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(chapterIndex)
        list.getOrNull(chapterIndex)
            ?.getChapterInfo()
            ?.map { storages ->
                headers.putAll(storages.flatMap { h -> h.headers.toList() })
                storages.mapNotNull(KmpStorage::link)
            }
            ?.onStart {
                loadingChapters = loadingChapters + chapterIndex
                pageItems.clear()
            }
            ?.catch { exceptionDao.insertException(it) }
            ?.onEach { urls ->
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterIndex, i) })
                heatMapDao.upsertHeatMap()
            }
            ?.onCompletion { loadingChapters = loadingChapters - chapterIndex }
            ?.launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        chapterHolder.chapterModel = null
        chapterHolder.chapters = null
    }
}