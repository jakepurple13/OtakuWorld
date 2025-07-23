package com.programmersbox.kmpuiviews.presentation.details

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ExceptionDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.kmpmodels.KmpChapterModel
import com.programmersbox.kmpmodels.KmpInfoModel
import com.programmersbox.kmpmodels.KmpItemModel
import com.programmersbox.kmpmodels.SourceRepository
import com.programmersbox.kmpuiviews.domain.TranslationHandler
import com.programmersbox.kmpuiviews.presentation.Screen
import com.programmersbox.kmpuiviews.presentation.toItemModel
import com.programmersbox.kmpuiviews.recordFirebaseException
import com.programmersbox.kmpuiviews.repository.FavoritesRepository
import com.programmersbox.kmpuiviews.utils.Cached
import com.programmersbox.kmpuiviews.utils.KmpFirebaseConnection
import com.programmersbox.kmpuiviews.utils.dispatchIo
import com.programmersbox.kmpuiviews.utils.fireListener
import com.programmersbox.kmpuiviews.utils.printLogs
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.ktor.util.decodeBase64Bytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailsViewModel(
    //handle: SavedStateHandle,
    details: Screen.DetailsScreen.Details?,
    private val blurHashDao: BlurHashDao,
    sourceRepository: SourceRepository,
    private val favoritesRepository: FavoritesRepository,
    firebaseItemListener: KmpFirebaseConnection.KmpFirebaseListener,
    firebaseDbModelListener: KmpFirebaseConnection.KmpFirebaseListener,
    firebaseChapterListener: KmpFirebaseConnection.KmpFirebaseListener,
    private val translationHandler: TranslationHandler,
    private val exceptionDao: ExceptionDao,
) : ViewModel() {

    //private val details: Screen.DetailsScreen.Details? = handle.toRoute()

    val itemModel: KmpItemModel? = details?.toItemModel(sourceRepository)
    //TODO: Fix this
    //?: handle.get<String>("model")
    //  ?.fromJson<KmpItemModel>(KmpApiService::class.java to ApiServiceDeserializer(genericInfo))

    private var detailState by mutableStateOf<DetailState>(DetailState.Loading)

    var info: KmpInfoModel? by mutableStateOf(null)

    var palette by mutableStateOf<Palette?>(null)

    private var addRemoveFavoriteJob: Job? = null

    private val itemListener = fireListener("favorite", firebaseItemListener)
    private val dbModelListener = fireListener("update", firebaseDbModelListener)
    private val chapterListener = fireListener("chapter", firebaseChapterListener)

    var favoriteListener by mutableStateOf(false)
    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    var description: String by mutableStateOf("")

    var dbModel by mutableStateOf<DbModel?>(null)

    var imageBitmap: ImageBitmap? by mutableStateOf(null)
    var blurHash by mutableStateOf<BitmapPainter?>(null)
    private var blurHashItem: BlurHashItem? = null

    val currentState by derivedStateOf {
        (detailState as? DetailState.Success)?.let {
            it.copy(action = if (favoriteListener) DetailFavoriteAction.Remove(it.info) else DetailFavoriteAction.Add(it.info))
        } ?: detailState
    }

    init {
        itemModel?.url?.let { url ->
            Cached.cache[url]?.let { flow { emit(Result.success(it)) } } ?: itemModel.toInfoModel()
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

        combine(
            blurHashDao
                .getHash(itemModel?.imageUrl)
                .onEach { blurHashItem = it }
                .map { it?.blurHash?.decodeBase64Bytes()?.decodeToImageBitmap() }
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
    }

    class BlurAdd(
        val bitmap: ImageBitmap?,
        val isFavorite: Boolean,
        val blurHashItem: BlurHashItem?,
        val imageUrl: String?,
    )

    suspend fun toggleNotify() {
        dbModel
            ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
            ?.let { favoritesRepository.toggleNotify(it) }
    }


    fun translateDescription(progress: MutableState<Boolean>) {
        viewModelScope.launch {
            progress.value = true
            description = translationHandler.translate(info!!.description)
            progress.value = false
        }
    }

    private fun setup(info: KmpInfoModel) {
        favoritesRepository
            .isFavorite(
                url = info.url,
                fireListenerClosable = itemListener,
            )
            .dispatchIo()
            .onEach { favoriteListener = it }
            .launchIn(viewModelScope)

        favoritesRepository
            .getChapters(
                url = info.url,
                fireListenerClosable = chapterListener,
            )
            .onEach { chapters = it }
            .launchIn(viewModelScope)

        favoritesRepository
            .getModel(
                url = info.url,
                fireListenerClosable = dbModelListener,
            )
            .onEach { dbModel = it }
            .launchIn(viewModelScope)
    }

    fun markAs(c: KmpChapterModel, b: Boolean) {
        val chapter = ChapterWatched(
            url = c.url,
            name = c.name,
            favoriteUrl = info!!.url
        )

        viewModelScope.launch {
            if (
                favoritesRepository.isIncognito(info!!.source.serviceName) ||
                favoritesRepository.isIncognito(info!!.url)
            ) return@launch

            if (b) {
                favoritesRepository.addWatched(chapter)
            } else {
                favoritesRepository.removeWatched(chapter)
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