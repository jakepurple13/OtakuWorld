package com.programmersbox.uiviews.details

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.kmpalette.palette.graphics.Palette
import com.programmersbox.extensionloader.SourceRepository
import com.programmersbox.favoritesdatabase.ChapterWatched
import com.programmersbox.favoritesdatabase.DbModel
import com.programmersbox.favoritesdatabase.ItemDao
import com.programmersbox.favoritesdatabase.toDbModel
import com.programmersbox.gsonutils.fromJson
import com.programmersbox.models.ApiService
import com.programmersbox.models.ChapterModel
import com.programmersbox.models.InfoModel
import com.programmersbox.models.ItemModel
import com.programmersbox.sharedutils.FirebaseDb
import com.programmersbox.sharedutils.TranslateItems
import com.programmersbox.uiviews.GenericInfo
import com.programmersbox.uiviews.utils.ApiServiceDeserializer
import com.programmersbox.uiviews.utils.Cached
import com.programmersbox.uiviews.utils.ComposableUtils
import com.programmersbox.uiviews.utils.Screen
import com.programmersbox.uiviews.utils.blurhash.BlurHashDao
import com.programmersbox.uiviews.utils.blurhash.BlurHashItem
import com.programmersbox.uiviews.utils.dispatchIo
import com.programmersbox.uiviews.utils.recordFirebaseException
import com.vanniktech.blurhash.BlurHash
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DetailsViewModel(
    handle: SavedStateHandle,
    genericInfo: GenericInfo,
    private val dao: ItemDao,
    private val blurHashDao: BlurHashDao,
    sourceRepository: SourceRepository,
) : ViewModel() {

    private val details: Screen.DetailsScreen.Details? = handle.toRoute()

    val itemModel: ItemModel? = details?.toItemModel(sourceRepository, genericInfo)
        ?: handle.get<String>("model")
            ?.fromJson<ItemModel>(ApiService::class.java to ApiServiceDeserializer(genericInfo))

    private var detailState by mutableStateOf<DetailState>(DetailState.Loading)

    var info: InfoModel? by mutableStateOf(null)

    var palette by mutableStateOf<Palette?>(null)

    private var addRemoveFavoriteJob: Job? = null

    private val itemListener = FirebaseDb.FirebaseListener()
    private val dbModelListener = FirebaseDb.FirebaseListener()
    private val chapterListener = FirebaseDb.FirebaseListener()

    var favoriteListener by mutableStateOf(false)
    var chapters: List<ChapterWatched> by mutableStateOf(emptyList())

    var description: String by mutableStateOf("")

    var dbModel by mutableStateOf<DbModel?>(null)

    var imageBitmap: Bitmap? by mutableStateOf(null)
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

        blurHashDao
            .getHash(itemModel?.imageUrl)
            .onEach { blurHashItem = it }
            .filterNotNull()
            .mapNotNull {
                BlurHash.decode(
                    it.blurHash,
                    width = ComposableUtils.IMAGE_WIDTH_PX,
                    height = ComposableUtils.IMAGE_HEIGHT_PX
                )?.asImageBitmap()
            }
            .onEach { blurHash = BitmapPainter(it) }
            .onEach { if (palette == null) palette = Palette.from(it).generate() }
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
            .onEach {
                if (it.blurHashItem == null && it.bitmap != null && it.isFavorite && it.imageUrl != null) {
                    blurHashDao.insertHash(
                        BlurHashItem(
                            it.imageUrl,
                            BlurHash.encode(it.bitmap, 4, 3)
                        )
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    class BlurAdd(
        val bitmap: Bitmap?,
        val isFavorite: Boolean,
        val blurHashItem: BlurHashItem?,
        val imageUrl: String?,
    )

    suspend fun toggleNotify() {
        dbModel
            ?.let { it.copy(shouldCheckForUpdate = !it.shouldCheckForUpdate) }
            ?.let {
                dao.updateFavoriteItem(it)
                FirebaseDb.toggleUpdateCheckShowFlow(it).collect()
            }
    }

    private val englishTranslator = TranslateItems()

    fun translateDescription(progress: MutableState<Boolean>) {
        englishTranslator.translateDescription(
            textToTranslate = info!!.description,
            progress = { progress.value = it },
            translatedText = { description = it }
        )
    }

    private fun setup(info: InfoModel) {
        combine(
            itemListener.findItemByUrlFlow(info.url),
            dao.containsItem(info.url)
        ) { f, d -> f || d }
            .dispatchIo()
            .onEach { favoriteListener = it }
            .launchIn(viewModelScope)

        combine(
            chapterListener.getAllEpisodesByShowFlow(info.url),
            dao.getAllChapters(info.url)
        ) { f, d -> (f + d).distinctBy { it.url } }
            .onEach { chapters = it }
            .launchIn(viewModelScope)

        combine(
            dao.getDbModel(info.url),
            dbModelListener.getShowFlow(info.url)
        ) { d, f -> f ?: d }
            .onEach { dbModel = it }
            .launchIn(viewModelScope)
    }

    fun markAs(c: ChapterModel, b: Boolean) {
        val chapter = ChapterWatched(
            url = c.url,
            name = c.name,
            favoriteUrl = info!!.url
        )

        viewModelScope.launch {
            if (b) dao.insertChapter(chapter) else dao.deleteChapter(chapter)
            (if (b) FirebaseDb.insertEpisodeWatchedFlow(chapter) else FirebaseDb.removeEpisodeWatchedFlow(chapter)).collect()
        }
    }

    fun favoriteAction(action: DetailFavoriteAction) {
        when (action) {
            is DetailFavoriteAction.Add -> {
                val db = action.info.toDbModel(action.info.chapters.size)
                addRemoveFavoriteJob?.cancel()
                addRemoveFavoriteJob = viewModelScope.launch {
                    dao.insertFavorite(db)
                    FirebaseDb.insertShowFlow(db).collect()
                }

                imageBitmap?.let {
                    viewModelScope.launch {
                        blurHashDao.insertHash(
                            BlurHashItem(
                                action.info.imageUrl,
                                BlurHash.encode(it, 4, 3)
                            )
                        )
                    }
                }
            }

            is DetailFavoriteAction.Remove -> {
                val db = action.info.toDbModel(action.info.chapters.size)
                addRemoveFavoriteJob?.cancel()
                addRemoveFavoriteJob = viewModelScope.launch {
                    dao.deleteFavorite(db)
                    FirebaseDb.removeShowFlow(db).collect()
                }

                viewModelScope.launch {
                    blurHashItem?.let { blurHashDao.deleteHash(it) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        itemListener.unregister()
        chapterListener.unregister()
        dbModelListener.unregister()
        englishTranslator.clear()
    }
}

sealed class DetailState {
    object Loading : DetailState()

    data class Success(
        val info: InfoModel,
        val action: DetailFavoriteAction,
    ) : DetailState()

    data class Error(val e: Throwable) : DetailState()
}

sealed class DetailFavoriteAction {
    data class Add(val info: InfoModel) : DetailFavoriteAction()
    data class Remove(val info: InfoModel) : DetailFavoriteAction()
}