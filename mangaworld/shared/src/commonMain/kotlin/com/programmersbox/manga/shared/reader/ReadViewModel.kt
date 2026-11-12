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
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.HeatMapDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.manga.shared.ChapterHolder
import com.programmersbox.manga.shared.downloads.MangaDownloadManager
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.toKotlinxIoPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val heatMapDao: HeatMapDao,
    private val exceptionDao: ExceptionDao,
    private val mangaDownloadManager: MangaDownloadManager,
    private val bookmarkRepository: BookmarkRepository,
) : ViewModel() {

    val headers = mutableStateMapOf<String, String>()

    // Plain map, not snapshot state: recording a ratio must not recompose every visible page.
    private val pageAspectRatios = mutableMapOf<String, Float>()
    private var lastAspectRatio = DEFAULT_PAGE_ASPECT_RATIO

    /** Height to reserve for a page before it loads: its known ratio, else the last page seen. */
    fun aspectRatioFor(url: String): Float = pageAspectRatios[url] ?: lastAspectRatio

    fun recordAspectRatio(url: String, ratio: Float) {
        if (!ratio.isFinite() || ratio <= 0f) return
        pageAspectRatios[url] = ratio
        lastAspectRatio = ratio
    }

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
                    // Hidden files, like the download marker, aren't pages
                    .filterNot { f -> f.name.startsWith(".") }
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
                .filterNot { f -> f.name.startsWith(".") }
                .sortedBy { f -> f.name.split(".").first().toIntOrNull() ?: 0 }
                .fastMap { sanitizePath(it.toKotlinxIoPath().toString()) }
                .let { emit(it) }
        }
            .catch { e -> exceptionDao.insertException(e); emit(emptyList()) }
            .flowOn(Dispatchers.IO)

    companion object {
        private const val WINDOW_SIZE = 3
        private const val DEFAULT_PAGE_ASPECT_RATIO = 0.7f

        fun navigateToMangaReader(
            navController: NavigationActions,
            mangaTitle: String? = null,
            mangaUrl: String? = null,
            mangaInfoUrl: String? = null,
            downloaded: Boolean = false,
            filePath: String? = null,
            seedColor: Int? = null,
        ) {
            navController.navigate(
                MangaReader(
                    mangaTitle = mangaTitle,
                    mangaUrl = mangaUrl,
                    mangaInfoUrl = mangaInfoUrl,
                    downloaded = downloaded,
                    filePath = filePath,
                    seedColor = seedColor
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
        // Cover color handed over by Details to theme the reader
        val seedColor: Int? = null,
    ) : NavKey

    val seedColor = mangaReader.seedColor

    val title by lazy { mangaReader.mangaTitle ?: "" }

    var list by mutableStateOf<List<KmpChapterModel>>(emptyList())

    private val mangaUrl by lazy { mangaReader.mangaInfoUrl ?: "" }

    var currentChapter: Int by mutableIntStateOf(0)

    val pageItems = mutableStateListOf<PageItem>()
    var loadingChapters by mutableStateOf(emptySet<Int>())
    val isLoadingPages: Boolean get() = loadingChapters.isNotEmpty()

    private val loadedChapterWindow = ArrayDeque<Int>()

    // Every load feeding the current chapter window, so jumping to another chapter can stop them
    private val windowJobs = mutableListOf<Job>()
    private var windowGeneration = 0

    private var downloadedPaths: List<String> = emptyList()
    val isDownloadedPathsMode: Boolean get() = downloadedPaths.isNotEmpty()
    val chapterCount: Int get() = if (isDownloadedPathsMode) downloadedPaths.size else list.size

    fun chapterName(index: Int): String? =
        if (isDownloadedPathsMode) downloadedPaths.getOrNull(index)?.substringAfterLast("/")
        else list.getOrNull(index)?.name

    val currentChapterModel by derivedStateOf { list.getOrNull(currentChapter) }

    var addToFavorites by mutableStateOf(FavoriteChecker(false, 0))

    data class FavoriteChecker(val hasShown: Boolean, val count: Int, val isFavorite: Boolean = false) {
        val shouldShow: Boolean = !hasShown && count > FAVORITE_CHECK && !isFavorite
    }

    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    val isChapterBookmarked by derivedStateOf {
        currentChapterModel
            ?.url
            ?.let { bookmarkRepository.hasBookmarkFlow(it) }
            ?: flowOf(false)
    }

    init {
        val url = chapterHolder.chapterModel?.url ?: mangaReader.mangaUrl
        list = chapterHolder.chapters.orEmpty()
        currentChapter = list
            .indexOfFirst { l -> l.url == url }
            .coerceIn(0, list.lastIndex.coerceAtLeast(1))

        val paths = chapterHolder.downloadedChapterPaths
        chapterHolder.downloadedChapterPaths = null
        if (!paths.isNullOrEmpty()) {
            downloadedPaths = paths
            currentChapter = paths.indexOf(mangaReader.filePath).coerceAtLeast(0)
            loadDownloadedChapterAtIndex(currentChapter)
        } else if (list.isEmpty() && mangaReader.downloaded && !mangaReader.filePath.isNullOrEmpty()) {
            loadDirectFromPath(mangaReader.filePath)
        } else {
            loadInitialChapter()
        }

        favoritesRepository
            .isFavorite(url = mangaUrl)
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

    fun loadPreviousChapter(chapter: (String) -> Unit) {
        loadChapter(++currentChapter, chapter)
    }

    fun loadNextChapter(chapter: (String) -> Unit) {
        loadChapter(--currentChapter, chapter)
    }

    /** Cancels every load into the old window so its pages can't land in the new one. */
    private fun resetChapterWindow(chapterIndex: Int) {
        windowJobs.forEach { it.cancel() }
        windowJobs.clear()
        windowGeneration++
        loadingChapters = emptySet()
        loadedChapterWindow.clear()
        loadedChapterWindow.addLast(chapterIndex)
    }

    private fun Job.trackInWindow() {
        windowJobs.removeAll { it.isCompleted }
        windowJobs.add(this)
    }

    private fun loadChapter(newChapter: Int, chapter: (String) -> Unit) {
        if (isDownloadedPathsMode) {
            if (newChapter !in downloadedPaths.indices) return
            currentChapter = newChapter
            addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
            chapterName(newChapter)?.let(chapter)
            loadDownloadedChapterAtIndex(newChapter)
            return
        }
        currentChapter = newChapter
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        list.getOrNull(newChapter)?.let { item ->
            chapterHolder.chapterModel = item
            viewModelScope.launch {
                if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                    favoritesRepository.addWatched(ChapterWatched(item.url, item.name, mangaUrl))
                }
                withContext(Dispatchers.Main) { chapter(item.name) }
            }

            resetChapterWindow(newChapter)
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
                .onCompletion { if (it !is CancellationException) loadingChapters = loadingChapters - newChapter }
                .launchIn(viewModelScope)
                .trackInWindow()
        }
    }

    fun addChapterToWatched(
        newChapter: Int,
        chapter: (String) -> Unit,
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
        resetChapterWindow(chapterIndex)
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
            .onCompletion { if (it !is CancellationException) loadingChapters = loadingChapters - chapterIndex }
            .launchIn(viewModelScope)
            .trackInWindow()
    }

    private fun loadDirectFromPath(filePath: String) {
        resetChapterWindow(0)
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
            .onCompletion { if (it !is CancellationException) loadingChapters = loadingChapters - 0 }
            .launchIn(viewModelScope)
            .trackInWindow()
    }

    private fun loadDownloadedChapterAtIndex(index: Int) {
        val filePath = downloadedPaths.getOrNull(index) ?: return
        resetChapterWindow(index)
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
            .onCompletion { if (it !is CancellationException) loadingChapters = loadingChapters - index }
            .launchIn(viewModelScope)
            .trackInWindow()
    }

    private fun appendDownloadedChapter(chapterListIndex: Int) {
        if (chapterListIndex < 0 || chapterListIndex >= downloadedPaths.size) return
        if (chapterListIndex in loadedChapterWindow) return
        loadedChapterWindow.addLast(chapterListIndex)
        val fromChapterListIndex = loadedChapterWindow[loadedChapterWindow.size - 2]

        viewModelScope.launch {
            while (loadedChapterWindow.size > WINDOW_SIZE) {
                pageItems.evictChapter(loadedChapterWindow.removeFirst(), fromTop = true)
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
                    if (it is CancellationException) return@onCompletion
                    loadingChapters = loadingChapters - chapterListIndex
                    addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
                }
                .collect()
        }.trackInWindow()
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

    /**
     * Called while [item] is on screen. A boundary whose newer side isn't loaded appends it; the top
     * boundary reached while scrolling back prepends the older chapter.
     */
    suspend fun onTransitionReached(item: PageItem.ChapterTransition, scrolledBackward: Boolean) {
        if (item.fromChapterListIndex == 0) {
            chapterRead(item)
            return
        }
        when (val load = item.loadFor(loadedChapterWindow, scrolledBackward)) {
            is TransitionLoad.Append -> appendChapter(load.chapterListIndex)
            is TransitionLoad.Prepend -> prependChapter(load.chapterListIndex)
            null -> Unit
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
                pageItems.evictChapter(loadedChapterWindow.removeFirst(), fromTop = true)
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
                        if (it is CancellationException) return@onCompletion
                        loadingChapters = loadingChapters - chapterListIndex
                        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
                    }
                    .collect()
            }
        }.trackInWindow()
    }

    /**
     * Loads the older chapter [chapterListIndex] above the loaded ones. Returns how many items were
     * inserted at the top; lazy keys keep the visible page in place.
     */
    suspend fun prependChapter(chapterListIndex: Int): Int {
        val downloaded = isDownloadedPathsMode
        if (chapterListIndex < 0 || chapterListIndex >= chapterCount) return 0
        if (chapterListIndex in loadedChapterWindow || loadedChapterWindow.isEmpty()) return 0

        val toChapterListIndex = loadedChapterWindow.first()
        loadedChapterWindow.addFirst(chapterListIndex)
        val generation = windowGeneration
        loadingChapters = loadingChapters + chapterListIndex

        val urls = if (downloaded) {
            downloadedChapterFlow(downloadedPaths[chapterListIndex]).firstOrNull()
        } else {
            chapterFlow(list[chapterListIndex])
                .catch { exceptionDao.insertException(it) }
                .firstOrNull()
        }.orEmpty()

        // A chapter jump replaced the window while this was loading
        if (generation != windowGeneration) return 0
        loadingChapters = loadingChapters - chapterListIndex

        if (urls.isEmpty()) {
            loadedChapterWindow.remove(chapterListIndex)
            return 0
        }
        heatMapDao.upsertHeatMap()

        // Evict newest loaded chapter if window is exceeded
        while (loadedChapterWindow.size > WINDOW_SIZE) {
            pageItems.evictChapter(loadedChapterWindow.removeLast(), fromTop = false)
        }

        val boundary = PageItem.ChapterTransition(chapterListIndex, toChapterListIndex)
        val insertedItems = buildList {
            add(PageItem.ChapterTransition(chapterListIndex + 1, chapterListIndex))
            addAll(urls.mapIndexed { i, url -> PageItem.Page(url, chapterListIndex, i, downloaded) })
            if (pageItems.firstOrNull() != boundary) add(boundary)
        }
        pageItems.addAll(0, insertedItems)
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        return insertedItems.size
    }

    fun updateCurrentChapter(chapterListIndex: Int) {
        if (chapterListIndex == currentChapter) return
        currentChapter = chapterListIndex
        chapterHolder.chapterModel = list.getOrNull(chapterListIndex)
    }

    fun refresh() {
        headers.clear()
        loadInitialChapter()
    }

    fun toggleBookmark(toggle: Boolean) {
        viewModelScope.launch {
            val chapter = currentChapterModel ?: return@launch
            if (toggle) {
                bookmarkRepository.insertBookmark(
                    BookmarkedChapter(
                        chapterUrl = chapter.url,
                        chapterName = chapter.name,
                        parentUrl = mangaUrl,
                        parentTitle = title,
                        parentImageUrl = "",
                        source = chapter.source.serviceName,
                    )
                )
            } else {
                bookmarkRepository.deleteBookmark(chapter.url)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        chapterHolder.chapterModel = null
        chapterHolder.chapters = null
        chapterHolder.downloadedChapterPaths = null
    }
}