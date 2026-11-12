package com.programmersbox.novel.shared.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpStorage
import com.programmersbox.datastore.DataStoreHandling
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.ChapterRecaps
import com.programmersbox.kmpuiviews.presentation.navactions.NavigationActions
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.novel.shared.ChapterHolder
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    novelReader: NovelReader,
    private val chapterHolder: ChapterHolder,
    private val favoritesRepository: FavoritesRepository,
    aiText: AiText,
    private val dataStoreHandling: DataStoreHandling,
) : ViewModel() {

    val recaps = ChapterRecaps(aiText, viewModelScope)
    /** Null until the stored value loads, so the bar never animates from a guessed default. */
    val recapEnabled: StateFlow<Boolean?> = dataStoreHandling.novelChapterRecap.asFlow()
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setRecapEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStoreHandling.novelChapterRecap.set(enabled) }
    }

    val isDownloaded: Boolean = novelReader.downloaded
    val headers = mutableStateMapOf<String, String>()

    val model: Flow<List<String>>? = chapterHolder.chapterModel
        ?.getChapterInfo()
        ?.map {
            headers.putAll(it.flatMap { h -> h.headers.toList() })
            it.mapNotNull(KmpStorage::link)
        }

    val filePath: PlatformFile? = runCatching { novelReader.filePath?.let { PlatformFile(it) } }.getOrNull()
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
        fun navigateToNovelReader(
            navController: NavigationActions,
            novelTitle: String? = null,
            novelUrl: String? = null,
            novelInfoUrl: String? = null,
            downloaded: Boolean = false,
            filePath: String? = null,
            seedColor: Int? = null,
        ) {
            navController.navigate(
                NovelReader(
                    novelTitle = novelTitle,
                    novelUrl = novelUrl,
                    novelInfoUrl = novelInfoUrl,
                    downloaded = downloaded,
                    filePath = filePath,
                    seedColor = seedColor
                )
            )// { launchSingleTop = true }
        }
    }

    @Serializable
    data class NovelReader(
        val novelTitle: String? = null,
        val novelUrl: String? = null,
        val novelInfoUrl: String? = null,
        val downloaded: Boolean,
        val filePath: String? = null,
        // Cover color handed over by Details to theme the reader
        val seedColor: Int? = null,
    ) : NavKey

    val seedColor = novelReader.seedColor

    val title by lazy { novelReader.novelTitle ?: "" }

    var list by mutableStateOf<List<KmpChapterModel>>(emptyList())

    private val mangaUrl by lazy { novelReader.novelInfoUrl ?: "" }

    var currentChapter: Int by mutableIntStateOf(0)

    var pageList by mutableStateOf("")
    var isLoadingPages by mutableStateOf(false)

    // Declared before init, which starts the first load
    private var loadPagesJob: Job? = null

    val currentChapterModel by derivedStateOf { list.getOrNull(currentChapter) }

    /** The list is newest-first, so the chapter before the current one is the next index. */
    val previousChapterModel by derivedStateOf { list.getOrNull(currentChapter + 1) }

    fun requestRecap(chapter: Int) {
        val current = list.getOrNull(chapter) ?: return
        val previous = list.getOrNull(chapter + 1) ?: return
        recaps.request(current.url) {
            withContext(Dispatchers.IO) {
                previous.getChapterInfo().first().firstNotNullOfOrNull(KmpStorage::link)
            }
        }
    }

    var addToFavorites by mutableStateOf(FavoriteChecker(false, 0))

    data class FavoriteChecker(val hasShown: Boolean, val count: Int, val isFavorite: Boolean = false) {
        val shouldShow: Boolean = !hasShown && count > FAVORITE_CHECK && !isFavorite
    }

    init {
        val url = chapterHolder.chapterModel?.url ?: novelReader.novelUrl
        list = chapterHolder.chapters.orEmpty()
        currentChapter = list.indexOfFirst { l -> l.url == url }.coerceIn(0, list.lastIndex.coerceAtLeast(0))

        loadPages(modelPath)

        favoritesRepository
            .isFavorite(url = mangaUrl)
            .dispatchIo()
            .onEach { addToFavorites = addToFavorites.copy(isFavorite = it) }
            .launchIn(viewModelScope)
    }

    var showInfo by mutableStateOf(true)

    fun addChapterToWatched(newChapter: Int, chapter: (String) -> Unit) {
        currentChapter = newChapter
        addToFavorites = addToFavorites.copy(count = addToFavorites.count + 1)
        list.getOrNull(newChapter)?.let { item ->
            recaps.onChapterChanged(item.url)
            ChapterWatched(item.url, item.name, mangaUrl)
                .let {
                    viewModelScope.launch {
                        if (!favoritesRepository.isIncognito(item.source.serviceName)) {
                            favoritesRepository.addWatched(it)
                        }
                        withContext(Dispatchers.Main) { chapter(item.name) }
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
        loadPagesJob?.cancel()
        loadPagesJob = modelPath
            ?.onStart {
                isLoadingPages = true
                pageList = ""
            }
            ?.onEach { pageList = it.firstOrNull().orEmpty() }
            // A cancelled load is replaced by a newer one that owns the loading flag
            ?.onCompletion { if (it !is CancellationException) isLoadingPages = false }
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