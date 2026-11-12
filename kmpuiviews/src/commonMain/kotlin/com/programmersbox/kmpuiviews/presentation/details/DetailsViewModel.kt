package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.material3.ColorScheme
import com.programmersbox.kmpuiviews.theme.SchemeInputs
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kmpalette.generatePalette
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.favoritesdatabase.BlurHashDao
import com.programmersbox.favoritesdatabase.BlurHashItem
import com.programmersbox.favoritesdatabase.BookmarkedChapter
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.ChapterDownloadUiState
import com.programmersbox.kmpuiviews.KmpGenericInfo
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.domain.aitext.AiFeature
import com.programmersbox.kmpuiviews.domain.aitext.AiText
import com.programmersbox.kmpuiviews.domain.aitext.AiTextPrompts
import com.programmersbox.kmpuiviews.domain.aitext.AiTextRunner
import com.programmersbox.kmpuiviews.domain.aitext.AiTextState
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.toItemModel
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.repository.BookmarkRepository
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.ImageModifier
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.SharedCoverBitmaps
import com.programmersbox.kmpuiviews.utils.composables.sharedelements.sharedItemKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64

class DetailsViewModel(
    //handle: SavedStateHandle,
    details: Screen.DetailsScreen.Details?,
    private val genericInfo: KmpGenericInfo,
    private val blurHashDao: BlurHashDao,
    sourceRepository: SourceRepository,
    private val favoritesRepository: FavoritesRepository,
    private val translationHandler: TranslationHandler,
    private val exceptionDao: ExceptionDao,
    private val imageModifier: ImageModifier,
    private val bookmarkRepository: BookmarkRepository,
    sharedCoverBitmaps: SharedCoverBitmaps,
    private val aiText: AiText,
    private val tldrCache: TldrCache,
) : ViewModel() {

    //private val details: Screen.DetailsScreen.Details? = handle.toRoute()

    val itemModel: KmpItemModel? = details?.toItemModel(sourceRepository)
    //TODO: Fix this
    //?: handle.get<String>("model")
    //  ?.fromJson<KmpItemModel>(KmpApiService::class.java to ApiServiceDeserializer(genericInfo))

    private var detailState by mutableStateOf<DetailState>(DetailState.Loading)

    var info: KmpInfoModel? by mutableStateOf(null)

    var palette by mutableStateOf<Palette?>(null)

    /** The last color scheme built for this screen, so a rotation doesn't rebuild or re-animate it */
    internal var colorScheme: Pair<SchemeInputs, ColorScheme>? = null

    private var addRemoveFavoriteJob: Job? = null

    var favoriteListener by mutableStateOf(false)
    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())
    val bookmarkedChapterUrls = mutableStateSetOf<String>()

    val selectedChapterUrls = mutableStateSetOf<String>()

    val isSelecting by derivedStateOf { selectedChapterUrls.isNotEmpty() }

    var description: String by mutableStateOf("")

    var dbModel by mutableStateOf<DbModel?>(null)

    /** True when the description is long enough to be worth summarizing and an on-device engine is ready. */
    val tldrAvailable: StateFlow<Boolean> = snapshotFlow { description }
        .map {
            it.isNotBlank() && it.length > AiTextPrompts.TLDR_MIN_CHARS &&
                    aiText.engineFor(AiFeature.Tldr, it.length) != null
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val tldrRunner = AiTextRunner(viewModelScope)
    private val cachedTldr = MutableStateFlow<TldrEntry?>(null)
    private var tldrJob: Job? = null

    val tldr: StateFlow<AiTextState> = combine(tldrRunner.state, cachedTldr) { run, cached ->
        cached?.done ?: run
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AiTextState.Idle)

    private val _tldrTranslating = MutableStateFlow(false)
    val tldrTranslating: StateFlow<Boolean> = _tldrTranslating.asStateFlow()

    private val _tldrTranslated = MutableStateFlow(false)

    /** True when the shown TL;DR was summarized from a translated description. */
    val tldrTranslated: StateFlow<Boolean> = _tldrTranslated.asStateFlow()

    private val tldrUrl: String? get() = itemModel?.url ?: info?.url

    /** A copy of the cover the tapped card already loaded, if any; safe to draw directly */
    val handedOffCover: ImageBitmap? = itemModel?.let { sharedCoverBitmaps.take(it.sharedItemKey()) }

    // Starts as the handed-off cover so the palette builds before Details loads the image
    var imageBitmap: ImageBitmap? by mutableStateOf(handedOffCover)
    var blurHash by mutableStateOf<BitmapPainter?>(null)
    private var blurHashItem: BlurHashItem? = null

    val downloadStates = mutableStateMapOf<String, ChapterDownloadUiState>()

    val currentState by derivedStateOf {
        (detailState as? DetailState.Success)?.let {
            it.copy(action = if (favoriteListener) DetailFavoriteAction.Remove(it.info) else DetailFavoriteAction.Add(it.info))
        } ?: detailState
    }

    init {
        itemModel?.url?.let { url ->
            flow {
                Cached
                    .cache[url]
                    ?.let { emit(Result.success(it)) }
                    ?: emitAll(itemModel.toInfoModel())
            }
        }
            ?.dispatchIo()
            ?.catch {
                recordFirebaseException(it)
                exceptionDao.insertException(it)
                it.printStackTrace()
                emit(Result.failure(it))
                detailState = DetailState.Error(it)
            }
            ?.filter { it.isSuccess }
            ?.map { it.getOrThrow() }
            ?.onEach { item ->
                info = item
                detailState = DetailState.Success(
                    info = item,
                    action = DetailFavoriteAction.Add(item)
                )
                description = item.description.ifEmpty { "No Description Found" }
                setup(item)
                Cached.cache[item.url] = item
            }
            ?.launchIn(viewModelScope)

        snapshotFlow { info }
            .filterNotNull()
            .flatMapLatest {
                genericInfo.observeChapterDownloadStates(
                    chapters = it.chapters,
                    mangaTitle = it.title.ifBlank { it.url }
                )
            }
            .onEach {
                downloadStates.clear()
                downloadStates.putAll(it)
            }
            .launchIn(viewModelScope)

        combine(
            blurHashDao
                .getHash(itemModel?.imageUrl)
                .onEach { blurHashItem = it }
                .map { runCatching { it?.blurHash?.let { Base64.decode(it) }?.decodeToImageBitmap() }.getOrNull() }
                .onEach { blurHash = runCatching { BitmapPainter(it!!) }.getOrNull() },
            snapshotFlow { imageBitmap }
        ) { hash, image -> hash to image }
            .onEach {
                runCatching { (it.second ?: it.first)?.generatePalette()!! }
                    .onSuccess { p -> palette = p }
                    .onSuccess { printLogs { it } }
                    .onFailure { e -> e.printStackTrace() }
            }
            .dispatchIo()
            .launchIn(viewModelScope)

        combine(
            snapshotFlow { favoriteListener },
            snapshotFlow { imageBitmap },
            blurHashDao.getHash(itemModel?.imageUrl),
            snapshotFlow { itemModel?.imageUrl }
        ) { f, i, b, image ->
            BlurAdd(
                bitmap = i,
                isFavorite = f,
                blurHashItem = b,
                imageUrl = image
            )
        }
            .dispatchIo()
            .onEach {
                if (it.blurHashItem == null && it.bitmap != null && it.isFavorite && it.imageUrl != null) {
                    runCatching {
                        blurHashDao.insertHash(
                            BlurHashItem(
                                it.imageUrl,
                                it.bitmap.encodeToByteArray().decodeToString()//BlurHash.encode(it.bitmap, 4, 3)
                            )
                        )
                    }
                } else if (it.blurHashItem != null && !it.isFavorite) {
                    runCatching {
                        blurHashDao.deleteHash(it.blurHashItem)
                    }
                }
            }
            .launchIn(viewModelScope)

        snapshotFlow { imageBitmap }
            .onEach {
                it
                    ?.let { bitmap -> imageModifier.startImageDescription(bitmap) }
                    ?: println("[ImageModifier] Not ready yet")
            }
            .dispatchIo()
            .launchIn(viewModelScope)

        itemModel?.url?.let { mangaUrl ->
            bookmarkRepository
                .getBookmarksForDetail(mangaUrl)
                .onEach { bookmarks ->
                    bookmarkedChapterUrls.clear()
                    bookmarkedChapterUrls.addAll(bookmarks.map { it.chapterUrl })
                }
                .launchIn(viewModelScope)
        }
    }

    class BlurAdd(
        val bitmap: ImageBitmap?,
        val isFavorite: Boolean,
        val blurHashItem: BlurHashItem?,
        val imageUrl: String?,
    )

    fun toggleBookmark(chapter: KmpChapterModel) {
        val mangaUrl = itemModel?.url ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (bookmarkRepository.hasBookmark(chapter.url)) {
                bookmarkRepository.deleteBookmark(chapter.url)
            } else {
                bookmarkRepository.insertBookmark(bookmarkFor(chapter, mangaUrl))
            }
        }
    }

    fun setBookmarked(chapters: List<KmpChapterModel>, bookmarked: Boolean) {
        val mangaUrl = itemModel?.url ?: return
        viewModelScope.launch(Dispatchers.IO) {
            chapters.forEach { chapter ->
                val has = bookmarkRepository.hasBookmark(chapter.url)
                when {
                    bookmarked && !has -> bookmarkRepository.insertBookmark(bookmarkFor(chapter, mangaUrl))
                    !bookmarked && has -> bookmarkRepository.deleteBookmark(chapter.url)
                }
            }
        }
    }

    fun restoreBookmarkState(chapters: List<KmpChapterModel>, previouslyBookmarked: Set<String>) {
        val (wasBookmarked, wasNot) = chapters.partition { it.url in previouslyBookmarked }
        setBookmarked(wasBookmarked, true)
        setBookmarked(wasNot, false)
    }

    private fun bookmarkFor(chapter: KmpChapterModel, mangaUrl: String) = BookmarkedChapter(
        chapterUrl = chapter.url,
        chapterName = chapter.name,
        parentUrl = mangaUrl,
        parentTitle = itemModel?.title ?: "",
        parentImageUrl = itemModel?.imageUrl ?: "",
        source = itemModel?.source?.serviceName ?: "",
    )

    fun toggleSelection(url: String) {
        if (!selectedChapterUrls.remove(url)) selectedChapterUrls.add(url)
    }

    fun selectAllChapters() {
        info?.chapters?.let { chapters -> selectedChapterUrls.addAll(chapters.map { it.url }) }
    }

    fun invertSelection() {
        val all = info?.chapters?.map { it.url }.orEmpty()
        val inverted = all.filterNot { it in selectedChapterUrls }
        selectedChapterUrls.clear()
        selectedChapterUrls.addAll(inverted)
    }

    fun clearSelection() {
        selectedChapterUrls.clear()
    }

    fun toggleNotify() {
        viewModelScope.launch(Dispatchers.IO) {
            dbModel
                ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
                ?.let { favoritesRepository.toggleNotify(it) }
        }
    }


    fun translateDescription(progress: MutableState<Boolean>) {
        viewModelScope.launch(Dispatchers.IO) {
            progress.value = true
            description = translationHandler.translate(info!!.description)
            progress.value = false
        }
    }

    fun showTldr() {
        val url = tldrUrl
        val cached = url?.let { tldrCache[it] }
        if (cached != null) {
            _tldrTranslated.value = cached.translated
            cachedTldr.value = cached
            return
        }
        tldrJob?.cancel()
        // Drop any earlier error so Retry isn't shown under the Translating label
        tldrRunner.reset()
        tldrJob = viewModelScope.launch(Dispatchers.IO) {
            val original = description
            _tldrTranslating.value = true
            val text = try {
                translationHandler.translate(original)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                original
            } finally {
                _tldrTranslating.value = false
            }
            val translated = text != original
            val engine = aiText.engineFor(AiFeature.Tldr, text.length) ?: return@launch
            _tldrTranslated.value = translated
            // On Main, like hideTldr, so a hide either cancels this before the run or resets after it
            withContext(Dispatchers.Main) {
                ensureActive()
                tldrRunner.run(engine) { aiText.tldr(text) }
            }
            val result = tldrRunner.state.first { it is AiTextState.Done || it is AiTextState.Error }
            if (result is AiTextState.Done && url != null) tldrCache[url] = TldrEntry(result, translated)
        }
    }

    fun hideTldr() {
        tldrJob?.cancel()
        tldrJob = null
        tldrRunner.reset()
        cachedTldr.value = null
        _tldrTranslating.value = false
        _tldrTranslated.value = false
    }

    private fun setup(info: KmpInfoModel) {
        favoritesRepository
            .isFavorite(url = info.url)
            .dispatchIo()
            .onEach { favoriteListener = it }
            .launchIn(viewModelScope)

        favoritesRepository
            .getChapters(url = info.url)
            .onEach { chapters = it }
            .launchIn(viewModelScope)

        favoritesRepository
            .getModel(url = info.url)
            .onEach { dbModel = it }
            .launchIn(viewModelScope)
    }

    fun markAs(c: KmpChapterModel, b: Boolean) {
        val chapter = ChapterWatched(
            url = c.url,
            name = c.name,
            favoriteUrl = info!!.url
        )

        viewModelScope.launch(Dispatchers.IO) {
            if (
                favoritesRepository.isIncognito(info!!.source.serviceName) ||
                favoritesRepository.isIncognito(info!!.url)
            ) return@launch

            if (b) {
                // Marking from Details is not reading, so it gets no read time.
                favoritesRepository.addWatched(chapter, stampReadTime = false)
            } else {
                favoritesRepository.removeWatched(chapter)
            }
        }
    }

    // Only touch chapters whose read state changes; re-inserting a read chapter resets its sync fields.
    fun markChaptersAs(chapters: List<KmpChapterModel>, read: Boolean) {
        val readUrls = this.chapters.map { it.url }.toSet()
        chapters
            .filter { (it.url in readUrls) != read }
            .forEach { markAs(it, read) }
    }

    fun restoreReadState(chapters: List<KmpChapterModel>, previouslyRead: Set<String>) {
        val readUrls = this.chapters.map { it.url }.toSet()
        chapters
            .filter { (it.url in readUrls) != (it.url in previouslyRead) }
            .forEach { markAs(it, it.url in previouslyRead) }
    }

    fun reread() {
        viewModelScope.launch(Dispatchers.IO) {
            chapters.forEach {
                favoritesRepository.removeWatched(it)
            }
        }
    }

    fun favoriteAction(action: DetailFavoriteAction) {
        when (action) {
            is DetailFavoriteAction.Add -> {
                val db = action.info.toDbModel(action.info.chapters.size)
                addRemoveFavoriteJob?.cancel()
                addRemoveFavoriteJob = viewModelScope.launch(Dispatchers.IO) {
                    favoritesRepository.addFavorite(db)
                }
            }

            is DetailFavoriteAction.Remove -> {
                val db = action.info.toDbModel(action.info.chapters.size)
                addRemoveFavoriteJob?.cancel()
                addRemoveFavoriteJob = viewModelScope.launch(Dispatchers.IO) {
                    favoritesRepository.removeFavorite(db)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        translationHandler.clear()
        imageModifier.close()
    }
}

sealed class DetailState {
    object Loading : DetailState()

    data class Success(
        val info: KmpInfoModel,
        val action: DetailFavoriteAction,
    ) : DetailState()

    data class Error(val e: Throwable) : DetailState()
}

sealed class DetailFavoriteAction {
    data class Add(val info: KmpInfoModel) : DetailFavoriteAction()
    data class Remove(val info: KmpInfoModel) : DetailFavoriteAction()
}