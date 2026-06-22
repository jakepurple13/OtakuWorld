package com.programmersbox.manga.shared.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastAny
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
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
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
    private val mangaDownloadManager: MangaDownloadManager,
) : ViewModel() {

    val headers = mutableStateMapOf<String, String>()

    val currentChapterIsDownloaded: Boolean
        get() = pageItems
            .filterIsInstance<PageItem.Page>()
            .firstOrNull { it.chapterListIndex == currentChapter }
            ?.isDownloaded ?: false

    private fun chapterFlow(chapter: KmpChapterModel): Flow<List<String>> {
        val localPath = mangaDownloadManager.getDownloadedChapterPath(chapter, title)
        return if (localPath != null) {
            flow {
                PlatformFile(localPath)
                    .list()
                    .sortedBy { f -> f.name.split(".").first().toIntOrNull() ?: 0 }
                    .fastMap { sanitizePath(it.toKotlinxIoPath().toString()) }
                    .let { emit(it) }
            }
                .catch { emit(emptyList()) }
                .flowOn(Dispatchers.IO)
        } else {
            chapter
                .getChapterInfo()
                .map { storages ->
                    headers.putAll(storages.flatMap { h -> h.headers.toList() })
                    storages.mapNotNull(KmpStorage::link)
                }
        }
    }

    private fun downloadedChapterFlow(filePath: String): Flow<List<String>> =
        flow {
            PlatformFile(filePath)
                .list()
                .sortedBy { f -> f.name.split(".").first().toIntOrNull() ?: 0 }
                .fastMap { sanitizePath(it.toKotlinxIoPath().toString()) }
                .let { emit(it) }
        }
            .catch { e -> exceptionDao.insertException(e); emit(emptyList()) }
            .flowOn(Dispatchers.IO)

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

    private var downloadedPaths: List<String> = emptyList()
    val isDownloadedPathsMode: Boolean get() = downloadedPaths.isNotEmpty()
    val chapterCount: Int get() = if (isDownloadedPathsMode) downloadedPaths.size else list.size

    fun chapterName(index: Int): String? =
        if (isDownloadedPathsMode) downloadedPaths.getOrNull(index)?.substringAfterLast("/")
        else list.getOrNull(index)?.name

    val currentChapterModel by derivedStateOf { list.getOrNull(currentChapter) }

    private val itemListener = fireListener(itemListener = itemListenerFirebase)
    var addToFavorites by mutableStateOf(FavoriteChecker(false, 0))

    data class FavoriteChecker(val hasShown: Boolean, val count: Int, val isFavorite: Boolean = false) {
        val shouldShow: Boolean = !hasShown && count > FAVORITE_CHECK && !isFavorite
    }

    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    init {
        val url = chapterHolder.chapterModel?.url ?: mangaReader.mangaUrl
        list = chapterHolder.chapters.orEmpty()
        currentChapter = list
            .indexOfFirst { l -> l.url == url }
            .coerceIn(0, list.lastIndex.coerceAtLeast(1))

        val paths = chapterHolder.downloadedChapterPaths
        chapterHolder.downloadedChapterPaths = null
        if (paths != null && paths.isNotEmpty()) {
            downloadedPaths = paths
            currentChapter = paths.indexOf(mangaReader.filePath).coerceAtLeast(0)
            loadDownloadedChapterAtIndex(currentChapter)
        } else if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
            loadDirectFromPath(mangaReader.filePath)
        } else {
            loadInitialChapter()
        }

        favoritesRepository
            .isFavorite(
                url = mangaUrl,
                fireListenerClosable = itemListener
            )
            .dispatchIo()
            .onEach { addToFavorites = addToFavorites.copy(isFavorite = it) }
            .launchIn(viewModelScope)

        favoritesRepository
            .getChaptersLocal(mangaUrl)
            .onEach { chapters = it }
            .launchIn(viewModelScope)
    }

    var showInfo by mutableStateOf(true)

    var firstScroll by mutableStateOf(true)

    fun loadPreviousChapter(chapter: () -> Unit) {
        loadChapter(++currentChapter, chapter)
    }

    fun loadNextChapter(chapter: () -> Unit) {
        loadChapter(--currentChapter, chapter)
    }

    private fun loadChapter(newChapter: Int, chapter: () -> Unit) {
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
            chapterFlow(item)
                .onStart {
                    loadingChapters = loadingChapters + newChapter
                    pageItems.clear()
                }
                .catch { exceptionDao.insertException(it) }
                .onEach { urls ->
                    pageItems.add(PageItem.ChapterTransition(newChapter + 1, newChapter))
                    pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, newChapter, i, false) })
                    pageItems.add(PageItem.ChapterTransition(newChapter, newChapter - 1))
                    heatMapDao.upsertHeatMap()
                }
                .onCompletion { loadingChapters = loadingChapters - newChapter }
                .launchIn(viewModelScope)
        }
    }

    fun addChapterToWatched(
        newChapter: Int,
        chapter: () -> Unit,
    ) {
        loadChapter(newChapter, chapter)
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
        val chapter = list.getOrNull(currentChapter) ?: return
        val chapterIndex = currentChapter
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(chapterIndex)
        chapterFlow(chapter)
            .onStart {
                loadingChapters = loadingChapters + chapterIndex
                pageItems.clear()
            }
            .catch { exceptionDao.insertException(it) }
            .onEach { urls ->
                pageItems.add(PageItem.ChapterTransition(chapterIndex + 1, chapterIndex))
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterIndex, i, false) })
                pageItems.add(PageItem.ChapterTransition(chapterIndex, chapterIndex - 1))
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - chapterIndex }
            .launchIn(viewModelScope)
    }

    private fun loadDirectFromPath(filePath: String) {
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(0)
        downloadedChapterFlow(filePath)
            .onStart {
                loadingChapters = loadingChapters + 0
                pageItems.clear()
            }
            .onEach { urls ->
                pageItems.add(PageItem.ChapterTransition(1, 0))
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, 0, i, true) })
                pageItems.add(PageItem.ChapterTransition(0, -1))
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - 0 }
            .launchIn(viewModelScope)
    }

    private fun loadDownloadedChapterAtIndex(index: Int) {
        val filePath = downloadedPaths.getOrNull(index) ?: return
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(index)
        downloadedChapterFlow(filePath)
            .onStart {
                loadingChapters = loadingChapters + index
                pageItems.clear()
            }
            .onEach { urls ->
                pageItems.add(PageItem.ChapterTransition(index + 1, index))
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, index, i, true) })
                pageItems.add(PageItem.ChapterTransition(index, index - 1))
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - index }
            .launchIn(viewModelScope)
    }

    private fun appendDownloadedChapter(chapterListIndex: Int) {
        if (chapterListIndex < 0 || chapterListIndex >= downloadedPaths.size) return
        if (chapterListIndex in loadedChapterWindow) return
        loadedChapterWindow.addLast(chapterListIndex)
        val fromChapterListIndex = loadedChapterWindow[loadedChapterWindow.size - 2]

        viewModelScope.launch {
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

            val newPageTransition = PageItem.ChapterTransition(fromChapterListIndex, chapterListIndex)
            if (newPageTransition !in pageItems) pageItems.add(newPageTransition)

            downloadedChapterFlow(downloadedPaths[chapterListIndex])
                .onEach { urls ->
                    pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, true) })
                    pageItems.add(PageItem.ChapterTransition(chapterListIndex, chapterListIndex - 1))
                    heatMapDao.upsertHeatMap()
                }
                .onCompletion {
                    loadingChapters = loadingChapters - chapterListIndex
                    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
                }
                .launchIn(viewModelScope)
        }
    }

    private suspend fun prependDownloadedChapter(chapterListIndex: Int): Int {
        if (chapterListIndex < 0 || chapterListIndex >= downloadedPaths.size) return 0
        if (chapterListIndex in loadedChapterWindow) return 0

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
        downloadedChapterFlow(downloadedPaths[chapterListIndex])
            .firstOrNull()
            ?.let { urls ->
                newPages.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, true) })
                heatMapDao.upsertHeatMap()
            }

        loadingChapters = loadingChapters - chapterListIndex

        val insertedItems: List<PageItem> = newPages + PageItem.ChapterTransition(chapterListIndex, toChapterListIndex)
        pageItems.addAll(0, insertedItems)
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        return insertedItems.size
    }

    fun chapterRead(item: PageItem.ChapterTransition) {
        viewModelScope.launch {
            list.getOrNull(item.fromChapterListIndex)?.let { item ->
                if (chapters.fastAny { it.url == item.url }) return@let
                if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                    favoritesRepository.addWatched(ChapterWatched(item.url, item.name, mangaUrl))
                }
            }
        }
    }

    fun appendChapter(chapterListIndex: Int) {
        if (isDownloadedPathsMode) {
            appendDownloadedChapter(chapterListIndex)
            return
        }
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

            val newPageTransition = PageItem.ChapterTransition(fromChapterListIndex, chapterListIndex)
            if (newPageTransition !in pageItems) pageItems.add(newPageTransition)

            list.getOrNull(fromChapterListIndex)?.let { item ->
                if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                    favoritesRepository.addWatched(ChapterWatched(item.url, item.name, mangaUrl))
                }
            }

            list.getOrNull(chapterListIndex)?.let { chapterItem ->
                chapterFlow(chapterItem)
                    .catch { exceptionDao.insertException(it) }
                    .onEach { urls ->
                        pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, false) })
                        pageItems.add(PageItem.ChapterTransition(chapterListIndex, chapterListIndex - 1))
                        heatMapDao.upsertHeatMap()
                    }
                    .onCompletion {
                        loadingChapters = loadingChapters - chapterListIndex
                        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
                    }
                    .launchIn(viewModelScope)
            }
        }
    }

    suspend fun prependChapter(chapterListIndex: Int): Int {
        if (isDownloadedPathsMode) return prependDownloadedChapter(chapterListIndex)
        println("prependChapter: $chapterListIndex")
        println("loadedChapterWindow: $loadedChapterWindow")
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
        list.getOrNull(chapterListIndex)?.let { chapterItem ->
            chapterFlow(chapterItem)
                .catch { exceptionDao.insertException(it) }
                .firstOrNull()
                ?.let { urls ->
                    newPages.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, false) })
                    heatMapDao.upsertHeatMap()
                }
        }

        loadingChapters = loadingChapters - chapterListIndex

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
        val chapter = list.getOrNull(chapterIndex) ?: return
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(chapterIndex)
        chapterFlow(chapter)
            .onStart {
                loadingChapters = loadingChapters + chapterIndex
                pageItems.clear()
            }
            .catch { exceptionDao.insertException(it) }
            .onEach { urls ->
                pageItems.addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterIndex, i, false) })
                pageItems.add(PageItem.ChapterTransition(chapterIndex, chapterIndex - 1))
                heatMapDao.upsertHeatMap()
            }
            .onCompletion { loadingChapters = loadingChapters - chapterIndex }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        chapterHolder.chapterModel = null
        chapterHolder.chapters = null
    }
}